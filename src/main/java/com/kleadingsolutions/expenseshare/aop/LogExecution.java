package com.kleadingsolutions.expenseshare.aop;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogExecution {
    boolean includeArgs() default false;
    boolean includeResult() default false;
    long warnThresholdMs() default 400;
    String description() default "";
}