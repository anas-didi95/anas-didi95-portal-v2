package com.anasdidi.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class PaginationDTO {

  @Min(1) private Integer pageNo;

  @Min(1) private Integer totalRecordsPerPage;

  private Integer totalPages;
  private Long totalRecords;
}
