package com.anasdidi.uam.service;

import com.anasdidi.uam.common.IBaseReqDTO;
import com.anasdidi.uam.common.IBaseResDTO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Validated
public interface UamService<A extends IBaseReqDTO, B extends IBaseResDTO> {

  Mono<B> execute(@Valid A req);
}
