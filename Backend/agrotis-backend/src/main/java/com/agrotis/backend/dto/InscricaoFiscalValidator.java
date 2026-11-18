package com.agrotis.backend.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class InscricaoFiscalValidator implements ConstraintValidator<ValidInscricaoFiscal, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String digits = value.replaceAll("\\D", "");
        return digits.length() == 11 ? isValidCpf(digits)
            : digits.length() == 14 && isValidCnpj(digits);
    }

    private boolean isValidCpf(String cpf) {
        if (hasRepeatedDigits(cpf)) {
            return false;
        }

        int first = calculateDigit(cpf.substring(0, 9), new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2});
        int second = calculateDigit(cpf.substring(0, 9) + first, new int[]{11, 10, 9, 8, 7, 6, 5, 4, 3, 2});
        return cpf.endsWith("" + first + second);
    }

    private boolean isValidCnpj(String cnpj) {
        if (hasRepeatedDigits(cnpj)) {
            return false;
        }

        int first = calculateDigit(cnpj.substring(0, 12), new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int second = calculateDigit(cnpj.substring(0, 12) + first,
            new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return cnpj.endsWith("" + first + second);
    }

    private int calculateDigit(String value, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.getNumericValue(value.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private boolean hasRepeatedDigits(String digits) {
        return digits.chars().allMatch(character -> character == digits.charAt(0));
    }
}
