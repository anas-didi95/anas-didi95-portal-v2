package com.anasdidi.common;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Validated
public interface IBaseService<A extends IBaseReqDTO, B extends IBaseResDTO> {

  Mono<B> execute(@Valid A req);
}
