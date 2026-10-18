package org.hit.chiikaiwabe.service;

import org.hit.chiikaiwabe.domain.dto.pagination.PaginationFullRequestDto;
import org.hit.chiikaiwabe.domain.dto.pagination.PaginationResponseDto;
import org.hit.chiikaiwabe.domain.dto.request.UserCreateDto;
import org.hit.chiikaiwabe.domain.dto.request.UserUpdateDto;
import org.hit.chiikaiwabe.domain.dto.response.UserDto;
import org.hit.chiikaiwabe.domain.entity.User;
import org.hit.chiikaiwabe.security.UserPrincipal;

public interface UserService {

  UserDto getUserById(String userId);

  PaginationResponseDto<UserDto> getCustomers(PaginationFullRequestDto request);

  UserDto getCurrentUser(UserPrincipal principal);

}
