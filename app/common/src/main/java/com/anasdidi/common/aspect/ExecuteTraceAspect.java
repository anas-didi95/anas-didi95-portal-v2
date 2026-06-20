package com.anasdidi.common.aspect;

import com.anasdidi.common.CommonUtils;
import com.anasdidi.common.dto.BaseReqDTO;
import com.anasdidi.common.dto.BaseResDTO;
import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.common.error.E99UnexpectedError;
import com.anasdidi.common.error.ServiceError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ExecuteTraceAspect {

  private final ObjectMapper objectMapper;

  public ExecuteTraceAspect() {
    this.objectMapper = CommonUtils.prepareObjectMapper();
  }

  @Pointcut("execution(* com.anasdidi.common.service.IBaseService.execute(..))")
  void serviceExecution() {}

  @Around("serviceExecution()")
  @SuppressWarnings("unchecked")
  public Object traceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
    var timeStart = System.currentTimeMillis();
    var signature = joinPoint.getSignature().toShortString();
    var req = Arrays.stream(joinPoint.getArgs())
        .filter(o -> o instanceof BaseReqDTO)
        .map(o -> (BaseReqDTO) o)
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No parameter found!"));

    var method = (MethodSignature) joinPoint.getSignature();
    Class<? extends BaseResDTO> returnClass = method.getReturnType();
    var res =
        objectMapper.convertValue(Map.of("correlationId", req.getCorrelationId()), returnClass);

    try {
      MDC.put("traceId", req.getCorrelationId());
      MDC.put("spanId", timeStart + "");
      MDC.put("signature", signature);

      log.info("AOP Request: {}", Arrays.toString(joinPoint.getArgs()));

      res = (BaseResDTO) joinPoint.proceed();
    } catch (ConstraintViolationException e) {
      log.error(e.getMessage(), e);
      res.setResponse(ResponseEnum.E01_VALIDATION_ERROR);
    } catch (ServiceError e) {
      if (e instanceof E99UnexpectedError ee) {
        res.setResponse(ResponseEnum.E99_UNEXPECTED_ERROR);
        log.error("Unexpected error! {}", ee.getReason());

        if (ee.getEx() != null) {
          log.error(ee.getEx().getMessage(), ee.getEx());
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      res.setResponse(ResponseEnum.E99_UNEXPECTED_ERROR);
    } finally {
      var timeTaken = System.currentTimeMillis() - timeStart;
      res.setTraceId(timeStart + "");
      res.setTimestamp(OffsetDateTime.now());
      res.setTimeTaken(timeTaken);
      res.setResponseCode(res.getResponse().code);
      res.setResponseDesc(res.getResponse().message);

      log.info("AOP Response: {}", res);
      log.info("AOP Time taken: {} ms", timeTaken);

      MDC.clear();
    }

    return res;
  }
}
