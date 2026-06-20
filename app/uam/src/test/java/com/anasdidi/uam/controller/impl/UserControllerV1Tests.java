package com.anasdidi.uam.controller.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anasdidi.common.CommonConstants;
import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.UamConstants;
import com.anasdidi.uam.dto.RegisterUserReqDTO;
import com.anasdidi.uam.dto.RegisterUserResDTO;
import com.anasdidi.uam.dto.RegisterUserResDTO.RegisterUserResDTOPayload;
import com.anasdidi.uam.service.impl.RegisterUserService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerV1Tests {

  @Mock
  private RegisterUserService registerUserService;

  @InjectMocks
  private UserControllerV1 userControllerV1;

  private MockMvc mockMvc;

  private static final String BASE_URL =
      UamConstants.CONTEXT_PATH + CommonConstants.API_V1 + "/user";
  private static final String CORRELATION_ID = "corr-123";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(userControllerV1).build();
  }

  @Test
  void testRegisterUser_success() throws Exception {
    var userId = UUID.randomUUID();
    var mockResponse = RegisterUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.S00_SUCCESS)
        .payload(RegisterUserResDTOPayload.builder().userId(userId).build())
        .build();

    when(registerUserService.execute(any(RegisterUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(post(BASE_URL + "/register")
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"john\",\"password\":\"pass123\",\"name\":\"John\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
        .andExpect(jsonPath("$.payload.userId").value(userId.toString()));
  }

  @Test
  void testRegisterUser_missingCorrelationId_returnsBadRequest() throws Exception {
    mockMvc
        .perform(post(BASE_URL + "/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"john\",\"password\":\"pass123\",\"name\":\"John\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testRegisterUser_missingBodyField_returnsBadRequest() throws Exception {
    var mockResponse = RegisterUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.E01_VALIDATION_ERROR)
        .build();

    when(registerUserService.execute(any(RegisterUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(post(BASE_URL + "/register")
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"password\":\"pass123\",\"name\":\"John\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
        .andExpect(jsonPath("$.payload").doesNotExist());
  }

  @Test
  void testRegisterUser_usernameTooLong_returnsBadRequest() throws Exception {
    var mockResponse = RegisterUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.E01_VALIDATION_ERROR)
        .build();

    when(registerUserService.execute(any(RegisterUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(post(BASE_URL + "/register")
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\""
                + "a".repeat(21)
                + "\",\"password\":\"pass123\",\"name\":\"John\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
        .andExpect(jsonPath("$.payload").doesNotExist());
  }

  @Test
  void testRegisterUser_serviceReturnsE01_returnsBadRequest() throws Exception {
    var mockResponse = RegisterUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.E01_VALIDATION_ERROR)
        .build();

    when(registerUserService.execute(any(RegisterUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(post(BASE_URL + "/register")
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"john\",\"password\":\"pass123\",\"name\":\"John\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
        .andExpect(jsonPath("$.payload").doesNotExist());
  }

  @Test
  void testRegisterUser_serviceReturnsE02_returnsBadRequest() throws Exception {
    var mockResponse = RegisterUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.E02_RESOURCE_ALREADY_EXISTS)
        .build();

    when(registerUserService.execute(any(RegisterUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(post(BASE_URL + "/register")
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"john\",\"password\":\"pass123\",\"name\":\"John\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
        .andExpect(jsonPath("$.payload").doesNotExist());
  }

  @Test
  void testRegisterUser_serviceReturnsE99_returnsInternalServerError() throws Exception {
    var mockResponse = RegisterUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.E99_UNEXPECTED_ERROR)
        .build();

    when(registerUserService.execute(any(RegisterUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(post(BASE_URL + "/register")
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"john\",\"password\":\"pass123\",\"name\":\"John\"}"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
        .andExpect(jsonPath("$.payload").doesNotExist());
  }

  @Test
  void testRegisterUser_jsonResponseStructure() throws Exception {
    var userId = UUID.randomUUID();
    var mockResponse = RegisterUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.S00_SUCCESS)
        .payload(RegisterUserResDTOPayload.builder().userId(userId).build())
        .build();

    when(registerUserService.execute(any(RegisterUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(post(BASE_URL + "/register")
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"john\",\"password\":\"pass123\",\"name\":\"John\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correlationId").isString())
        .andExpect(jsonPath("$.payload").isMap())
        .andExpect(jsonPath("$.payload.userId").isString());
  }

  @Test
  void testRegisterUser_emptyBody_returnsBadRequest() throws Exception {
    mockMvc
        .perform(post(BASE_URL + "/register")
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }
}
