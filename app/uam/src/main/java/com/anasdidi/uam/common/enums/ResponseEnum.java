package com.anasdidi.uam.common.enums;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ResponseEnum {
  S00_SUCCESS(HttpStatus.OK, "00", "Success");

  public final HttpStatus httpStatus;
  public final String code;
  public final String message;
}
