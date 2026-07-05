package com.anasdidi.uam.controller.impl;

import com.anasdidi.common.CommonConstants;
import com.anasdidi.common.dto.PaginationDTO;
import com.anasdidi.uam.UamConstants;
import com.anasdidi.uam.controller.UserController;
import com.anasdidi.uam.dto.DeleteUserReqDTO;
import com.anasdidi.uam.dto.DeleteUserReqDTO.DeleteUserReqDTOPayload;
import com.anasdidi.uam.dto.DeleteUserResDTO;
import com.anasdidi.uam.dto.GetUserReqDTO;
import com.anasdidi.uam.dto.GetUserReqDTO.GetUserReqDTOPayload;
import com.anasdidi.uam.dto.GetUserResDTO;
import com.anasdidi.uam.dto.RegisterUserReqDTO;
import com.anasdidi.uam.dto.RegisterUserReqDTO.RegisterUserReqDTOPayload;
import com.anasdidi.uam.dto.RegisterUserResDTO;
import com.anasdidi.uam.dto.SearchUserReqDTO;
import com.anasdidi.uam.dto.SearchUserReqDTO.SearchUserReqDTOPayload;
import com.anasdidi.uam.dto.SearchUserResDTO;
import com.anasdidi.uam.dto.UpdateUserReqDTO;
import com.anasdidi.uam.dto.UpdateUserReqDTO.UpdateUserReqDTOPayload;
import com.anasdidi.uam.dto.UpdateUserResDTO;
import com.anasdidi.uam.service.impl.DeleteUserService;
import com.anasdidi.uam.service.impl.GetUserService;
import com.anasdidi.uam.service.impl.RegisterUserService;
import com.anasdidi.uam.service.impl.SearchUserService;
import com.anasdidi.uam.service.impl.UpdateUserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UamConstants.CONTEXT_PATH + CommonConstants.API_V1 + UserController.BASE_URL)
@RequiredArgsConstructor
public class UserControllerV1 implements UserController {

  private final RegisterUserService registerUserService;
  private final SearchUserService searchUserService;
  private final GetUserService getUserService;
  private final UpdateUserService updateUserService;
  private final DeleteUserService deleteUserService;

  @Override
  public ResponseEntity<RegisterUserResDTO> registerUser(
      String correlationId, RegisterUserReqDTOPayload body) {
    var req =
        RegisterUserReqDTO.builder().correlationId(correlationId).payload(body).build();
    var res = registerUserService.execute(req);
    return ResponseEntity.status(res.getResponse().httpStatus).body(res);
  }

  @Override
  public ResponseEntity<SearchUserResDTO> searchUser(
      String correlationId, String name, Integer pageNo, Integer totalRecordsPerPage) {
    var req = SearchUserReqDTO.builder()
        .correlationId(correlationId)
        .payload(SearchUserReqDTOPayload.builder()
            .name(name)
            .paginationDTO(PaginationDTO.builder()
                .pageNo(pageNo)
                .totalRecordsPerPage(totalRecordsPerPage)
                .build())
            .build())
        .build();
    var res = searchUserService.execute(req);
    return ResponseEntity.status(res.getResponse().httpStatus).body(res);
  }

  @Override
  public ResponseEntity<GetUserResDTO> getUser(String correlationId, UUID userId) {
    var req = GetUserReqDTO.builder()
        .correlationId(correlationId)
        .payload(GetUserReqDTOPayload.builder().userId(userId).build())
        .build();
    var res = getUserService.execute(req);
    return ResponseEntity.status(res.getResponse().httpStatus).body(res);
  }

  @Override
  public ResponseEntity<UpdateUserResDTO> updateUser(
      String correlationId, UUID userId, Integer version, UpdateUserReqDTOPayload body) {
    var req = UpdateUserReqDTO.builder()
        .correlationId(correlationId)
        .userId(userId)
        .version(version)
        .payload(body)
        .build();
    var res = updateUserService.execute(req);
    return ResponseEntity.status(res.getResponse().httpStatus).body(res);
  }

  @Override
  public ResponseEntity<DeleteUserResDTO> deleteUser(
      String correlationId, UUID userId, Integer version) {
    var req = DeleteUserReqDTO.builder()
        .correlationId(correlationId)
        .payload(
            DeleteUserReqDTOPayload.builder().userId(userId).version(version).build())
        .build();
    var res = deleteUserService.execute(req);
    return ResponseEntity.status(res.getResponse().httpStatus).body(res);
  }
}
