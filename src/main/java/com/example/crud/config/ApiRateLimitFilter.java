package com.example.crud.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 60;

    private static final long WINDOW_MILLIS = 60_000;

    //保存不同 IP 的请求计数，支持并发访问
    private final ConcurrentHashMap<String, Window> requestCounts =
            new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // 只限制用户 API
        if (!path.equals("/api/users")
                && !path.startsWith("/api/users/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 健康检查接口暂不限制
        if ("/api/users/health".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 使用服务端获取的远程 IP，不直接信任客户端传入的 X-Forwarded-For
        String clientIp = request.getRemoteAddr();

        long now = System.currentTimeMillis();

        //保证多个并发请求更新计数时的原子性
        AtomicInteger remaining = new AtomicInteger();
        //告诉客户端大约还需要等待多少秒。
        AtomicLong retryAfter = new AtomicLong();

        boolean[] allowed = {false};

        requestCounts.compute(clientIp, (ip, window) -> {

            // 新窗口或旧窗口已过期
            if (window == null
                    || now - window.startTime >= WINDOW_MILLIS) {

                Window newWindow = new Window(now, new AtomicInteger(1));

                remaining.set(MAX_REQUESTS - 1);
                allowed[0] = true;

                return newWindow;
            }

            int count = window.count.incrementAndGet();

            if (count <= MAX_REQUESTS) {
                remaining.set(MAX_REQUESTS - count);
                allowed[0] = true;
            } else {
                long seconds = (window.startTime
                        + WINDOW_MILLIS - now + 999) / 1000;

                retryAfter.set(Math.max(1, seconds));
                remaining.set(0);
            }

            return window;
        });

        // 定期清理过期窗口，避免 IP 记录无限增长
        if (requestCounts.size() > 10000) {
            requestCounts.entrySet().removeIf(entry ->
                    now - entry.getValue().startTime >= WINDOW_MILLIS);
        }

        response.setHeader("X-RateLimit-Limit",
                String.valueOf(MAX_REQUESTS));

        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(remaining.get()));

        if (!allowed[0]) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());;
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After",
                    String.valueOf(retryAfter.get()));

            response.getWriter().write(
                    "{\"status\":429,"
                            + "\"error\":\"Too Many Requests\","
                            + "\"message\":\"请求过于频繁，请稍后重试\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static class Window {

        private final long startTime;

        private final AtomicInteger count;

        private Window(long startTime, AtomicInteger count) {
            this.startTime = startTime;
            this.count = count;
        }
    }
}

