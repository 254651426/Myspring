package com.yangjie.spring.annoation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface YJRequestParam {
    String value() default "";
}
