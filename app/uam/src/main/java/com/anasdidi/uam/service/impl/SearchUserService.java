package com.anasdidi.uam.service.impl;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.SearchUserReqDTO;
import com.anasdidi.uam.dto.SearchUserResDTO;
import com.anasdidi.uam.dto.SearchUserResDTO.SearchUserResDTOPayload;
import com.anasdidi.uam.dto.model.UserDTO;
import com.anasdidi.uam.repository.UserRepository;
import com.anasdidi.uam.service.UamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class SearchUserService implements UamService<SearchUserReqDTO, SearchUserResDTO> {

  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  @Override
  public SearchUserResDTO execute(@Valid SearchUserReqDTO req) {
    var res = SearchUserResDTO.builder();

    var resultList = userRepository.findAll((root, query, builder) -> {
      List<Predicate> list = new ArrayList<>();

      var name = req.getPayload().getName();
      if (StringUtils.isNoneBlank(name)) {
        log.debug("like name={}", name);
        list.add(builder.like(root.get("name"), "%" + name + "%"));
      }

      return builder.and(list);
    });

    var payload = SearchUserResDTOPayload.builder()
        .resultList(resultList.stream()
            .map(o -> objectMapper.convertValue(o, UserDTO.class))
            .toList())
        .build();

    return res.response(ResponseEnum.S00_SUCCESS).payload(payload).build();
  }
}
