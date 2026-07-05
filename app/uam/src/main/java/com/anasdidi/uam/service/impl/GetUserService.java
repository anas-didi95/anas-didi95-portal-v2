package com.anasdidi.uam.service.impl;

import com.anasdidi.common.enums.ResourceEnum;
import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.common.error.E03ResourceNotFound;
import com.anasdidi.uam.dto.GetUserReqDTO;
import com.anasdidi.uam.dto.GetUserResDTO;
import com.anasdidi.uam.dto.GetUserResDTO.GetUserResDTOPayload;
import com.anasdidi.uam.dto.model.UserDTO;
import com.anasdidi.uam.repository.UserRepository;
import com.anasdidi.uam.service.UamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class GetUserService implements UamService<GetUserReqDTO, GetUserResDTO> {

  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  @Override
  public GetUserResDTO execute(@Valid GetUserReqDTO req) {
    var res = GetUserResDTO.builder();

    var result = userRepository.findById(req.getPayload().getUserId()).orElseThrow(() -> {
      log.error("User ID not found! {}", req.getPayload().getUserId());
      return new E03ResourceNotFound(ResourceEnum.USER);
    });

    var payload = GetUserResDTOPayload.builder()
        .result(objectMapper.convertValue(result, UserDTO.class))
        .build();

    return res.response(ResponseEnum.S00_SUCCESS).payload(payload).build();
  }
}
