package com.anasdidi.uam.service.impl;

import com.anasdidi.uam.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.HelloWorldReqDTO;
import com.anasdidi.uam.dto.HelloWorldResDTO;
import com.anasdidi.uam.dto.HelloWorldResDTO.HelloWorldResDTOPayload;
import com.anasdidi.uam.service.UamService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Service
@Validated
public class HelloWorldService implements UamService<HelloWorldReqDTO, HelloWorldResDTO> {

  @Override
  public Mono<HelloWorldResDTO> execute(@Valid HelloWorldReqDTO req) {
    var res = HelloWorldResDTO.builder().correlationId(req.correlationId());
    res.payload(HelloWorldResDTOPayload.builder()
        .greeting("Hi, %s".formatted(req.payload().name()))
        .build());
    res.response(ResponseEnum.S00_SUCCESS);
    return Mono.just(res.build());
  }
}
