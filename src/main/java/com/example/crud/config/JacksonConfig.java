
package com.example.crud.config;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictStringCoercion() {

        return builder -> builder.postConfigurer(objectMapper -> {

            // 针对 String 类型设置严格的 JSON 类型转换规则
            // 指定配置针对 Java 文本类型，也就是 String 等文本类型。
            objectMapper.coercionConfigFor(LogicalType.Textual)

                    // 禁止数字转换为 String
                    .setCoercion(
                            CoercionInputShape.Integer,
                            CoercionAction.Fail
                    )

                    // 禁止浮点数转换为 String
                    .setCoercion(
                            CoercionInputShape.Float,
                            CoercionAction.Fail
                    )

                    // 禁止布尔值转换为 String
                    .setCoercion(
                            CoercionInputShape.Boolean,
                            CoercionAction.Fail
                    )

                    // 禁止数组转换为 String
                    .setCoercion(
                            CoercionInputShape.Array,
                            CoercionAction.Fail
                    )

                    // 禁止对象转换为 String
                    .setCoercion(
                            CoercionInputShape.Object,
                            CoercionAction.Fail
                    );
        });
    }
}