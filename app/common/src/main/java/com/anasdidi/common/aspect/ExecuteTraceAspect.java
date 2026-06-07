package com.anasdidi.common.aspect;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Aspect
@Component
public class ExecuteTraceAspect {

  @Pointcut("execution(* com.anasdidi.common.IBaseService.execute(..))")
  void serviceExecution() {}

  @Around("serviceExecution()")
  public Object traceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
    String signature = joinPoint.getSignature().toShortString();

    if (log.isInfoEnabled()) {
      log.info("[{}] Request: {}", signature, Arrays.toString(joinPoint.getArgs()));
    }

    Object result = joinPoint.proceed();

    if (!(result instanceof Mono<?> mono)) {
      log.warn("[{}] returned non-Mono, skipping response log", signature);
      return result;
    }

    mono = mono.doOnSuccess(res -> log.info("[{}] Response: {}", signature, res))
        .doOnError(err -> log.error("[{}] Execution error", signature, err));
    return mono;
  }
}
