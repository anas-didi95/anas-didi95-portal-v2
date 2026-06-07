package com.anasdidi.uam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  void testExecute() {
    HelloWorldReqDTO req = HelloWorldReqDTO.builder()
        .correlationId("corr-123")
        .payload(HelloWorldReqDTOPayload.builder().name("John").build())
        .build();

    HelloWorldResDTO result = helloWorldService.execute(req).block();

    assertNotNull(result);
    assertEquals("corr-123", result.correlationId());
    assertEquals(ResponseEnum.S00_SUCCESS, result.response());
    assertNotNull(result.payload());
    assertEquals("Hi, John", result.payload().greeting());
  }

  @Test
  void testExecute_blankName_throwsConstraintViolation() {
    HelloWorldReqDTO req = HelloWorldReqDTO.builder()
        .correlationId("corr-123")
        .payload(HelloWorldReqDTOPayload.builder().name("").build())
        .build();

    ConstraintViolationException ex = assertThrows(ConstraintViolationException.class, () -> {
      helloWorldService.execute(req).block();
    });

    assertTrue(ex.getMessage().contains("name"));
  }

  @Test
  void testExecute_blankCorrelationId_throwsConstraintViolation() {
    HelloWorldReqDTO req = HelloWorldReqDTO.builder()
        .correlationId("")
        .payload(HelloWorldReqDTOPayload.builder().name("John").build())
        .build();

    assertThrows(ConstraintViolationException.class, () -> {
      helloWorldService.execute(req).block();
    });
  }
}
