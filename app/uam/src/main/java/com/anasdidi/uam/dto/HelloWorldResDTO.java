package com.anasdidi.uam.dto;

import com.anasdidi.common.enums.ResponseEnum;
import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record HelloWorldResDTO(
    String correlationId,
    Integer timeTaken,
    OffsetDateTime timestamp,
    String responseCode,
    String responseDesc,
    ResponseEnum response,
    HelloWorldResDTOPayload payload)
    implements IUamResDTO {

  @Override
  public String responseCode() {
    return this.response().code;
  }

  @Override
  public String responseDesc() {
    return this.response().message;
  }

  @Builder
  public record HelloWorldResDTOPayload(String greeting) {}
}
