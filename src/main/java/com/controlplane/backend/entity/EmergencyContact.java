package com.controlplane.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContact {

    @Column(name = "emergency_contact_name", length = 150)
    private String contactName;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String relationship;

    @Column(name = "emergency_contact_phone", length = 20)
    private String phoneNumber;

    @Column(name = "emergency_contact_alternate_phone", length = 20)
    private String alternatePhoneNumber;
}
