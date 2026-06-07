package com.anasdidi.common;

import com.anasdidi.common.enums.ResponseEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public abstract class BaseResDTO {

  private String correlationId;
  private String traceId;
  private OffsetDateTime timestamp;
  private Long timeTaken;
  private String responseCode;
  private String responseDesc;

  @JsonIgnore
  private ResponseEnum response;
}
