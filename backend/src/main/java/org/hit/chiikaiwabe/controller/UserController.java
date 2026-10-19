package org.hit.chiikaiwabe.controller;

import org.hit.chiikaiwabe.base.RestApiV1;
import org.hit.chiikaiwabe.base.VsResponseUtil;
import org.hit.chiikaiwabe.constant.UrlConstant;
import org.hit.chiikaiwabe.domain.dto.pagination.PaginationFullRequestDto;
import org.hit.chiikaiwabe.security.CurrentUser;
import org.hit.chiikaiwabe.security.UserPrincipal;
import org.hit.chiikaiwabe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.validation.Valid;

@RequiredArgsConstructor
@RestApiV1
public class UserController {

  private final UserService userService;

  @Tag(name = "user-controller-admin")
  @Operation(summary = "API get user")
  @GetMapping(UrlConstant.User.GET_USER)
  public ResponseEntity<?> getUserById(@PathVariable String userId) {
    return VsResponseUtil.success(userService.getUserById(userId));
  }

  @Tags({@Tag(name = "user-controller-admin"), @Tag(name = "user-controller")})
  @Operation(summary = "API get current user login")
  @GetMapping(UrlConstant.User.GET_CURRENT_USER)
  public ResponseEntity<?> getCurrentUser(@Parameter(name = "principal", hidden = true)
                                          @CurrentUser UserPrincipal principal) {
    return VsResponseUtil.success(userService.getCurrentUser(principal));
  }

  @Tag(name = "user-controller-admin")
  @Operation(summary = "API get all customer")
  @GetMapping(UrlConstant.User.GET_USERS)
  public ResponseEntity<?> getCustomers(@Valid @ParameterObject PaginationFullRequestDto requestDTO) {
    return VsResponseUtil.success(userService.getCustomers(requestDTO));
  }

}
