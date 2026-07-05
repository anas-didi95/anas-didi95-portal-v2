package com.anasdidi.uam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.GetUserReqDTO;
import com.anasdidi.uam.dto.GetUserReqDTO.GetUserReqDTOPayload;
import com.anasdidi.uam.dto.GetUserResDTO;
import com.anasdidi.uam.entity.UserEntity;
import com.anasdidi.uam.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class GetUserServiceTests {

  @Autowired
  private GetUserService getUserService;

  @Autowired
  private UserRepository userRepository;

  @Test
  @Transactional
  void testGetUser_success() {
    var entity = UserEntity.builder()
        .username("getuser")
        .password("pass123")
        .name("Get User")
        .build();
    var savedEntity = userRepository.save(entity);

    var req = GetUserReqDTO.builder()
        .correlationId("corr-get-123")
        .payload(GetUserReqDTOPayload.builder().userId(savedEntity.getId()).build())
        .build();

    GetUserResDTO result = getUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-get-123", result.getCorrelationId());
    assertNotNull(result.getPayload());
    assertNotNull(result.getPayload().getResult());
    assertEquals(savedEntity.getId(), result.getPayload().getResult().getId());
    assertEquals("getuser", result.getPayload().getResult().getUsername());
    assertNull(result.getPayload().getResult().getPassword());
    assertEquals("Get User", result.getPayload().getResult().getName());
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("00", result.getResponseCode());
    assertEquals("Success", result.getResponseDesc());
  }

  @Test
  @Transactional
  void testGetUser_notFound() {
    var req = GetUserReqDTO.builder()
        .correlationId("corr-not-found")
        .payload(GetUserReqDTOPayload.builder().userId(UUID.randomUUID()).build())
        .build();

    GetUserResDTO result = getUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-not-found", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E03_RESOURCE_NOT_FOUND, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("E03", result.getResponseCode());
    assertEquals("User Not Found", result.getResponseDesc());
  }

  @Test
  @Transactional
  void testGetUser_nullPayload() {
    var objenesis = new ObjenesisStd();
    var req = objenesis.newInstance(GetUserReqDTO.class);
    ReflectionTestUtils.setField(req, "correlationId", "corr-null");
    ReflectionTestUtils.setField(req, "payload", null);

    GetUserResDTO result = getUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-null", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E01_VALIDATION_ERROR, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("E01", result.getResponseCode());
    assertEquals("Validation Error", result.getResponseDesc());
  }

  @Test
  @Transactional
  void testGetUser_missingCorrelationId() {
    var req = GetUserReqDTO.builder()
        .correlationId("")
        .payload(GetUserReqDTOPayload.builder().userId(UUID.randomUUID()).build())
        .build();

    GetUserResDTO result = getUserService.execute(req);

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
  @Transactional
  void testGetUser_nullUserId() {
    var req = GetUserReqDTO.builder()
        .correlationId("corr-null-userid")
        .payload(GetUserReqDTOPayload.builder().userId(null).build())
        .build();

    GetUserResDTO result = getUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-null-userid", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E01_VALIDATION_ERROR, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("E01", result.getResponseCode());
    assertEquals("Validation Error", result.getResponseDesc());
  }
}
