package pl.cyfronet.rimrock.controllers.rest;

import java.nio.charset.Charset;
import java.util.Base64;
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

	public static String decodeProxy(String proxy) {
		return new String(Base64.getDecoder().decode(proxy), Charset.forName("utf-8"));
	}

	public static String encodeProxy(String proxy) {
		return Base64.getEncoder().encodeToString(proxy.replaceAll("\n", "\n").getBytes());
	}
}