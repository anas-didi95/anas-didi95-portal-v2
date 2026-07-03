package com.anasdidi.common.error;

import com.anasdidi.common.enums.ResourceEnum;
import com.anasdidi.common.enums.ResponseEnum;
import java.util.Map;
import lombok.Getter;

@Getter
public class E03ResourceNotFound extends ServiceError {

  private final ResourceEnum resource;
  private final Map<String, Object> param;

  public E03ResourceNotFound(ResourceEnum resource) {
    this(resource, null);
  }

  public E03ResourceNotFound(ResourceEnum resource, Map<String, Object> param) {
    super(ResponseEnum.E03_RESOURCE_NOT_FOUND);
    this.resource = resource;
    this.param = param;
  }
}
