package com.clinic.dto;

import com.clinic.entity.Room;
import com.clinic.enums.Access;
import com.clinic.enums.RoomType;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class RoomDto extends AuditedDto {

    @NotBlank(message = "Room number is required")
    @Size(max = 20, message = "Room number must be at most 20 characters")
    private String roomNumber;

    @NotNull(message = "Room type is required")
    private RoomType roomType;

    @Size(max = 20, message = "Floor must be at most 20 characters")
    private String floor;

    @NotNull(message = "Daily charge is required (use 0 if none)")
    @PositiveOrZero(message = "Daily charge cannot be negative")
    @DecimalMax(value = "99999999.99", message = "Daily charge is too large")
    private BigDecimal dailyCharge;

    private Boolean active;

    /** Create only: number of beds to add automatically (numbered 1..n). */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Min(value = 0, message = "Beds cannot be negative")
    @Max(value = 50, message = "At most 50 beds can be added at once")
    private Integer initialBeds;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<BedDto> beds;
}
