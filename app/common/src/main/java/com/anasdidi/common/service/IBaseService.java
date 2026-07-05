package com.anasdidi.common.service;

import com.anasdidi.common.dto.BaseReqDTO;
import com.anasdidi.common.dto.BaseResDTO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public interface IBaseService<A extends BaseReqDTO, B extends BaseResDTO> {

  B execute(@Valid A req);
}
