package com.anasdidi.uam.controller.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.HelloWorldReqDTO;
import com.anasdidi.uam.dto.HelloWorldResDTO;
import com.anasdidi.uam.dto.HelloWorldResDTO.HelloWorldResDTOPayload;
import com.anasdidi.uam.service.impl.HelloWorldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = HelloWorldControllerV1.class)
@Import(HelloWorldControllerV1Tests.TestConfig.class)
class HelloWorldControllerV1Tests {

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private HelloWorldService helloWorldService;

  private static final String BASE_URL = "/uam/v1/hello-world";
  private static final String CORRELATION_ID = "corr-123";

  @BeforeEach
  void setUp() {
    Mockito.reset(helloWorldService);
  }

  @Test
  void testGreeting() {
    var mockResponse = HelloWorldResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.S00_SUCCESS)
        .payload(HelloWorldResDTOPayload.builder().greeting("Hi, John").build())
        .build();

    when(helloWorldService.execute(any(HelloWorldReqDTO.class)))
        .thenReturn(Mono.just(mockResponse));

    webTestClient
        .get()
        .uri(BASE_URL + "/greeting?name=John")
        .header("App-Correlation-Id", CORRELATION_ID)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.correlationId")
        .isEqualTo(CORRELATION_ID)
        .jsonPath("$.payload.greeting")
        .isEqualTo("Hi, John");
  }

  @Test
  void testGreeting_missingName_returnsBadRequest() {
    webTestClient
        .get()
        .uri(BASE_URL + "/greeting")
        .header("App-Correlation-Id", CORRELATION_ID)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    HelloWorldService helloWorldService() {
      return Mockito.mock(HelloWorldService.class);
    }
  }
}
