package com.anasdidi.uam.dto;

import com.anasdidi.common.IBaseReqDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record HelloWorldReqDTO(
    @NotBlank String correlationId, @Valid @NotNull HelloWorldReqDTOPayload payload)
    implements IBaseReqDTO {

  @Builder
  public record HelloWorldReqDTOPayload(@NotBlank String name) {}
}
