package com.anasdidi.uam.service.impl;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.HelloWorldReqDTO2;
import com.anasdidi.uam.dto.HelloWorldResDTO2;
import com.anasdidi.uam.dto.HelloWorldResDTO2.HelloWorldResDTO2Payload;
import com.anasdidi.uam.service.UamService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Service
@Validated
public class HelloWorldService implements UamService<HelloWorldReqDTO2, HelloWorldResDTO2> {

  @Override
  public Mono<HelloWorldResDTO2> execute(@Valid HelloWorldReqDTO2 req) {
    var res = HelloWorldResDTO2.builder().correlationId(req.getCorrelationId());
    var greeting = "Hi, %s".formatted(req.getPayload().getName());

    res.payload(HelloWorldResDTO2Payload.builder().greeting(greeting).build());
    res.response(ResponseEnum.S00_SUCCESS);
    return Mono.just(res.build());
  }
}
