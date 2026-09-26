package com.clinic.dto;

import com.clinic.dto.ValidationGroups.OnCreate;
import com.clinic.entity.Bed;
import com.clinic.entity.Room;
import com.clinic.enums.Access;
import com.clinic.enums.BedStatus;
import com.clinic.enums.RoomType;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BedDto extends AuditedDto {

    @NotNull(groups = OnCreate.class, message = "Room is required")
    private Long roomId;

    @NotBlank(message = "Bed number is required")
    @Size(max = 20, message = "Bed number must be at most 20 characters")
    private String bedNumber;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BedStatus status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String roomNumber;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private RoomType roomType;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal dailyCharge;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long currentIpdId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String currentIpdCode;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String currentPatientName;
}
