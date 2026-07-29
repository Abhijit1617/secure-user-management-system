package com.controlplane.backend.dto.employee;

import com.controlplane.backend.entity.enums.EmployeeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeStatusRequest {

    @NotNull(message = "Status is required")
    private EmployeeStatus status;

    /**
     * Required when transitioning to RESIGNED or TERMINATED; ignored
     * otherwise.
     */
    private LocalDate dateOfExit;
}
