package com.anasdidi.uam.service.impl;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.common.error.E02ResourceAlreadyExists;
import com.anasdidi.common.error.E02ResourceAlreadyExists.Resource;
import com.anasdidi.uam.dto.RegisterUserReqDTO;
import com.anasdidi.uam.dto.RegisterUserResDTO;
import com.anasdidi.uam.dto.RegisterUserResDTO.RegisterUserResDTOPayload;
import com.anasdidi.uam.entity.UserEntity;
import com.anasdidi.uam.repository.UserRepository;
import com.anasdidi.uam.service.UamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RegisterUserService implements UamService<RegisterUserReqDTO, RegisterUserResDTO> {

  private final UserRepository userRepository;

  @Override
  public RegisterUserResDTO execute(@Valid RegisterUserReqDTO req) {
    var res = RegisterUserResDTO.builder();

    if (userRepository.findByUsername(req.getPayload().getUsername()).isPresent()) {
      log.error("Username already exists! {}", req.getPayload().getUsername());
      throw new E02ResourceAlreadyExists(Resource.USER);
    }

    var entity = UserEntity.builder()
        .username(req.getPayload().getUsername())
        .password(req.getPayload().getPassword())
        .name(req.getPayload().getName().toUpperCase())
        .isDeleted(false)
        .build();
    entity = userRepository.save(entity);

    var payload = RegisterUserResDTOPayload.builder().userId(entity.getId()).build();

    return res.response(ResponseEnum.S00_SUCCESS).payload(payload).build();
  }
}
