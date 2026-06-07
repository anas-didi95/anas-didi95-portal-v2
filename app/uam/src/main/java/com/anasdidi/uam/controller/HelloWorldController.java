package com.anasdidi.uam.controller;

import com.anasdidi.common.CommonConstants;
import com.anasdidi.uam.dto.HelloWorldResDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

public interface HelloWorldController {

  String BASE_URL = "/hello-world";

  @GetMapping("/greeting")
  Mono<ResponseEntity<HelloWorldResDTO>> greeting(
      @RequestHeader(name = CommonConstants.HEADER_CORR_ID) String correlationId,
      @RequestParam(required = true) String name);
}
