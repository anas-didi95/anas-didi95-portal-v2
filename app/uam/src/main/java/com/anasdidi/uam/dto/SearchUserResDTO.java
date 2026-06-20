package com.anasdidi.uam.dto;

import com.anasdidi.common.dto.BaseResDTO;
import com.anasdidi.common.dto.PaginationDTO;
import com.anasdidi.uam.dto.model.UserDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
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
public class SearchUserResDTO extends BaseResDTO {
  private SearchUserResDTOPayload payload;

  @NoArgsConstructor
  @AllArgsConstructor
  @Data
  @SuperBuilder
  @Jacksonized
  @JsonIgnoreProperties(ignoreUnknown = true)
  @ToString
  public static class SearchUserResDTOPayload {
    private List<UserDTO> resultList;
    private PaginationDTO pagination;
  }
}
