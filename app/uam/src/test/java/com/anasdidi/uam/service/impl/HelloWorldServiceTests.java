package com.anasdidi.uam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.HelloWorldReqDTO;
import com.anasdidi.uam.dto.HelloWorldReqDTO.HelloWorldReqDTOPayload;
import com.anasdidi.uam.dto.HelloWorldResDTO;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

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
  void testGreeting_missingCorrelationId_returnsE01() {
    var req = HelloWorldReqDTO.builder()
        .correlationId("")
        .payload(HelloWorldReqDTOPayload.builder().name("John").build())
        .build();

    HelloWorldResDTO result = helloWorldService.execute(req);

    assertNotNull(result);
    assertEquals("", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E01_VALIDATION_ERROR, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("E01", result.getResponseCode());
    assertEquals("Validation Error", result.getResponseDesc());
  }

  @Test
  void testGreeting_missingName_returnsE01() {
    var req = HelloWorldReqDTO.builder()
        .correlationId("corr-123")
        .payload(HelloWorldReqDTOPayload.builder().name("").build())
        .build();

    HelloWorldResDTO result = helloWorldService.execute(req);

    assertNotNull(result);
    assertEquals("corr-123", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E01_VALIDATION_ERROR, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("E01", result.getResponseCode());
    assertEquals("Validation Error", result.getResponseDesc());
  }

  @Test
  void testGreeting_specialCharacters() {
    var req = HelloWorldReqDTO.builder()
        .correlationId("corr-456")
        .payload(HelloWorldReqDTOPayload.builder().name("José!@#$%^&*()").build())
        .build();

    HelloWorldResDTO result = helloWorldService.execute(req);

    assertNotNull(result);
    assertEquals("corr-456", result.getCorrelationId());
    assertEquals("Hi, José!@#$%^&*()", result.getPayload().getGreeting());
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertEquals("00", result.getResponseCode());
    assertEquals("Success", result.getResponseDesc());
  }

  @Test
  void testGreeting_whitespaceName_returnsE01() {
    var req = HelloWorldReqDTO.builder()
        .correlationId("corr-789")
        .payload(HelloWorldReqDTOPayload.builder().name("   ").build())
        .build();

    HelloWorldResDTO result = helloWorldService.execute(req);

    assertNotNull(result);
    assertEquals("corr-789", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E01_VALIDATION_ERROR, result.getResponse());
    assertEquals("E01", result.getResponseCode());
    assertEquals("Validation Error", result.getResponseDesc());
  }

  @Test
  void testGreeting_longName() {
    var longName = "A".repeat(1000);
    var req = HelloWorldReqDTO.builder()
        .correlationId("corr-long")
        .payload(HelloWorldReqDTOPayload.builder().name(longName).build())
        .build();

    HelloWorldResDTO result = helloWorldService.execute(req);

    assertNotNull(result);
    assertEquals("corr-long", result.getCorrelationId());
    assertEquals("Hi, " + longName, result.getPayload().getGreeting());
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertEquals("00", result.getResponseCode());
    assertEquals("Success", result.getResponseDesc());
  }

  @Test
  void testGreeting_nullName_returnsE01() {
    var req = HelloWorldReqDTO.builder()
        .correlationId("corr-null-name")
        .payload(HelloWorldReqDTOPayload.builder().name(null).build())
        .build();

    HelloWorldResDTO result = helloWorldService.execute(req);

    assertNotNull(result);
    assertEquals("corr-null-name", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E01_VALIDATION_ERROR, result.getResponse());
    assertEquals("E01", result.getResponseCode());
    assertEquals("Validation Error", result.getResponseDesc());
  }

  @Test
  void testGreeting_nullPayload_returnsE99() {
    var objenesis = new ObjenesisStd();
    var req = objenesis.newInstance(HelloWorldReqDTO.class);
    ReflectionTestUtils.setField(req, "correlationId", "corr-null");
    ReflectionTestUtils.setField(req, "payload", null);

    HelloWorldResDTO result = helloWorldService.execute(req);

    assertNotNull(result);
    assertEquals("corr-null", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E01_VALIDATION_ERROR, result.getResponse());
    assertEquals("E01", result.getResponseCode());
    assertEquals("Validation Error", result.getResponseDesc());
  }
}
