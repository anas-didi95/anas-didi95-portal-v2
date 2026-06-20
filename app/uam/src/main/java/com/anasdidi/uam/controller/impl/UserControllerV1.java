package com.anasdidi.uam.controller.impl;

import com.anasdidi.common.CommonConstants;
import com.anasdidi.uam.UamConstants;
import com.anasdidi.uam.controller.UserController;
import com.anasdidi.uam.dto.RegisterUserReqDTO;
import com.anasdidi.uam.dto.RegisterUserReqDTO.RegisterUserReqDTOPayload;
import com.anasdidi.uam.dto.RegisterUserResDTO;
import com.anasdidi.uam.service.impl.RegisterUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UamConstants.CONTEXT_PATH + CommonConstants.API_V1 + UserController.BASE_URL)
@RequiredArgsConstructor
public class UserControllerV1 implements UserController {

  private final RegisterUserService registerUserService;

  @Override
  public ResponseEntity<RegisterUserResDTO> registerUser(
      String correlationId, RegisterUserReqDTOPayload body) {
    var req =
        RegisterUserReqDTO.builder().correlationId(correlationId).payload(body).build();
    var res = registerUserService.execute(req);
    return ResponseEntity.status(res.getResponse().httpStatus).body(res);
  }
}
