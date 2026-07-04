package com.anasdidi.uam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.DeleteUserReqDTO;
import com.anasdidi.uam.dto.DeleteUserReqDTO.DeleteUserReqDTOPayload;
import com.anasdidi.uam.dto.DeleteUserResDTO;
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
class DeleteUserServiceTests {

  @Autowired
  private DeleteUserService deleteUserService;

  @Autowired
  private UserRepository userRepository;

  @Test
  @Transactional
  void testDeleteUser_success() {
    var savedEntity = seedUser("deleteuser", "Delete User");

    var req = DeleteUserReqDTO.builder()
        .correlationId("corr-delete-123")
        .payload(DeleteUserReqDTOPayload.builder()
            .userId(savedEntity.getId())
            .version(savedEntity.getVersion())
            .build())
        .build();

    DeleteUserResDTO result = deleteUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-delete-123", result.getCorrelationId());

    assertEquals(ResponseEnum.S02_DELETED, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("02", result.getResponseCode());
    assertEquals("Deleted", result.getResponseDesc());

    var entity = userRepository.findById(savedEntity.getId()).orElseThrow();
    assertTrue(entity.getIsDeleted());
  }

  @Test
  @Transactional
  void testDeleteUser_notFound() {
    var req = DeleteUserReqDTO.builder()
        .correlationId("corr-not-found")
        .payload(DeleteUserReqDTOPayload.builder()
            .userId(UUID.randomUUID())
            .version(1)
            .build())
        .build();

    DeleteUserResDTO result = deleteUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-not-found", result.getCorrelationId());

    assertEquals(ResponseEnum.E03_RESOURCE_NOT_FOUND, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("E03", result.getResponseCode());
    assertEquals("User Not Found", result.getResponseDesc());
  }

  @Test
  @Transactional
  void testDeleteUser_versionMismatch() {
    var savedEntity = seedUser("versionuser", "Version User");

    var req = DeleteUserReqDTO.builder()
        .correlationId("corr-version")
        .payload(DeleteUserReqDTOPayload.builder()
            .userId(savedEntity.getId())
            .version(999)
            .build())
        .build();

    DeleteUserResDTO result = deleteUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-version", result.getCorrelationId());

    assertEquals(ResponseEnum.E03_RESOURCE_NOT_FOUND, result.getResponse());
    assertEquals("E03", result.getResponseCode());
    assertEquals("User Not Found", result.getResponseDesc());
  }

  @Test
  @Transactional
  void testDeleteUser_nullPayload() {
    var objenesis = new ObjenesisStd();
    var req = objenesis.newInstance(DeleteUserReqDTO.class);
    ReflectionTestUtils.setField(req, "correlationId", "corr-null");
    ReflectionTestUtils.setField(req, "payload", null);

    DeleteUserResDTO result = deleteUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-null", result.getCorrelationId());

    assertEquals(ResponseEnum.E01_VALIDATION_ERROR, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("E01", result.getResponseCode());
    assertEquals("Validation Error", result.getResponseDesc());
  }

  @Test
  @Transactional
  void testDeleteUser_missingCorrelationId() {
    var savedEntity = seedUser("corriduser", "Corrid User");

    var req = DeleteUserReqDTO.builder()
        .correlationId("")
        .payload(DeleteUserReqDTOPayload.builder()
            .userId(savedEntity.getId())
            .version(savedEntity.getVersion())
            .build())
        .build();

    DeleteUserResDTO result = deleteUserService.execute(req);

    assertNotNull(result);
    assertEquals("", result.getCorrelationId());

    assertEquals(ResponseEnum.E01_VALIDATION_ERROR, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("E01", result.getResponseCode());
    assertEquals("Validation Error", result.getResponseDesc());
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
