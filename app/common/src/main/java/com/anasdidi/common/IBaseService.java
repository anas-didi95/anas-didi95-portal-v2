package com.anasdidi.common;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Validated
public interface IBaseService<A extends BaseReqDTO, B extends BaseResDTO> {

  Mono<B> execute(@Valid A req);
}
