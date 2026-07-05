package com.anasdidi.uam.service.impl;

import com.anasdidi.common.enums.ResourceEnum;
import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.common.error.E03ResourceNotFound;
import com.anasdidi.uam.dto.UpdateUserReqDTO;
import com.anasdidi.uam.dto.UpdateUserResDTO;
import com.anasdidi.uam.dto.UpdateUserResDTO.UpdateUserResDTOPayload;
import com.anasdidi.uam.repository.UserRepository;
import com.anasdidi.uam.service.UamService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UpdateUserService implements UamService<UpdateUserReqDTO, UpdateUserResDTO> {

  private final UserRepository userRepository;

  @Override
  public UpdateUserResDTO execute(@Valid UpdateUserReqDTO req) {
    var res = UpdateUserResDTO.builder();

    var entity = userRepository
        .findByIdAndVersion(req.getUserId(), req.getVersion())
        .orElseThrow(() -> new E03ResourceNotFound(
            ResourceEnum.USER, Map.of("userId", req.getUserId(), "version", req.getVersion())));

    entity.setName(req.getPayload().getName().toUpperCase());

    entity = userRepository.save(entity);

    var payload = UpdateUserResDTOPayload.builder().userId(entity.getId()).build();

    return res.response(ResponseEnum.S00_SUCCESS).payload(payload).build();
  }
}
