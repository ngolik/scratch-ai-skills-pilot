package com.example.scratch;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Rejects request bodies above {@link #MAX_CONTENT_LENGTH_BYTES} before they reach any
 * controller, so an unauthenticated caller cannot force the JVM to buffer an oversized JSON
 * body in memory ahead of Bean Validation's {@code @Size} check. Applies to every endpoint
 * (registered with no path restriction, so both {@code /api/v1/greetings} and
 * {@code /api/v1/farewells} are covered by one filter).
 *
 * <p>Only guards requests that declare {@code Content-Length}; a chunked request with no
 * declared length is not capped by this filter.
 */
@Component
public class RequestSizeLimitFilter implements Filter {

    static final long MAX_CONTENT_LENGTH_BYTES = 4096;
    private static final String PAYLOAD_TOO_LARGE_BODY =
            "{\"error\":\"payload_too_large\",\"details\":[{\"field\":\"body\",\"message\":\"must be at most 4096 bytes\"}]}";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest
                && response instanceof HttpServletResponse httpResponse
                && httpRequest.getContentLengthLong() > MAX_CONTENT_LENGTH_BYTES) {
            httpResponse.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.getWriter().write(PAYLOAD_TOO_LARGE_BODY);
            return;
        }
        chain.doFilter(request, response);
    }
}
