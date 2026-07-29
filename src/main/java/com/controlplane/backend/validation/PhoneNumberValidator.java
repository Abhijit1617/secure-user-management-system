package com.controlplane.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+?[1-9]\\d{7,14}$");

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(phoneNumber)) {
            return true;
        }
        return E164_PATTERN.matcher(phoneNumber).matches();
    }
}
