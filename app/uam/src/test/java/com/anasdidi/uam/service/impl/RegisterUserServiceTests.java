package com.anasdidi.uam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.RegisterUserReqDTO;
import com.anasdidi.uam.dto.RegisterUserReqDTO.RegisterUserReqDTOPayload;
import com.anasdidi.uam.dto.RegisterUserResDTO;
import com.anasdidi.uam.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class RegisterUserServiceTests {

  @Autowired
  private RegisterUserService registerUserService;

  @Autowired
  private UserRepository userRepository;

  @Test
  @Transactional
  void testRegisterUser_success() {
    var req = RegisterUserReqDTO.builder()
        .correlationId("corr-123")
        .payload(RegisterUserReqDTOPayload.builder()
            .username("john")
            .password("pass123")
            .name("John")
            .build())
        .build();

    RegisterUserResDTO result = registerUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-123", result.getCorrelationId());
    assertNotNull(result.getPayload());
    assertNotNull(result.getPayload().getUserId());
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("00", result.getResponseCode());
    assertEquals("Success", result.getResponseDesc());
  }

  @Test
  @Transactional
  void testRegisterUser_duplicateUsername() {
    var req = RegisterUserReqDTO.builder()
        .correlationId("corr-dup")
        .payload(RegisterUserReqDTOPayload.builder()
            .username("dupeuser")
            .password("pass123")
            .name("Dupe User")
            .build())
        .build();

    registerUserService.execute(req);

    RegisterUserResDTO result = registerUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-dup", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E02_RESOURCE_ALREADY_EXISTS, result.getResponse());
    assertNotNull(result.getTraceId());
    assertNotNull(result.getTimestamp());
    assertNotNull(result.getTimeTaken());
    assertEquals("E02", result.getResponseCode());
    assertEquals("User Already Exists", result.getResponseDesc());
  }

  @Test
  @Transactional
  void testRegisterUser_missingUsername() {
    var req = RegisterUserReqDTO.builder()
        .correlationId("corr-missing-user")
        .payload(RegisterUserReqDTOPayload.builder()
            .username("")
            .password("pass123")
            .name("John")
            .build())
        .build();

    RegisterUserResDTO result = registerUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-missing-user", result.getCorrelationId());
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
  void testRegisterUser_missingPassword() {
    var req = RegisterUserReqDTO.builder()
        .correlationId("corr-missing-pass")
        .payload(RegisterUserReqDTOPayload.builder()
            .username("john")
            .password("")
            .name("John")
            .build())
        .build();

    RegisterUserResDTO result = registerUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-missing-pass", result.getCorrelationId());
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
  void testRegisterUser_missingName() {
    var req = RegisterUserReqDTO.builder()
        .correlationId("corr-missing-name")
        .payload(RegisterUserReqDTOPayload.builder()
            .username("john")
            .password("pass123")
            .name("")
            .build())
        .build();

    RegisterUserResDTO result = registerUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-missing-name", result.getCorrelationId());
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
  void testRegisterUser_usernameTooLong() {
    var req = RegisterUserReqDTO.builder()
        .correlationId("corr-long")
        .payload(RegisterUserReqDTOPayload.builder()
            .username("a".repeat(21))
            .password("pass123")
            .name("John")
            .build())
        .build();

    RegisterUserResDTO result = registerUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-long", result.getCorrelationId());
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
  void testRegisterUser_missingCorrelationId() {
    var req = RegisterUserReqDTO.builder()
        .correlationId("")
        .payload(RegisterUserReqDTOPayload.builder()
            .username("john")
            .password("pass123")
            .name("John")
            .build())
        .build();

    RegisterUserResDTO result = registerUserService.execute(req);

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
  void testRegisterUser_nullPayload() {
    var objenesis = new ObjenesisStd();
    var req = objenesis.newInstance(RegisterUserReqDTO.class);
    ReflectionTestUtils.setField(req, "correlationId", "corr-null");
    ReflectionTestUtils.setField(req, "payload", null);

    RegisterUserResDTO result = registerUserService.execute(req);

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
  void testRegisterUser_nameUppercased() {
    var req = RegisterUserReqDTO.builder()
        .correlationId("corr-upper")
        .payload(RegisterUserReqDTOPayload.builder()
            .username("upperuser")
            .password("pass123")
            .name("john doe")
            .build())
        .build();

    registerUserService.execute(req);

    var entity = userRepository.findByUsername("upperuser").orElseThrow();
    assertEquals("JOHN DOE", entity.getName());
  }

  @Test
  @Transactional
  void testRegisterUser_whitespaceUsername_returnsE01() {
    var req = RegisterUserReqDTO.builder()
        .correlationId("corr-ws")
        .payload(RegisterUserReqDTOPayload.builder()
            .username("   ")
            .password("pass123")
            .name("John")
            .build())
        .build();

    RegisterUserResDTO result = registerUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-ws", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E01_VALIDATION_ERROR, result.getResponse());
    assertEquals("E01", result.getResponseCode());
    assertEquals("Validation Error", result.getResponseDesc());
  }

  @Test
  @Transactional
  void testRegisterUser_nullUsername_returnsE01() {
    var req = RegisterUserReqDTO.builder()
        .correlationId("corr-null-user")
        .payload(RegisterUserReqDTOPayload.builder()
            .username(null)
            .password("pass123")
            .name("John")
            .build())
        .build();

    RegisterUserResDTO result = registerUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-null-user", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E01_VALIDATION_ERROR, result.getResponse());
    assertEquals("E01", result.getResponseCode());
    assertEquals("Validation Error", result.getResponseDesc());
  }

  @Test
  @Transactional
  void testRegisterUser_specialCharacters() {
    var req = RegisterUserReqDTO.builder()
        .correlationId("corr-special")
        .payload(RegisterUserReqDTOPayload.builder()
            .username("José!@#")
            .password("pass123")
            .name("José!@#$%^&*()")
            .build())
        .build();

    RegisterUserResDTO result = registerUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-special", result.getCorrelationId());
    assertNotNull(result.getPayload());
    assertNotNull(result.getPayload().getUserId());
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertEquals("00", result.getResponseCode());
    assertEquals("Success", result.getResponseDesc());

    var entity = userRepository.findByUsername("José!@#").orElseThrow();
    assertEquals("JOSÉ!@#$%^&*()", entity.getName());
  }
}
