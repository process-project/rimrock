package pl.cyfronet.rimrock;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class RequestLoggingInterceptor extends HandlerInterceptorAdapter {
	private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
	
	private static final String REQUEST_ID_ATTRIBUTE = "RIMROCK_REQUEST_ID_ATTRIBUTE";
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String method = request.getMethod();
		String path = request.getRequestURI();
		String uuid = UUID.randomUUID().toString();
		request.setAttribute(REQUEST_ID_ATTRIBUTE, uuid);
		log.info("API request for {} {} with id {} started", method, path, uuid);
		
		return true;
	}
	
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		log.info("API request with id {} finished with status {}", request.getAttribute(REQUEST_ID_ATTRIBUTE), response.getStatus());
	}
}