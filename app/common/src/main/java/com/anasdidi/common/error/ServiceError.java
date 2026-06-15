package com.anasdidi.common.error;

import com.anasdidi.common.enums.ResponseEnum;
import lombok.Getter;

@Getter
public abstract class ServiceError extends RuntimeException {

  private final ResponseEnum response;

  ServiceError(ResponseEnum response) {
    this.response = response;
  }
}
