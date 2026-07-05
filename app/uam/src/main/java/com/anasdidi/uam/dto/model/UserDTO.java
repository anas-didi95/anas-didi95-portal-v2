package com.anasdidi.uam.dto.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
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
@ToString
public class UserDTO {

  private String username;

  @JsonIgnore
  private String password;

  private String name;

  // Metadata
  private UUID id;
  private Boolean isDeleted;
  private Integer version;
  private String createBy;
  private Instant createDate;
  private String updateBy;
  private Instant updateDate;
}
