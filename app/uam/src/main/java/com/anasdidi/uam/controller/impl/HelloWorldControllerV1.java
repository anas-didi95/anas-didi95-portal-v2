package com.anasdidi.uam.controller.impl;

import com.anasdidi.common.CommonConstants;
import com.anasdidi.uam.UamConstants;
import com.anasdidi.uam.controller.HelloWorldController;
import com.anasdidi.uam.dto.HelloWorldReqDTO;
import com.anasdidi.uam.dto.HelloWorldReqDTO.HelloWorldReqDTOPayload;
import com.anasdidi.uam.dto.HelloWorldResDTO;
import com.anasdidi.uam.service.impl.HelloWorldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UamConstants.CONTEXT_PATH + CommonConstants.API_V1 + HelloWorldController.BASE_URL)
@RequiredArgsConstructor
public class HelloWorldControllerV1 implements HelloWorldController {

  private final HelloWorldService helloWorldService;

  @Override
  public ResponseEntity<HelloWorldResDTO> greeting(String correlationId, String name) {
    var req = HelloWorldReqDTO.builder()
        .correlationId(correlationId)
        .payload(HelloWorldReqDTOPayload.builder().name(name).build())
        .build();
    var res = helloWorldService.execute(req);
    return ResponseEntity.status(res.getResponse().httpStatus).body(res);
  }
}
