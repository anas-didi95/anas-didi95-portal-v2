package com.anasdidi.uam.dto;

import com.anasdidi.common.dto.BaseReqDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.hibernate.validator.constraints.Length;

@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RegisterUserReqDTO extends BaseReqDTO {
  @Valid @NonNull private RegisterUserReqDTOPayload payload;

  @NoArgsConstructor
  @AllArgsConstructor
  @Data
  @SuperBuilder
  @Jacksonized
  @JsonIgnoreProperties(ignoreUnknown = true)
  @ToString
  public static class RegisterUserReqDTOPayload {
    @NotBlank @Length(max = 20) private String username;

    @NotBlank private String password;

    @NotBlank private String name;
  }
}
