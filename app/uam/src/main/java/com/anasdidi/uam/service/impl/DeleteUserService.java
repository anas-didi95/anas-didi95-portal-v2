package com.anasdidi.uam.service.impl;

import com.anasdidi.common.enums.ResourceEnum;
import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.common.error.E03ResourceNotFound;
import com.anasdidi.uam.dto.DeleteUserReqDTO;
import com.anasdidi.uam.dto.DeleteUserResDTO;
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
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DeleteUserService implements UamService<DeleteUserReqDTO, DeleteUserResDTO> {

  private final UserRepository userRepository;

  @Override
  public DeleteUserResDTO execute(@Valid DeleteUserReqDTO req) {
    var res = DeleteUserResDTO.builder();

    var entity = userRepository
        .findByIdAndVersion(req.getPayload().getUserId(), req.getPayload().getVersion())
        .orElseThrow(() -> new E03ResourceNotFound(
            ResourceEnum.USER,
            Map.of(
                "userId",
                req.getPayload().getUserId(),
                "version",
                req.getPayload().getVersion())));

    entity.setIsDeleted(true);
    userRepository.save(entity);

    return res.response(ResponseEnum.S02_DELETED).build();
  }
}
