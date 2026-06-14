package com.anasdidi.common.aspect;

import com.anasdidi.common.BaseReqDTO;
import com.anasdidi.common.BaseResDTO;
import java.time.OffsetDateTime;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ExecuteTraceAspect {

  @Pointcut("execution(* com.anasdidi.common.IBaseService.execute(..))")
  void serviceExecution() {}

  @Around("serviceExecution()")
  public Object traceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
    var timeStart = System.currentTimeMillis();
    var signature = joinPoint.getSignature().toShortString();
    var req = Arrays.stream(joinPoint.getArgs())
        .filter(o -> o instanceof BaseReqDTO)
        .map(o -> (BaseReqDTO) o)
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No parameter found!"));

    try {
      MDC.put("traceId", req.getCorrelationId());
      MDC.put("spanId", timeStart + "");
      MDC.put("signature", signature);

      log.info("AOP Request: {}", Arrays.toString(joinPoint.getArgs()));

      Object result = joinPoint.proceed();

      var oo = (BaseResDTO) result;
      var timeTaken = System.currentTimeMillis() - timeStart;
      oo.setTraceId(timeStart + "");
      oo.setTimestamp(OffsetDateTime.now());
      oo.setTimeTaken(timeTaken);
      oo.setResponseCode(oo.getResponse().code);
      oo.setResponseDesc(oo.getResponse().message);

      log.info("AOP Response: {}", oo);
      log.info("AOP Time taken: {} ms", timeTaken);

      return oo;
    } finally {
      MDC.clear();
    }
  }
}
