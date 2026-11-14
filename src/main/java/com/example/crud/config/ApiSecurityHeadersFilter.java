
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
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 对所有 HTTP 响应设置 MIME 类型保护
        response.setHeader(
                "X-Content-Type-Options",
                "nosniff"
        );

        // 2. 识别用户信息 API
        String requestPath = request.getServletPath();

        boolean isUserApi =
                USERS_API_PATH.equals(requestPath)
                        || requestPath.startsWith(USERS_API_PATH + "/");

        // 3. 禁止用户信息 API 响应被缓存
        if (isUserApi) {
            response.setHeader(
                    "Cache-Control",
                    "no-store"
            );
        }

        // 4. 继续执行后续过滤器及 Controller
        filterChain.doFilter(request, response);
    }
}
