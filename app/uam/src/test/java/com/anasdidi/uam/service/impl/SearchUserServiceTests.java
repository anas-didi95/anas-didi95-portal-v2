package com.anasdidi.uam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anasdidi.common.dto.PaginationDTO;
import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.SearchUserReqDTO;
import com.anasdidi.uam.dto.SearchUserReqDTO.SearchUserReqDTOPayload;
import com.anasdidi.uam.dto.SearchUserResDTO;
import com.anasdidi.uam.entity.UserEntity;
import com.anasdidi.uam.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class SearchUserServiceTests {

  @Autowired
  private SearchUserService searchUserService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @Test
  @Transactional
  void testSearchUser_byName_success() {
    seedUser("alice", "Alice");
    seedUser("bob", "Bob");
    seedUser("alex", "Alex");

    var req = buildRequest("A", 1, 10);
    SearchUserResDTO result = searchUserService.execute(req);

    assertNotNull(result);
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertEquals("00", result.getResponseCode());
    assertEquals("Success", result.getResponseDesc());
    assertNotNull(result.getPayload());
    assertEquals(2, result.getPayload().getResultList().size());
    assertEquals(1, result.getPayload().getPagination().getPageNo());
    assertEquals(10, result.getPayload().getPagination().getTotalRecordsPerPage());
    assertEquals(1, result.getPayload().getPagination().getTotalPages());
    assertEquals(2L, result.getPayload().getPagination().getTotalRecords());
  }

  @Test
  @Transactional
  void testSearchUser_withoutName_success() {
    seedUser("alice", "Alice");
    seedUser("bob", "Bob");
    seedUser("alex", "Alex");

    var req = buildRequest(null, 1, 10);
    SearchUserResDTO result = searchUserService.execute(req);

    assertNotNull(result);
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertEquals(3, result.getPayload().getResultList().size());
    assertEquals(3L, result.getPayload().getPagination().getTotalRecords());
  }

  @Test
  @Transactional
  void testSearchUser_pagination_success() {
    seedUser("user1", "User1");
    seedUser("user2", "User2");
    seedUser("user3", "User3");
    seedUser("user4", "User4");
    seedUser("user5", "User5");

    var req = buildRequest(null, 1, 2);
    SearchUserResDTO result = searchUserService.execute(req);

    assertNotNull(result);
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertEquals(2, result.getPayload().getResultList().size());
    assertEquals(1, result.getPayload().getPagination().getPageNo());
    assertEquals(2, result.getPayload().getPagination().getTotalRecordsPerPage());
    assertEquals(3, result.getPayload().getPagination().getTotalPages());
    assertEquals(5L, result.getPayload().getPagination().getTotalRecords());
  }

  @Test
  @Transactional
  void testSearchUser_emptyResult() {
    seedUser("alice", "Alice");

    var req = buildRequest("Z", 1, 10);
    SearchUserResDTO result = searchUserService.execute(req);

    assertNotNull(result);
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertNotNull(result.getPayload());
    assertTrue(result.getPayload().getResultList().isEmpty());
    assertEquals(0, result.getPayload().getPagination().getTotalPages());
    assertEquals(0L, result.getPayload().getPagination().getTotalRecords());
  }

  @Test
  @Transactional
  void testSearchUser_blankName_returnsAll() {
    seedUser("alice", "Alice");
    seedUser("bob", "Bob");
    seedUser("alex", "Alex");

    var req = buildRequest("", 1, 10);
    SearchUserResDTO result = searchUserService.execute(req);

    assertNotNull(result);
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertEquals(3, result.getPayload().getResultList().size());
    assertEquals(3L, result.getPayload().getPagination().getTotalRecords());
  }

  @Test
  @Transactional
  void testSearchUser_nullPayload_returnsE01() {
    var objenesis = new ObjenesisStd();
    var req = objenesis.newInstance(SearchUserReqDTO.class);
    ReflectionTestUtils.setField(req, "correlationId", "corr-null");
    ReflectionTestUtils.setField(req, "payload", null);

    SearchUserResDTO result = searchUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-null", result.getCorrelationId());
    assertNull(result.getPayload());
    assertEquals(ResponseEnum.E01_VALIDATION_ERROR, result.getResponse());
    assertEquals("E01", result.getResponseCode());
    assertEquals("Validation Error", result.getResponseDesc());
  }

  private void seedUser(String username, String name) {
    userRepository.save(UserEntity.builder()
        .username(username)
        .password("pass")
        .name(name)
        .isDeleted(false)
        .build());
  }

  private SearchUserReqDTO buildRequest(String name, int pageNo, int totalRecordsPerPage) {
    return SearchUserReqDTO.builder()
        .correlationId("corr-123")
        .payload(SearchUserReqDTOPayload.builder()
            .name(name)
            .paginationDTO(PaginationDTO.builder()
                .pageNo(pageNo)
                .totalRecordsPerPage(totalRecordsPerPage)
                .build())
            .build())
        .build();
  }
}
