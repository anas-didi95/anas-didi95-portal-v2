package com.anasdidi.uam.controller.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anasdidi.common.CommonConstants;
import com.anasdidi.common.dto.PaginationDTO;
import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.UamConstants;
import com.anasdidi.uam.dto.SearchUserReqDTO;
import com.anasdidi.uam.dto.SearchUserResDTO;
import com.anasdidi.uam.dto.SearchUserResDTO.SearchUserResDTOPayload;
import com.anasdidi.uam.dto.model.UserDTO;
import com.anasdidi.uam.service.impl.RegisterUserService;
import com.anasdidi.uam.service.impl.SearchUserService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SearchUserControllerV1Tests {

  @Mock
  private RegisterUserService registerUserService;

  @Mock
  private SearchUserService searchUserService;

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
  void testSearchUser_success() throws Exception {
    var userId = UUID.randomUUID();
    var mockResponse = SearchUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.S00_SUCCESS)
        .traceId("trace-123")
        .timestamp(OffsetDateTime.now())
        .timeTaken(100L)
        .responseCode("00")
        .responseDesc("Success")
        .payload(SearchUserResDTOPayload.builder()
            .resultList(List.of(UserDTO.builder()
                .id(userId)
                .username("john")
                .name("John")
                .isDeleted(false)
                .build()))
            .pagination(PaginationDTO.builder()
                .pageNo(1)
                .totalRecordsPerPage(10)
                .totalPages(1)
                .totalRecords(1L)
                .build())
            .build())
        .build();

    when(searchUserService.execute(any(SearchUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(get(BASE_URL)
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .param("name", "John")
            .param("pageNo", "1")
            .param("totalRecordsPerPage", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
        .andExpect(jsonPath("$.traceId").value("trace-123"))
        .andExpect(jsonPath("$.responseCode").value("00"))
        .andExpect(jsonPath("$.responseDesc").value("Success"))
        .andExpect(jsonPath("$.payload.resultList[0].username").value("john"))
        .andExpect(jsonPath("$.payload.resultList[0].name").value("John"))
        .andExpect(jsonPath("$.payload.pagination.pageNo").value(1))
        .andExpect(jsonPath("$.payload.pagination.totalRecordsPerPage").value(10))
        .andExpect(jsonPath("$.payload.pagination.totalPages").value(1))
        .andExpect(jsonPath("$.payload.pagination.totalRecords").value(1));
  }

  @Test
  void testSearchUser_successWithoutName() throws Exception {
    var mockResponse = SearchUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.S00_SUCCESS)
        .traceId("trace-123")
        .timestamp(OffsetDateTime.now())
        .timeTaken(50L)
        .responseCode("00")
        .responseDesc("Success")
        .payload(SearchUserResDTOPayload.builder()
            .resultList(List.of())
            .pagination(PaginationDTO.builder()
                .pageNo(1)
                .totalRecordsPerPage(10)
                .totalPages(0)
                .totalRecords(0L)
                .build())
            .build())
        .build();

    when(searchUserService.execute(any(SearchUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(get(BASE_URL)
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .param("pageNo", "1")
            .param("totalRecordsPerPage", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
  }

  @Test
  void testSearchUser_successDefaultPagination() throws Exception {
    var mockResponse = SearchUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.S00_SUCCESS)
        .traceId("trace-123")
        .timestamp(OffsetDateTime.now())
        .timeTaken(50L)
        .responseCode("00")
        .responseDesc("Success")
        .payload(SearchUserResDTOPayload.builder()
            .resultList(List.of())
            .pagination(PaginationDTO.builder()
                .pageNo(1)
                .totalRecordsPerPage(10)
                .totalPages(0)
                .totalRecords(0L)
                .build())
            .build())
        .build();

    when(searchUserService.execute(any(SearchUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(get(BASE_URL).header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
  }

  @Test
  void testSearchUser_missingCorrelationId_returnsBadRequest() throws Exception {
    mockMvc
        .perform(get(BASE_URL)
            .param("name", "John")
            .param("pageNo", "1")
            .param("totalRecordsPerPage", "10"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testSearchUser_serviceReturnsE01_returnsBadRequest() throws Exception {
    var mockResponse = SearchUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.E01_VALIDATION_ERROR)
        .traceId("trace-123")
        .timestamp(OffsetDateTime.now())
        .timeTaken(50L)
        .responseCode("E01")
        .responseDesc("Validation Error")
        .build();

    when(searchUserService.execute(any(SearchUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(get(BASE_URL)
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .param("name", "John")
            .param("pageNo", "1")
            .param("totalRecordsPerPage", "10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
        .andExpect(jsonPath("$.responseCode").value("E01"))
        .andExpect(jsonPath("$.responseDesc").value("Validation Error"))
        .andExpect(jsonPath("$.payload").doesNotExist());
  }

  @Test
  void testSearchUser_serviceReturnsE99_returnsInternalServerError() throws Exception {
    var mockResponse = SearchUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.E99_UNEXPECTED_ERROR)
        .traceId("trace-123")
        .timestamp(OffsetDateTime.now())
        .timeTaken(50L)
        .responseCode("E99")
        .responseDesc("Unexpected Error")
        .build();

    when(searchUserService.execute(any(SearchUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(get(BASE_URL)
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .param("name", "John")
            .param("pageNo", "1")
            .param("totalRecordsPerPage", "10"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
        .andExpect(jsonPath("$.responseCode").value("E99"))
        .andExpect(jsonPath("$.responseDesc").value("Unexpected Error"))
        .andExpect(jsonPath("$.payload").doesNotExist());
  }

  @Test
  void testSearchUser_jsonResponseStructure() throws Exception {
    var mockResponse = SearchUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.S00_SUCCESS)
        .traceId("trace-123")
        .timestamp(OffsetDateTime.now())
        .timeTaken(100L)
        .responseCode("00")
        .responseDesc("Success")
        .payload(SearchUserResDTOPayload.builder()
            .resultList(List.of())
            .pagination(PaginationDTO.builder()
                .pageNo(1)
                .totalRecordsPerPage(10)
                .totalPages(0)
                .totalRecords(0L)
                .build())
            .build())
        .build();

    when(searchUserService.execute(any(SearchUserReqDTO.class))).thenReturn(mockResponse);

    mockMvc
        .perform(get(BASE_URL)
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .param("name", "John")
            .param("pageNo", "1")
            .param("totalRecordsPerPage", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correlationId").isString())
        .andExpect(jsonPath("$.traceId").isString())
        .andExpect(jsonPath("$.timestamp").isString())
        .andExpect(jsonPath("$.timeTaken").isNumber())
        .andExpect(jsonPath("$.payload").isMap())
        .andExpect(jsonPath("$.payload.resultList").isArray())
        .andExpect(jsonPath("$.payload.pagination").isMap())
        .andExpect(jsonPath("$.payload.pagination.pageNo").isNumber())
        .andExpect(jsonPath("$.payload.pagination.totalRecordsPerPage").isNumber())
        .andExpect(jsonPath("$.payload.pagination.totalPages").isNumber())
        .andExpect(jsonPath("$.payload.pagination.totalRecords").isNumber());
  }
}
