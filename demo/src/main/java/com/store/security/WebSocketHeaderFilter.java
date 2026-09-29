package com.store.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Filter để loại bỏ header Sec-WebSocket-Extensions (permessage-deflate)
 * trên các endpoint WebSocket.
 * Tránh lỗi "Invalid frame header" do xung đột nén giữa Tomcat, Nginx và trình duyệt.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSocketHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest) {
            String uri = httpRequest.getRequestURI();
            if (uri != null && (uri.startsWith("/ws-chat") || uri.startsWith("/ws"))) {
                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(httpRequest) {
                    @Override
                    public String getHeader(String name) {
                        if ("Sec-WebSocket-Extensions".equalsIgnoreCase(name)) {
                            return null;
                        }
                        return super.getHeader(name);
                    }

                    @Override
                    public Enumeration<String> getHeaders(String name) {
                        if ("Sec-WebSocket-Extensions".equalsIgnoreCase(name)) {
                            return Collections.enumeration(Collections.emptyList());
                        }
                        return super.getHeaders(name);
                    }
                };
                chain.doFilter(wrappedRequest, response);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
