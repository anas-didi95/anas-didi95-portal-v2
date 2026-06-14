package com.anasdidi.common;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public interface IBaseService<A extends BaseReqDTO, B extends BaseResDTO> {

  B execute(@Valid A req);
}
