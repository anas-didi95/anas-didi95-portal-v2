package com.anasdidi.uam.controller;

import com.anasdidi.common.CommonConstants;
import com.anasdidi.uam.dto.RegisterUserReqDTO.RegisterUserReqDTOPayload;
import com.anasdidi.uam.dto.RegisterUserResDTO;
import com.anasdidi.uam.dto.SearchUserResDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
      @RequestParam(required = false) String name);
}
