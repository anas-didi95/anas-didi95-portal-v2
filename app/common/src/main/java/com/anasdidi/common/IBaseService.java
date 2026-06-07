package com.anasdidi.common;

import com.anasdidi.common.aspect.ExecuteTrace;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Validated
public interface IBaseService<A extends IBaseReqDTO, B extends IBaseResDTO> {

  @ExecuteTrace
  Mono<B> execute(@Valid A req);
}
