package com.anasdidi.common;

import com.anasdidi.common.enums.ResponseEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;

public interface IBaseResDTO {

  String correlationId();

  Integer timeTaken();

  OffsetDateTime timestamp();

  String responseCode();

  String responseDesc();

  @JsonIgnore
  ResponseEnum response();
}
