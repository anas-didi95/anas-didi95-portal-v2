package com.anasdidi.common.enums;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ResponseEnum {
  S00_SUCCESS(HttpStatus.OK, "00", "Success"),
  E01_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "E01", "Validation Error"),
  E99_UNEXPECTED_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E99", "Unexpected Error");

  public final HttpStatus httpStatus;
  public final String code;
  public final String message;
}
