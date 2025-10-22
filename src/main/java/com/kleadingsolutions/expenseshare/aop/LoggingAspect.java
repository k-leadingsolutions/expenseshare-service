package com.kleadingsolutions.expenseshare.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;



@Slf4j
@RequiredArgsConstructor
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("within(com.kleadingsolutions.expenseshare.service..*)")
    public void serviceLayer() {}

    /**
     * Aspect to provide logging on service layer performance
     * @param pjp
     * @return
     * @throws Throwable
     */
    @Around("serviceLayer()")
    public Object time(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();

        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();

        LogExecution ann = AnnotationUtils.findAnnotation(method, LogExecution.class);

        boolean includeArgs   = ann != null && ann.includeArgs();
        boolean includeResult = ann != null && ann.includeResult();
        long warnThresholdMs  = ann != null ? ann.warnThresholdMs() : 400L;

        String methodName = sig.getDeclaringTypeName() + "." + sig.getName();

        if (includeArgs && log.isDebugEnabled()) {
            log.debug("ENTER {} args={}", methodName, abbreviateArgs(pjp.getArgs()));
        } else if (log.isTraceEnabled()) {
            log.trace("ENTER {}", methodName);
        }

        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable t) {
            long elapsed = elapsedMs(start);
            log.warn("EXCEPTION {} after {} ms: {}", methodName, elapsed, t.toString());
            throw t;
        }

        long elapsed = elapsedMs(start);

        if (elapsed >= warnThresholdMs) {
            log.warn("SLOW {} took {} ms", methodName, elapsed);
        }

        if (includeResult && log.isDebugEnabled() && result != null) {
            log.debug("EXIT {} in {} ms result={}", methodName, elapsed, abbreviate(result));
        } else if (log.isTraceEnabled()) {
            log.trace("EXIT {} in {} ms", methodName, elapsed);
        }

        return result;
    }

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }

    private String abbreviate(Object o) {
        if (o == null) return "null";
        String s = o.toString();
        return (s.length() > 250) ? s.substring(0, 247) + "..." : s;
    }

    private String abbreviateArgs(Object[] args) {
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg instanceof String && isSensitive(arg)) {
                        return "****";
                    }
                    return abbreviate(arg);
                })
                .toList()
                .toString();
    }

    private boolean isSensitive(Object arg) {
        if (arg == null) {
            return false;
        }
        String argString = arg.toString().toLowerCase();
        return argString.contains("password") ||
                argString.contains("token") ||
                argString.contains("apikey") ||
                argString.contains("api_key") ||
                argString.contains("secret") ||
                argString.matches(".*[a-zA-Z0-9_-]{20,}.*");
    }
}