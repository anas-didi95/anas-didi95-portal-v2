package com.anasdidi.uam.dto;

import com.anasdidi.common.BaseResDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class HelloWorldResDTO extends BaseResDTO {
  private HelloWorldResDTOPayload payload;

  @Data
  @SuperBuilder
  @Jacksonized
  @JsonIgnoreProperties(ignoreUnknown = true)
  @ToString(callSuper = true)
  public static class HelloWorldResDTOPayload {
    private String greeting;
  }
}
