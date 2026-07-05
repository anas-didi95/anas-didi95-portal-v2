package com.anasdidi.uam.service.impl;

import com.anasdidi.common.enums.ResponseEnum;
import com.anasdidi.uam.dto.HelloWorldReqDTO;
import com.anasdidi.uam.dto.HelloWorldResDTO;
import com.anasdidi.uam.dto.HelloWorldResDTO.HelloWorldResDTOPayload;
import com.anasdidi.uam.service.UamService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HelloWorldService implements UamService<HelloWorldReqDTO, HelloWorldResDTO> {

  @Override
  public HelloWorldResDTO execute(@Valid HelloWorldReqDTO req) {
    var greeting = prepareGreeting(req);
    return HelloWorldResDTO.builder()
        .correlationId(req.getCorrelationId())
        .response(ResponseEnum.S00_SUCCESS)
        .payload(HelloWorldResDTOPayload.builder().greeting(greeting).build())
        .build();
  }

  private String prepareGreeting(HelloWorldReqDTO in) {
    return "Hi, %s".formatted(in.getPayload().getName());
  }
}
