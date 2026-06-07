package com.anasdidi.uam.dto;

import com.anasdidi.common.BaseResDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class HelloWorldResDTO extends BaseResDTO {
  private HelloWorldResDTO2Payload payload;

  @Data
  @SuperBuilder
  @JsonIgnoreProperties(ignoreUnknown = true)
  @ToString
  public static class HelloWorldResDTO2Payload {
    private String greeting;
  }
}
