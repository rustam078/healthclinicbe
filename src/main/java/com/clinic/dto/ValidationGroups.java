package com.clinic.dto;

import com.clinic.dto.ValidationGroups.OnCreate;
import com.clinic.dto.ValidationGroups.OnDischarge;
import com.clinic.dto.ValidationGroups.OnLogin;
import com.clinic.dto.ValidationGroups.OnUpdate;

import jakarta.validation.groups.Default;

/** Validation groups that let one DTO serve create, update and workflow requests. */
public final class ValidationGroups {

    private ValidationGroups() {
    }

    public interface OnCreate extends Default {
    }

    public interface OnUpdate extends Default {
    }

    public interface OnDischarge {
    }

    public interface OnLogin {
    }
}
