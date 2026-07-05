package com.anasdidi.uam.service;

import com.anasdidi.common.dto.BaseReqDTO;
import com.anasdidi.common.dto.BaseResDTO;
import com.anasdidi.common.service.IBaseService;

public interface UamService<A extends BaseReqDTO, B extends BaseResDTO>
    extends IBaseService<A, B> {}
