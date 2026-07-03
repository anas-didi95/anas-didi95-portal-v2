package com.anasdidi.uam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.UpdateUserReqDTO;
import com.anasdidi.uam.dto.UpdateUserReqDTO.UpdateUserReqDTOPayload;
import com.anasdidi.uam.dto.UpdateUserResDTO;
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
class UpdateUserServiceTests {

  @Autowired
  private UpdateUserService updateUserService;

  @Autowired
  private UserRepository userRepository;

  @Test
  @Transactional
  void testUpdateUser_success() {
    var savedEntity = seedUser("updateuser", "Update User");

    var req = UpdateUserReqDTO.builder()
        .correlationId("corr-update-123")
        .userId(savedEntity.getId())
        .version(savedEntity.getVersion())
        .payload(UpdateUserReqDTOPayload.builder().name("Updated Name").build())
        .build();

    UpdateUserResDTO result = updateUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-update-123", result.getCorrelationId());
    assertNotNull(result.getPayload());
    assertEquals(savedEntity.getId(), result.getPayload().getUserId());
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("00", result.getResponseCode());
    assertEquals("Success", result.getResponseDesc());

    var entity = userRepository.findById(savedEntity.getId()).orElseThrow();
    assertEquals("UPDATED NAME", entity.getName());
  }

  @Test
  @Transactional
  void testUpdateUser_notFound() {
    var req = UpdateUserReqDTO.builder()
        .correlationId("corr-not-found")
        .userId(UUID.randomUUID())
        .version(1)
        .payload(UpdateUserReqDTOPayload.builder().name("Updated Name").build())
        .build();

    UpdateUserResDTO result = updateUserService.execute(req);

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
  void testUpdateUser_versionMismatch() {
    var savedEntity = seedUser("versionuser", "Version User");

    var req = UpdateUserReqDTO.builder()
        .correlationId("corr-version")
        .userId(savedEntity.getId())
        .version(999)
        .payload(UpdateUserReqDTOPayload.builder().name("Updated Name").build())
        .build();

    UpdateUserResDTO result = updateUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-version", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E03_RESOURCE_NOT_FOUND, result.getResponse());
    assertEquals("E03", result.getResponseCode());
    assertEquals("User Not Found", result.getResponseDesc());
  }

  @Test
  @Transactional
  void testUpdateUser_nullPayload() {
    var objenesis = new ObjenesisStd();
    var req = objenesis.newInstance(UpdateUserReqDTO.class);
    ReflectionTestUtils.setField(req, "correlationId", "corr-null");
    ReflectionTestUtils.setField(req, "userId", UUID.randomUUID());
    ReflectionTestUtils.setField(req, "version", 1);
    ReflectionTestUtils.setField(req, "payload", null);

    UpdateUserResDTO result = updateUserService.execute(req);

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
  void testUpdateUser_missingCorrelationId() {
    var savedEntity = seedUser("corriduser", "Corrid User");

    var req = UpdateUserReqDTO.builder()
        .correlationId("")
        .userId(savedEntity.getId())
        .version(savedEntity.getVersion())
        .payload(UpdateUserReqDTOPayload.builder().name("Updated Name").build())
        .build();

    UpdateUserResDTO result = updateUserService.execute(req);

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
  void testUpdateUser_nullName() {
    var savedEntity = seedUser("nullnameuser", "Nullname User");

    var req = UpdateUserReqDTO.builder()
        .correlationId("corr-null-name")
        .userId(savedEntity.getId())
        .version(savedEntity.getVersion())
        .payload(UpdateUserReqDTOPayload.builder().name(null).build())
        .build();

    UpdateUserResDTO result = updateUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-null-name", result.getCorrelationId());
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
  void testUpdateUser_blankName() {
    var savedEntity = seedUser("blanknameuser", "Blankname User");

    var req = UpdateUserReqDTO.builder()
        .correlationId("corr-blank-name")
        .userId(savedEntity.getId())
        .version(savedEntity.getVersion())
        .payload(UpdateUserReqDTOPayload.builder().name("").build())
        .build();

    UpdateUserResDTO result = updateUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-blank-name", result.getCorrelationId());
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
  void testUpdateUser_nameUppercased() {
    var savedEntity = seedUser("upperuser", "John Doe");

    var req = UpdateUserReqDTO.builder()
        .correlationId("corr-upper")
        .userId(savedEntity.getId())
        .version(savedEntity.getVersion())
        .payload(UpdateUserReqDTOPayload.builder().name("Jane smith").build())
        .build();

    updateUserService.execute(req);

    var entity = userRepository.findById(savedEntity.getId()).orElseThrow();
    assertEquals("JANE SMITH", entity.getName());
  }

  private UserEntity seedUser(String username, String name) {
    return userRepository.save(UserEntity.builder()
        .username(username)
        .password("pass")
        .name(name)
        .isDeleted(false)
        .build());
  }
}
