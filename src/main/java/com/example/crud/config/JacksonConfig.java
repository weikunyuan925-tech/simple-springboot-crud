
package com.example.crud.config;

import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer strictStringCoercion() {

        return builder -> builder.withCoercionConfig(
                LogicalType.Textual,
                coercions -> {

                    // 禁止整数转换为 String
                    coercions.setCoercion(
                            CoercionInputShape.Integer,
                            CoercionAction.Fail
                    );

                    // 禁止浮点数转换为 String
                    coercions.setCoercion(
                            CoercionInputShape.Float,
                            CoercionAction.Fail
                    );

                    // 禁止布尔值转换为 String
                    coercions.setCoercion(
                            CoercionInputShape.Boolean,
                            CoercionAction.Fail
                    );

                    // 禁止数组转换为 String
                    coercions.setCoercion(
                            CoercionInputShape.Array,
                            CoercionAction.Fail
                    );

                    // 禁止对象转换为 String
                    coercions.setCoercion(
                            CoercionInputShape.Object,
                            CoercionAction.Fail
                    );
                }
        );
    }
}
