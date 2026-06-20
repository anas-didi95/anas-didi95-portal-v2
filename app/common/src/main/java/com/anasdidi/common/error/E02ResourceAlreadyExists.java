package com.anasdidi.common.error;

import com.anasdidi.common.enums.ResponseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class E02ResourceAlreadyExists extends ServiceError {

  private final Resource resource;

  public E02ResourceAlreadyExists(Resource resource) {
    super(ResponseEnum.E02_RESOURCE_ALREADY_EXISTS);
    this.resource = resource;
  }

  @RequiredArgsConstructor
  public enum Resource {
    USER("User");

    public final String resource;
  }
}
