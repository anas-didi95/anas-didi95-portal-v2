package com.anasdidi.uam.controller.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.HelloWorldReqDTO;
import com.anasdidi.uam.dto.HelloWorldResDTO;
import com.anasdidi.uam.dto.HelloWorldResDTO.HelloWorldResDTOPayload;
import com.anasdidi.uam.service.impl.HelloWorldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HelloWorldControllerV1Tests {

  @Mock
  private HelloWorldService helloWorldService;

  @InjectMocks
  private HelloWorldControllerV1 helloWorldControllerV1;

  private MockMvc mockMvc;

  private static final String BASE_URL = "/uam/v1/hello-world";
  private static final String CORRELATION_ID = "corr-123";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(helloWorldControllerV1).build();
  }

  @Test
  void testGreeting() throws Exception {
    var mockResponse = HelloWorldResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.S00_SUCCESS)
        .payload(HelloWorldResDTOPayload.builder().greeting("Hi, John").build())
        .build();

    when(helloWorldService.execute(any(HelloWorldReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(get(BASE_URL + "/greeting?name=John").header("App-Correlation-Id", CORRELATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
        .andExpect(jsonPath("$.payload.greeting").value("Hi, John"));
  }

  @Test
  void testGreeting_missingName_returnsBadRequest() throws Exception {
    mockMvc
        .perform(get(BASE_URL + "/greeting").header("App-Correlation-Id", CORRELATION_ID))
        .andExpect(status().isBadRequest());
  }
}
