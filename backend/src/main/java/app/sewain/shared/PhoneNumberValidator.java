package app.sewain.shared;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

	private static final Pattern PHONE_PATTERN =
			Pattern.compile("^\\+[1-9][0-9]{7,14}$");

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}

		String phone = value.replaceAll("[\\s-]", "");

		return PHONE_PATTERN.matcher(phone).matches();
	}
}