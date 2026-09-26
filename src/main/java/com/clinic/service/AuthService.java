package com.clinic.service;

import com.clinic.dto.PageResponse;
import com.clinic.dto.PermissionDto;
import com.clinic.dto.RoleActionDto;
import com.clinic.dto.SearchFilter;
import com.clinic.dto.UserDto;
import com.clinic.entity.AppUser;
import com.clinic.entity.RoleAction;
import com.clinic.entity.RolePermission;
import com.clinic.enums.Access;
import com.clinic.enums.Action;
import com.clinic.enums.Module;
import com.clinic.enums.Role;
import com.clinic.exception.BusinessException;
import com.clinic.mapper.SettingsMapper;
import com.clinic.repository.RoleActionRepository;
import com.clinic.repository.RolePermissionRepository;
import com.clinic.repository.Specs;
import com.clinic.repository.UserRepository;
import com.clinic.util.CurrentUser;
import com.clinic.util.Enums;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication and authorisation: session login, the signed-in user, user accounts,
 * module access per role (NONE / READ / WRITE) and individual actions per role.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RolePermissionRepository permissionRepository;
    private final RoleActionRepository actionRepository;
    private final SettingsMapper mapper;
    private final CrudSupport crud;
    private final ActivityService activityService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository contextRepository;
    private final AuthenticationManager authenticationManager;
    private final ChangeSessionIdAuthenticationStrategy sessionStrategy = new ChangeSessionIdAuthenticationStrategy();

    private volatile Map<Role, Map<Module, Access>> accessCache;

    // ---- session -----------------------------------------------------------------------------

    /** Verifies the password, keeps the login in the HTTP session and returns the user with permissions. */
    public UserDto login(UserDto credentials, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(credentials.getUsername().trim(), credentials.getPassword()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        sessionStrategy.onAuthentication(auth, request, response);
        contextRepository.saveContext(context, request, response);
        return me();
    }

    @Transactional(readOnly = true)
    public UserDto me() {
        AppUser user = userRepository.findByUsernameIgnoreCaseAndDeletedFalse(CurrentUser.username().orElseThrow())
                .orElseThrow(() -> BusinessException.forbidden("User account not found"));
        UserDto dto = mapper.toDto(user);
        dto.setPermissions(forRole(user.getRole()));
        dto.setActions(actionsFor(user.getRole()));
        return dto;
    }

    // ---- users ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<UserDto> users(SearchFilter filter) {
        Boolean active = filter.getStatus() == null ? null : "ACTIVE".equalsIgnoreCase(filter.getStatus());
        Specification<AppUser> spec = Specs.all(Specs.containsAny(filter.getSearch(), "username", "fullName"),
                Specs.eq("role", Enums.parse(Role.class, filter.getType())), Specs.eq("active", active));
        return crud.page(userRepository, spec, filter.toPageable("fullName,asc"), mapper::toDto);
    }

    @Transactional
    public UserDto createUser(UserDto dto) {
        AppUser user = mapper.toEntity(dto);
        applyAccount(user, dto);
        return saveUser(user, "created");
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto dto) {
        AppUser user = crud.find(userRepository, id, "User");
        ensureAdminKept(user, dto.getRole() != Role.ADMIN || Boolean.FALSE.equals(dto.getActive()));
        mapper.updateEntity(dto, user);
        applyAccount(user, dto);
        return saveUser(user, "updated");
    }

    @Transactional
    public void deleteUser(Long id) {
        AppUser user = crud.find(userRepository, id, "User");
        if (isSelf(user)) {
            throw BusinessException.badRequest("You cannot delete your own account");
        }
        ensureAdminKept(user, true);
        user.setDeleted(true);
        saveUser(user, "deleted");
    }

    // ---- module access (role x module) ---------------------------------------------------------

    public Access access(Role role, Module module) {
        return forRole(role).getOrDefault(module, Access.NONE);
    }

    public Map<Module, Access> forRole(Role role) {
        return accessMatrix().getOrDefault(role, Map.of());
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> permissions() {
        return permissionRepository.findAll().stream().filter(permission -> !permission.isDeleted())
                .sorted(Comparator.comparing(RolePermission::getRole).thenComparing(RolePermission::getModule))
                .map(PermissionDto::of).toList();
    }

    @Transactional
    public List<PermissionDto> updatePermissions(List<PermissionDto> changes) {
        changes.forEach(this::applyPermission);
        accessCache = null;
        activityService.log("PERMISSION", 0L, null, "UPDATED", "Role permissions updated");
        return permissions();
    }

    // ---- actions (role x action) -----------------------------------------------------------------

    /** Administrators are always allowed, so the system cannot be locked. */
    @Transactional(readOnly = true)
    public boolean allowed(Role role, Action action) {
        return role == Role.ADMIN || actionRepository.findByRoleAndAction(role, action).map(RoleAction::isAllowed).orElse(false);
    }

    @Transactional(readOnly = true)
    public Set<Action> actionsFor(Role role) {
        return Arrays.stream(Action.values()).filter(action -> allowed(role, action))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Action.class)));
    }

    @Transactional(readOnly = true)
    public List<RoleActionDto> actions() {
        return actionRepository.findAll().stream()
                .sorted(Comparator.comparing(RoleAction::getRole).thenComparing(RoleAction::getAction))
                .map(RoleActionDto::of).toList();
    }

    @Transactional
    public List<RoleActionDto> updateActions(List<RoleActionDto> changes) {
        changes.stream().filter(change -> change.role() != Role.ADMIN).forEach(this::applyAction);
        activityService.log("PERMISSION", 0L, null, "UPDATED", "Role actions updated");
        return actions();
    }

    // ---- helpers ------------------------------------------------------------------------------------

    private void applyAccount(AppUser user, UserDto dto) {
        user.setActive(dto.getActive() == null || dto.getActive());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
    }

    /** Keeps at least one active administrator, and stops users removing their own admin access. */
    private void ensureAdminKept(AppUser user, boolean losesAdmin) {
        if (!losesAdmin || user.getRole() != Role.ADMIN) {
            return;
        }
        if (isSelf(user)) {
            throw BusinessException.badRequest("You cannot remove your own administrator access");
        }
        if (userRepository.countByRoleAndActiveTrueAndDeletedFalseAndIdNot(Role.ADMIN, user.getId()) == 0) {
            throw BusinessException.conflict("At least one active administrator is required");
        }
    }

    private boolean isSelf(AppUser user) {
        return CurrentUser.username().map(name -> name.equalsIgnoreCase(user.getUsername())).orElse(false);
    }

    private UserDto saveUser(AppUser user, String action) {
        AppUser saved = crud.save(userRepository, user);
        activityService.log("USER", saved.getId(), null, action.toUpperCase(), "User " + saved.getUsername() + " " + action);
        return mapper.toDto(saved);
    }

    private Map<Role, Map<Module, Access>> accessMatrix() {
        Map<Role, Map<Module, Access>> current = accessCache;
        if (current == null) {
            current = new EnumMap<>(Role.class);
            for (RolePermission permission : permissionRepository.findAll().stream().filter(p -> !p.isDeleted()).toList()) {
                current.computeIfAbsent(permission.getRole(), role -> new EnumMap<>(Module.class)).put(permission.getModule(), permission.getAccess());
            }
            accessCache = current;
        }
        return current;
    }

    private void applyPermission(PermissionDto change) {
        if (change.role() == Role.ADMIN && change.module() == Module.SETTINGS && change.access() != Access.WRITE) {
            throw BusinessException.badRequest("Administrators must keep full access to Settings");
        }
        RolePermission permission = permissionRepository.findByRoleAndModule(change.role(), change.module()).orElseGet(RolePermission::new);
        permission.setRole(change.role());
        permission.setModule(change.module());
        permission.setAccess(change.access());
        permissionRepository.save(permission);
    }

    private void applyAction(RoleActionDto change) {
        RoleAction entity = actionRepository.findByRoleAndAction(change.role(), change.action()).orElseGet(RoleAction::new);
        entity.setRole(change.role());
        entity.setAction(change.action());
        entity.setAllowed(change.allowed());
        actionRepository.save(entity);
    }
}
