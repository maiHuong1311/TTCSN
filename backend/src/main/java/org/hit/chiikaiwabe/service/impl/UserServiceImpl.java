package org.hit.chiikaiwabe.service.impl;

import org.hit.chiikaiwabe.constant.ErrorMessage;
import org.hit.chiikaiwabe.constant.SortByDataConstant;
import org.hit.chiikaiwabe.domain.dto.pagination.PaginationFullRequestDto;
import org.hit.chiikaiwabe.domain.dto.pagination.PaginationResponseDto;
import org.hit.chiikaiwabe.domain.dto.response.UserDto;
import org.hit.chiikaiwabe.domain.entity.User;
import org.hit.chiikaiwabe.domain.mapper.UserMapper;
import org.hit.chiikaiwabe.exception.NotFoundException;
import org.hit.chiikaiwabe.repository.UserRepository;
import org.hit.chiikaiwabe.security.UserPrincipal;
import org.hit.chiikaiwabe.service.UserService;
import org.hit.chiikaiwabe.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  private final UserMapper userMapper;

  @Override
  public UserDto getUserById(String userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException(ErrorMessage.User.ERR_NOT_FOUND_ID, new String[]{userId}));
    return userMapper.toUserDto(user);
  }

  @Override
  public PaginationResponseDto<UserDto> getCustomers(PaginationFullRequestDto request) {
    //Pagination
    Pageable pageable = PaginationUtil.buildPageable(request, SortByDataConstant.USER);
    //Create Output
    return new PaginationResponseDto<>(null, null);
  }

  @Override
  public UserDto getCurrentUser(UserPrincipal principal) {
    User user = userRepository.getUser(principal);
    return userMapper.toUserDto(user);
  }

}
