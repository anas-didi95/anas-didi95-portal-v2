package com.anasdidi.uam.dto;

import com.anasdidi.common.IBaseResDTO;
import com.anasdidi.common.enums.ResponseEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

@Builder
public record HelloWorldResDTO(
    String correlationId, @JsonIgnore ResponseEnum response, HelloWorldResDTOPayload payload)
    implements IBaseResDTO {

  @Builder
  public record HelloWorldResDTOPayload(String greeting) {}
}
