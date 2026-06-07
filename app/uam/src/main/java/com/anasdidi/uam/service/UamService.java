package com.anasdidi.uam.service;

import com.anasdidi.common.BaseReqDTO;
import com.anasdidi.common.BaseResDTO;
import com.anasdidi.common.IBaseService;

public interface UamService<A extends BaseReqDTO, B extends BaseResDTO>
    extends IBaseService<A, B> {}
