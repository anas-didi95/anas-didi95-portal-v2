package com.anasdidi.uam.dto;

import com.anasdidi.common.BaseReqDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class HelloWorldReqDTO extends BaseReqDTO {
  @Valid @NonNull private HelloWorldReqDTO2Payload payload;

  @Data
  @SuperBuilder
  @Jacksonized
  @JsonIgnoreProperties(ignoreUnknown = true)
  @ToString(callSuper = true)
  public static class HelloWorldReqDTO2Payload {
    @NotBlank private String name;
  }
}
