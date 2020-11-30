package com.yangjie.spring.annoation;

import java.lang.annotation.*;

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface YJRequestMapping {
    String value() default "";
}
