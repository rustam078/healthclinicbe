package com.clinic.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "doctors")
public class Doctor extends BaseEntity {

    private String fullName;
    private String specialization;
    private String phone;
    private String email;
    private String facilities;
    private String signature;
    private boolean active = true;
}
