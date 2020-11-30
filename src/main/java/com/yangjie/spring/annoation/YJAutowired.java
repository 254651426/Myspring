package com.yangjie.spring.annoation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})//作用于字段上面
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface YJAutowired {
    String value() default "";
}
