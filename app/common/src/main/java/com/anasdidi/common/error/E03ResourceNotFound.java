package com.anasdidi.common.error;

import com.anasdidi.common.enums.ResourceEnum;
import com.anasdidi.common.enums.ResponseEnum;
import lombok.Getter;

@Getter
public class E03ResourceNotFound extends ServiceError {

  private final ResourceEnum resource;

  public E03ResourceNotFound(ResourceEnum resource) {
    super(ResponseEnum.E03_RESOURCE_NOT_FOUND);
    this.resource = resource;
  }
}
