package com.store.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolver để trích xuất địa chỉ Client IP an toàn.
 * <p>
 * Với cấu hình {@code server.forward-headers-strategy: native} và {@code server.tomcat.remoteip.*},
 * Tomcat RemoteIpValve chỉ tin cậy header X-Forwarded-For từ internal-proxies đã cấu hình,
 * và tự động gán client IP thực vào {@link HttpServletRequest#getRemoteAddr()}.
 * <p>
 * Do đó, không tự parse chuỗi header thủ công để tránh nguy cơ HTTP Header Injection / IP Spoofing.
 */
@Component
public class ClientIpResolver {

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr.trim() : "unknown";
    }
}
