package vn.edu.fpt.mss.engagementservice.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RatingRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void ratingValue_whenOutOFRange_failsValidation() {
        RatingRequest lowRating = new RatingRequest(10, (byte) 0);
        Set<ConstraintViolation<RatingRequest>> violationsLow = validator.validate(lowRating);
        assertFalse(violationsLow.isEmpty(), "Phải báo lỗi khi điểm rating < 1");

        RatingRequest highRating = new RatingRequest(10, (byte) 6);
        Set<ConstraintViolation<RatingRequest>> violationsHigh = validator.validate(highRating);
        assertFalse(violationsHigh.isEmpty(), "Phải báo lỗi khi điểm rating > 5");
    }
}