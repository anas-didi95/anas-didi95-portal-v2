package com.anasdidi.uam.service.impl;

import com.anasdidi.common.enums.ResponseEnum;
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
    var in = Mono.just(req);
    var greeting = in.flatMap(this::prepareGreeting);
    var tuple = Mono.zip(in, greeting);

    return tuple.map(t -> HelloWorldResDTO.builder()
        .correlationId(t.getT1().getCorrelationId())
        .response(ResponseEnum.S00_SUCCESS)
        .payload(HelloWorldResDTOPayload.builder().greeting(t.getT2()).build())
        .build());
  }

  private Mono<String> prepareGreeting(HelloWorldReqDTO in) {
    return Mono.just("Hi, %s".formatted(in.getPayload().getName()));
  }
}
