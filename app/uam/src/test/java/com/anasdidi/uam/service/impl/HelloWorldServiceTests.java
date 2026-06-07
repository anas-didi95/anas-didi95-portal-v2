package com.anasdidi.uam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.HelloWorldReqDTO2;
import com.anasdidi.uam.dto.HelloWorldReqDTO2.HelloWorldReqDTO2Payload;
import com.anasdidi.uam.dto.HelloWorldResDTO2;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HelloWorldServiceTests {

  @Autowired
  private HelloWorldService helloWorldService;

  @Test
  void testExecute() {
    HelloWorldReqDTO2 req = HelloWorldReqDTO2.builder()
        .correlationId("corr-123")
        .payload(HelloWorldReqDTO2Payload.builder().name("John").build())
        .build();

    HelloWorldResDTO2 result = helloWorldService.execute(req).block();

    assertNotNull(result);
    assertEquals("corr-123", result.getCorrelationId());
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertNotNull(result.getPayload());
    assertEquals("Hi, John", result.getPayload().getGreeting());
  }

  @Test
  void testExecute_blankName_throwsConstraintViolation() {
    HelloWorldReqDTO2 req = HelloWorldReqDTO2.builder()
        .correlationId("corr-123")
        .payload(HelloWorldReqDTO2Payload.builder().name("").build())
        .build();

    ConstraintViolationException ex = assertThrows(ConstraintViolationException.class, () -> {
      helloWorldService.execute(req).block();
    });

    assertTrue(ex.getMessage().contains("name"));
  }

  @Test
  void testExecute_blankCorrelationId_throwsConstraintViolation() {
    HelloWorldReqDTO2 req = HelloWorldReqDTO2.builder()
        .correlationId("")
        .payload(HelloWorldReqDTO2Payload.builder().name("John").build())
        .build();

    assertThrows(ConstraintViolationException.class, () -> {
      helloWorldService.execute(req).block();
    });
  }
}
