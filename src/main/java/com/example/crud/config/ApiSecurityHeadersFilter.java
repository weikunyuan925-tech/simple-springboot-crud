package com.example.crud.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//@Component 让 Spring 自动创建这个 Filter Bean，不需要手动实例化。
@Component
//在支持排序的集合中，排第一个的意思，值越小越先执行
@Order(Ordered.HIGHEST_PRECEDENCE)
//OncePerRequestFilter是 Spring 提供的过滤器基类，通常保证同一个请求在一次正常过滤流程中只执行一次过滤逻辑。
public class ApiSecurityHeadersFilter extends OncePerRequestFilter {

    private static final String USERS_API_PATH = "/api/users";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. 对所有 HTTP 响应设置 MIME 类型保护，防止浏览器进行 MIME 类型嗅探
        response.setHeader("X-Content-Type-Options", "nosniff");

       // 防止 API 响应被浏览器缓存
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store");
        }

        // 禁止页面被其他网站通过 iframe 嵌入
        response.setHeader("X-Frame-Options", "DENY");

        // 限制 Referer 请求头携带来源信息
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 禁止当前项目使用不需要的浏览器功能
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        // 当前项目是 API 项目，不提供网页资源，使用了较严格的策略，因为当前项目是纯 API 项目。如果以后增加 HTML 页面、前端资源或 Swagger UI，需要重新评估这条策略。
        response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'");

        // 4. 继续执行后续过滤器及 Controller
        filterChain.doFilter(request, response);
    }
}
