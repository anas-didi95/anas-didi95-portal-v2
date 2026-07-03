package com.anasdidi.uam.controller;

import com.anasdidi.common.CommonConstants;
import com.anasdidi.uam.dto.GetUserResDTO;
import com.anasdidi.uam.dto.RegisterUserReqDTO.RegisterUserReqDTOPayload;
import com.anasdidi.uam.dto.RegisterUserResDTO;
import com.anasdidi.uam.dto.SearchUserResDTO;
import com.anasdidi.uam.dto.UpdateUserReqDTO.UpdateUserReqDTOPayload;
import com.anasdidi.uam.dto.UpdateUserResDTO;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

public interface UserController {

  String BASE_URL = "/user";

  @PostMapping("/register")
  ResponseEntity<RegisterUserResDTO> registerUser(
      @RequestHeader(name = CommonConstants.HEADER_CORR_ID) String correlationId,
      @RequestBody RegisterUserReqDTOPayload body);

  @GetMapping("")
  ResponseEntity<SearchUserResDTO> searchUser(
      @RequestHeader(name = CommonConstants.HEADER_CORR_ID) String correlationId,
      @RequestParam(required = false) String name,
      @RequestParam(required = false, defaultValue = "1") Integer pageNo,
      @RequestParam(required = false, defaultValue = "10") Integer totalRecordsPerPage);

  @GetMapping("/{userId}")
  ResponseEntity<GetUserResDTO> getUser(
      @RequestHeader(name = CommonConstants.HEADER_CORR_ID) String correlationId,
      @PathVariable UUID userId);

  @PatchMapping("/{userId}")
  ResponseEntity<UpdateUserResDTO> updateUser(
      @RequestHeader(name = CommonConstants.HEADER_CORR_ID) String correlationId,
      @PathVariable UUID userId,
      @RequestParam Integer version,
      @RequestBody UpdateUserReqDTOPayload body);
}
