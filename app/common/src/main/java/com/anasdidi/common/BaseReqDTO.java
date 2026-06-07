package com.anasdidi.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public abstract class BaseReqDTO {

  @NotBlank private String correlationId;
}
