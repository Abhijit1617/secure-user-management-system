package com.controlplane.backend.dto.auth;

import com.controlplane.backend.validation.StrongPassword;
import com.controlplane.backend.validation.UniqueEmail;
import com.controlplane.backend.validation.UniqueUsername;
import com.controlplane.backend.validation.ValidPhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
            message = "Username may only contain letters, digits, dots, underscores and hyphens")
    @UniqueUsername
    @Schema(example = "jane.doe")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @UniqueEmail
    @Schema(example = "jane.doe@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @StrongPassword
    @Schema(example = "SecurePass123!")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 80, message = "First name must not exceed 80 characters")
    @Schema(example = "Jane")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 80, message = "Last name must not exceed 80 characters")
    @Schema(example = "Doe")
    private String lastName;

    @ValidPhoneNumber
    @Schema(example = "+14155552671")
    private String phoneNumber;
}
