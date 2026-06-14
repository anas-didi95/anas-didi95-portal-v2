package com.anasdidi.uam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.HelloWorldReqDTO;
import com.anasdidi.uam.dto.HelloWorldReqDTO.HelloWorldReqDTOPayload;
import com.anasdidi.uam.dto.HelloWorldResDTO;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HelloWorldServiceTests {

  @Autowired
  private HelloWorldService helloWorldService;

  @Test
  void testGreeting() {
    var req = HelloWorldReqDTO.builder()
        .correlationId("corr-123")
        .payload(HelloWorldReqDTOPayload.builder().name("John").build())
        .build();

    HelloWorldResDTO result = helloWorldService.execute(req);

    assertNotNull(result);
    assertEquals("corr-123", result.getCorrelationId());
    assertEquals("Hi, John", result.getPayload().getGreeting());
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("00", result.getResponseCode());
    assertEquals("Success", result.getResponseDesc());
  }

  @Test
  void testGreeting_missingCorrelationId_throwsConstraintViolationException() {
    var req = HelloWorldReqDTO.builder()
        .correlationId("")
        .payload(HelloWorldReqDTOPayload.builder().name("John").build())
        .build();

    assertThrows(ConstraintViolationException.class, () -> helloWorldService.execute(req));
  }

  @Test
  void testGreeting_missingName_throwsConstraintViolationException() {
    var req = HelloWorldReqDTO.builder()
        .correlationId("corr-123")
        .payload(HelloWorldReqDTOPayload.builder().name("").build())
        .build();

    assertThrows(ConstraintViolationException.class, () -> helloWorldService.execute(req));
  }
}
