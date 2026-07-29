package com.controlplane.backend.mapper;

import com.controlplane.backend.dto.employee.AddressDto;
import com.controlplane.backend.dto.employee.EmergencyContactDto;
import com.controlplane.backend.dto.employee.EmployeeResponse;
import com.controlplane.backend.dto.employee.EmployeeSummaryResponse;
import com.controlplane.backend.entity.Address;
import com.controlplane.backend.entity.EmergencyContact;
import com.controlplane.backend.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeMapper {

    private final UserMapper userMapper;

    public EmployeeResponse toResponse(Employee employee) {
        if (employee == null) {
            return null;
        }
        Employee manager = employee.getReportingManager();
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .organizationId(employee.getOrganization().getId())
                .user(userMapper.toSummaryResponse(employee.getUser()))
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .designation(employee.getDesignation())
                .employmentType(employee.getEmploymentType().name())
                .status(employee.getStatus().name())
                .dateOfJoining(employee.getDateOfJoining())
                .dateOfExit(employee.getDateOfExit())
                .reportingManagerId(manager != null ? manager.getId() : null)
                .reportingManagerName(manager != null && manager.getUser() != null
                        ? manager.getUser().getFullName() : null)
                .annualCtc(employee.getAnnualCtc())
                .address(toAddressDto(employee.getAddress()))
                .emergencyContact(toEmergencyContactDto(employee.getEmergencyContact()))
                .createdAt(employee.getCreatedAt())
                .build();
    }

    public EmployeeSummaryResponse toSummaryResponse(Employee employee) {
        if (employee == null) {
            return null;
        }
        return EmployeeSummaryResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getUser() != null ? employee.getUser().getFullName() : null)
                .designation(employee.getDesignation())
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .status(employee.getStatus().name())
                .build();
    }

    public Address toAddressEntity(AddressDto dto) {
        if (dto == null) {
            return Address.builder().build();
        }
        return Address.builder()
                .addressLine1(dto.getAddressLine1())
                .addressLine2(dto.getAddressLine2())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .build();
    }

    public EmergencyContact toEmergencyContactEntity(EmergencyContactDto dto) {
        if (dto == null) {
            return EmergencyContact.builder().build();
        }
        return EmergencyContact.builder()
                .contactName(dto.getContactName())
                .relationship(dto.getRelationship())
                .phoneNumber(dto.getPhoneNumber())
                .alternatePhoneNumber(dto.getAlternatePhoneNumber())
                .build();
    }

    private AddressDto toAddressDto(Address address) {
        if (address == null) {
            return null;
        }
        return AddressDto.builder()
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .build();
    }

    private EmergencyContactDto toEmergencyContactDto(EmergencyContact contact) {
        if (contact == null) {
            return null;
        }
        return EmergencyContactDto.builder()
                .contactName(contact.getContactName())
                .relationship(contact.getRelationship())
                .phoneNumber(contact.getPhoneNumber())
                .alternatePhoneNumber(contact.getAlternatePhoneNumber())
                .build();
    }
}
