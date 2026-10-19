package org.hit.chiikaiwabe.domain.dto.request;

import org.hit.chiikaiwabe.constant.ErrorMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TokenRefreshRequestDto {

  @NotBlank(message = ErrorMessage.NOT_BLANK_FIELD)
  private String refreshToken;

}
