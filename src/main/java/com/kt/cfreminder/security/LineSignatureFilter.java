package com.kt.cfreminder.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class LineSignatureFilter implements Filter {
    private static final int CACHE_LIMIT = 64 * 1024;

    @Value("${line.bot.channel-secret}")
    private String channelSecret;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest,CACHE_LIMIT);

        String signature = wrappedRequest.getHeader("x-line-signature");

        // ข้ามการเช็คถ้าไม่ใช่ Path ของ Webhook (เช่น API อื่นๆ)
        if (!wrappedRequest.getRequestURI().startsWith("/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // ต้องรัน doFilter ก่อนเพื่อให้ ContentCachingRequestWrapper เก็บข้อมูล body
            filterChain.doFilter(wrappedRequest, response);

            byte[] body = wrappedRequest.getContentAsByteArray();
            if (!validateSignature(body, signature)) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Signature");
            }
        } catch (Exception e) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private boolean validateSignature(byte[] body, String signature) throws NoSuchAlgorithmException, InvalidKeyException {
        if (signature == null || body.length == 0) return false;

        SecretKeySpec key = new SecretKeySpec(channelSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        byte[] source = mac.doFinal(body);
        String createdSignature = Base64.getEncoder().encodeToString(source);

        return createdSignature.equals(signature);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
