package com.anasdidi.common.error;

import com.anasdidi.common.enums.ResourceEnum;
import com.anasdidi.common.enums.ResponseEnum;
import lombok.Getter;

@Getter
public class E02ResourceAlreadyExists extends ServiceError {

  private final ResourceEnum resource;

  public E02ResourceAlreadyExists(ResourceEnum resource) {
    super(ResponseEnum.E02_RESOURCE_ALREADY_EXISTS);
    this.resource = resource;
  }
}
