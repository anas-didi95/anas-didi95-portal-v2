package com.anasdidi.common.error;

import com.anasdidi.common.enums.ResponseEnum;
import lombok.Getter;

@Getter
public class E99UnexpectedError extends ServiceError {

  private final String reason;
  private final Exception ex;

  public E99UnexpectedError(String reason) {
    this(reason, null);
  }

  public E99UnexpectedError(String reason, Exception ex) {
    super(ResponseEnum.E99_UNEXPECTED_ERROR);
    this.reason = reason;
    this.ex = ex;
  }
}
