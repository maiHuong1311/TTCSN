package org.hit.chiikaiwabe.service;

import org.hit.chiikaiwabe.domain.dto.request.LoginRequestDto;
import org.hit.chiikaiwabe.domain.dto.request.TokenRefreshRequestDto;
import org.hit.chiikaiwabe.domain.dto.response.CommonResponseDto;
import org.hit.chiikaiwabe.domain.dto.response.LoginResponseDto;
import org.hit.chiikaiwabe.domain.dto.response.TokenRefreshResponseDto;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

  LoginResponseDto login(LoginRequestDto request);

  TokenRefreshResponseDto refresh(TokenRefreshRequestDto request);

  CommonResponseDto logout(HttpServletRequest request);

}
