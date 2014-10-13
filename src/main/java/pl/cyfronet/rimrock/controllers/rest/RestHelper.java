package pl.cyfronet.rimrock.controllers.rest;

import java.util.stream.Collectors;

import org.springframework.validation.BindingResult;

public class RestHelper {
	public static String convertErrors(BindingResult errors) {
		return errors.getFieldErrors()
				.stream()
				.map(f -> {
					return f.getField() + ": " + String.join(", ", f.getCodes());
				})
				.collect(Collectors.joining("; "));
	}
}