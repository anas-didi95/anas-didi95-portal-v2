package com.anasdidi.uam.dto;

import com.anasdidi.common.dto.BaseReqDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class HelloWorldReqDTO extends BaseReqDTO {
  @Valid @NotNull private HelloWorldReqDTOPayload payload;

  @NoArgsConstructor
  @AllArgsConstructor
  @Data
  @SuperBuilder
  @Jacksonized
  @JsonIgnoreProperties(ignoreUnknown = true)
  @ToString
  public static class HelloWorldReqDTOPayload {
    @NotBlank private String name;
  }
}
