package com.clinic.service;

import com.clinic.dto.ClinicSettingsDto;
import com.clinic.dto.DoctorDto;
import com.clinic.dto.PageResponse;
import com.clinic.dto.SearchFilter;
import com.clinic.dto.TemplateDto;
import com.clinic.entity.ClinicSettings;
import com.clinic.entity.Doctor;
import com.clinic.entity.DocumentTemplate;
import com.clinic.enums.ImageType;
import com.clinic.enums.TemplateType;
import com.clinic.exception.BusinessException;
import com.clinic.mapper.SettingsMapper;
import com.clinic.repository.ClinicSettingsRepository;
import com.clinic.repository.DoctorRepository;
import com.clinic.repository.Specs;
import com.clinic.repository.TemplateRepository;
import com.clinic.util.Enums;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

/** Settings module: clinic details & branding, logo file, printable templates and the clinic's doctor. */
@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String LOGO_FOLDER = "clinic";

    private final ClinicSettingsRepository settingsRepository;
    private final TemplateRepository templateRepository;
    private final DoctorRepository doctorRepository;
    private final SettingsMapper mapper;
    private final CrudSupport crud;
    private final FileStorageService storage;
    private final ActivityService activityService;

    // ---- clinic details & logo ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public ClinicSettingsDto get() {
        return mapper.toDto(current());
    }

    @Transactional
    public ClinicSettingsDto update(ClinicSettingsDto dto) {
        ClinicSettings settings = current();
        mapper.updateEntity(dto, settings);
        if (settings.getSidebarMode() == null) {
            settings.setSidebarMode("FULL");
        }
        return saveSettings(settings, "Clinic settings updated");
    }

    /** Stores the logo file on disk (only its path goes to the database) and removes the previous one. */
    @Transactional
    public ClinicSettingsDto uploadLogo(MultipartFile file) {
        ImageType type = ImageType.detect(file);
        ClinicSettings settings = current();
        String previous = settings.getLogoPath();
        settings.setLogoPath(storage.store(open(file), LOGO_FOLDER, "logo-" + System.currentTimeMillis() + "." + type.extension()));
        ClinicSettingsDto saved = saveSettings(settings, previous == null ? "Clinic logo uploaded" : "Clinic logo replaced");
        storage.delete(previous);
        return saved;
    }

    @Transactional
    public ClinicSettingsDto removeLogo() {
        ClinicSettings settings = current();
        String previous = settings.getLogoPath();
        settings.setLogoPath(null);
        ClinicSettingsDto saved = saveSettings(settings, "Clinic logo removed");
        storage.delete(previous);
        return saved;
    }

    @Transactional(readOnly = true)
    public LogoFile loadLogo() {
        String path = current().getLogoPath();
        if (path == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "No logo has been uploaded");
        }
        return new LogoFile(storage.load(path), ImageType.fromPath(path).contentType());
    }

    // ---- templates ----------------------------------------------------------------------------

    @Transactional
    public List<TemplateDto> templates() {
        return Arrays.stream(TemplateType.values()).map(this::template).map(mapper::toDto).toList();
    }

    @Transactional
    public TemplateDto template(String type) {
        return mapper.toDto(template(Enums.require(TemplateType.class, type)));
    }

    @Transactional
    public TemplateDto updateTemplate(String type, TemplateDto dto) {
        ensureLogoFits(dto);
        TemplateType templateType = Enums.require(TemplateType.class, type);
        DocumentTemplate template = template(templateType);
        mapper.updateEntity(dto, template);
        return saveTemplate(template, Enums.label(templateType) + " template updated");
    }

    @Transactional
    public TemplateDto resetTemplate(String type) {
        TemplateType templateType = Enums.require(TemplateType.class, type);
        DocumentTemplate template = template(templateType);
        mapper.updateEntity(templateType.defaults(), template);
        return saveTemplate(template, Enums.label(templateType) + " template reset to default");
    }

    // ---- doctor -------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<DoctorDto> doctors(SearchFilter filter) {
        Boolean active = filter.getStatus() == null ? null : "ACTIVE".equalsIgnoreCase(filter.getStatus());
        var spec = Specs.<Doctor>all(Specs.containsAny(filter.getSearch(), "fullName", "specialization", "phone"), Specs.eq("active", active));
        return crud.page(doctorRepository, spec, filter.toPageable("fullName,asc"), mapper::toDto);
    }

    /** The clinic's doctor, or null when not set up yet. */
    @Transactional(readOnly = true)
    public DoctorDto clinicDoctorOrNull() {
        return doctorRepository.findFirstByActiveTrueAndDeletedFalseOrderByIdAsc().map(mapper::toDto).orElse(null);
    }

    /** The clinic's single doctor, used automatically for appointments and admissions. */
    public Doctor clinicDoctor() {
        return doctorRepository.findFirstByActiveTrueAndDeletedFalseOrderByIdAsc()
                .orElseThrow(() -> BusinessException.badRequest("Add the doctor's details in Settings → Doctor & fees first"));
    }

    @Transactional
    public DoctorDto createDoctor(DoctorDto dto) {
        if (doctorRepository.countByDeletedFalse() > 0) {
            throw BusinessException.conflict("The clinic has one doctor. Edit the existing doctor's details instead");
        }
        Doctor doctor = mapper.toEntity(dto);
        doctor.setActive(true);
        return saveDoctor(doctor, "Doctor added");
    }

    @Transactional
    public DoctorDto updateDoctor(Long id, DoctorDto dto) {
        Doctor doctor = crud.find(doctorRepository, id, "Doctor");
        mapper.updateEntity(dto, doctor);
        doctor.setActive(dto.getActive() == null || dto.getActive());
        return saveDoctor(doctor, "Doctor details updated");
    }

    // ---- helpers ------------------------------------------------------------------------------

    private ClinicSettings current() {
        return settingsRepository.findFirstByDeletedFalseOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Clinic settings row is missing"));
    }

    private ClinicSettingsDto saveSettings(ClinicSettings settings, String description) {
        ClinicSettings saved = settingsRepository.saveAndFlush(settings);
        activityService.log("CLINIC_SETTINGS", saved.getId(), null, "UPDATED", description);
        return mapper.toDto(saved);
    }

    /** The stored template of a type, created from its professional defaults when missing. */
    private DocumentTemplate template(TemplateType type) {
        return templateRepository.findByTemplateTypeAndDeletedFalse(type).orElseGet(() -> {
            DocumentTemplate template = mapper.toEntity(type.defaults());
            template.setTemplateType(type);
            return templateRepository.save(template);
        });
    }

    /** The logo area must fit inside the header and the printable width. */
    private void ensureLogoFits(TemplateDto dto) {
        if (dto.getLogoAreaHeight() > dto.getHeaderHeight()) {
            throw BusinessException.badRequest("Logo area height cannot be larger than the header height");
        }
        if (dto.getLogoAreaWidth() > dto.getPageWidth() - 2 * dto.getPadding()) {
            throw BusinessException.badRequest("Logo area width does not fit inside the page width and padding");
        }
    }

    private TemplateDto saveTemplate(DocumentTemplate template, String description) {
        DocumentTemplate saved = templateRepository.saveAndFlush(template);
        activityService.log("TEMPLATE", saved.getId(), null, "UPDATED", description);
        return mapper.toDto(saved);
    }

    private DoctorDto saveDoctor(Doctor doctor, String description) {
        Doctor saved = crud.save(doctorRepository, doctor);
        activityService.log("DOCTOR", saved.getId(), null, "UPDATED", description);
        return mapper.toDto(saved);
    }

    private InputStream open(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public record LogoFile(Resource resource, String contentType) {
    }
}
