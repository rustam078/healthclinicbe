package com.clinic.entity;

import com.clinic.enums.RoomType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {

    private String roomNumber;

    @Enumerated(EnumType.STRING)
    private RoomType roomType;

    private String floor;
    private BigDecimal dailyCharge = BigDecimal.ZERO;
    private boolean active = true;

    @OneToMany(mappedBy = "room")
    @SQLRestriction("deleted = false")
    @OrderBy("bedNumber")
    private List<Bed> beds = new ArrayList<>();
}
