package com.anasdidi.uam.entity;

import com.anasdidi.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Entity(name = "T_USER")
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity {

  @Column(name = "USERNAME")
  private String username;

  @Column(name = "PWD")
  private String password;

  @Column(name = "NM")
  private String name;
}
