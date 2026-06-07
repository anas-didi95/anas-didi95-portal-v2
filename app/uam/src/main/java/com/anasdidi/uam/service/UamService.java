package com.anasdidi.uam.service;

import com.anasdidi.common.IBaseService;
import com.anasdidi.uam.dto.IUamReqDTO;
import com.anasdidi.uam.dto.IUamResDTO;

public interface UamService<A extends IUamReqDTO, B extends IUamResDTO>
    extends IBaseService<A, B> {}
