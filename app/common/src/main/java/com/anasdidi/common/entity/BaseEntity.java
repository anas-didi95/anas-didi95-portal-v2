/* (C) Anas Juwaidi Bin Mohd Jeffry. All rights reserved. */
package com.anasdidi.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "ID")
  private UUID id;

  @Column(name = "IS_DEL")
  private Boolean isDeleted;

  @Version
  @Column(name = "VER")
  private Integer version;

  @CreatedBy
  @Column(name = "CREATE_BY")
  private String createBy;

  @CreatedDate
  @Column(name = "CREATE_DT")
  private Instant createDate;

  @LastModifiedBy
  @Column(name = "UPDATE_BY")
  private String updateBy;

  @LastModifiedDate
  @Column(name = "UPDATE_DT")
  private Instant updateDate;
}
