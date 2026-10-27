package app.sewain.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
class GlobalExceptionHandler {

	@ExceptionHandler(ValidationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleValidation(
			ValidationException ex,
			HttpServletRequest request
	) {
		return new ErrorResponse(
				LocalDateTime.now(ZoneId.systemDefault()),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI(),
				Collections.emptyMap()
		);
	}


    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return new ErrorResponse(LocalDateTime.now(ZoneId.systemDefault()),
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(), request.getRequestURI(), Collections.emptyMap());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        return new ErrorResponse(LocalDateTime.now(ZoneId.systemDefault()),
                HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(), request.getRequestURI(), Collections.emptyMap());
    }

	@ExceptionHandler(ConflictException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleConflict(
			ConflictException ex,
			HttpServletRequest request
	) {
		return new ErrorResponse(
				LocalDateTime.now(ZoneId.systemDefault()),
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI(),
				Collections.emptyMap()
		);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ErrorResponse handleGeneralException(
			Exception ex,
			HttpServletRequest request
	) {
		log.error("General Exception", ex);
		return new ErrorResponse(
				LocalDateTime.now(ZoneId.systemDefault()),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI(),
				Collections.emptyMap()
		);
	}

	@ExceptionHandler(BadCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ErrorResponse handleResourceNotFound(
			BadCredentialsException ex,
			HttpServletRequest request
	) {
		return new ErrorResponse(
				LocalDateTime.now(ZoneId.systemDefault()),
				HttpStatus.UNAUTHORIZED.value(),
				HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				"user.invalidCredentials",
				request.getRequestURI(),
				Collections.singletonMap("user", "user.invalidCredentials")
		);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpServletRequest request
	) {
		log.error(ex.getMessage(), ex);
		Map<String, String> errors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						this::resolveErrorCode,
						(first, second) -> first,
						LinkedHashMap::new
				));


		return new ErrorResponse(
				LocalDateTime.now(ZoneId.systemDefault()),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Validation failed",
				request.getRequestURI(),
				errors
		);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleDataIntegrityViolation(
			DataIntegrityViolationException ex,
			HttpServletRequest request
	) {
		String errorCode = resolveErrorCode(ex);

		return new ErrorResponse(
				LocalDateTime.now(ZoneId.systemDefault()),
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				errorCode,
				request.getRequestURI(),
				Collections.emptyMap()
		);
	}

	private String resolveErrorCode(
			DataIntegrityViolationException ex
	) {
		if (ex.getMostSpecificCause() instanceof PSQLException psqlException) {
			String constraint = null;
			if (psqlException
					.getServerErrorMessage() != null) {
				constraint = psqlException
						.getServerErrorMessage()
						.getConstraint();
			}
			log.info(constraint);
			if (constraint != null) {
				return switch (constraint) {
					case "uk_attribute_values_attribute_slug" -> ErrorCode.ATTRIBUTE_VALUE_SLUG_ALREADY_EXISTS;
					case "fk_categories_parent" -> ErrorCode.CATEGORY_IN_USE;
					case "inventory_units_sku_barcode_key" -> ErrorCode.INVENTORY_UNIT_SKU_ALREADY_EXISTS;
					case "fk_rental_allocations_inventory_unit" -> ErrorCode.INVENTORY_UNIT_DELETE_ALLOCATED;
					default -> "dataIntegrityViolation";
				};
			}
		}


		return "dataIntegrityViolation";
	}


	private String resolveErrorCode(FieldError error) {
		String errorCode = Objects.requireNonNull(error.getCode());
		String field = Objects.requireNonNull(error.getField());
		return String.format("%s.%s", field.toLowerCase(), errorCode.toLowerCase());
	}
}
