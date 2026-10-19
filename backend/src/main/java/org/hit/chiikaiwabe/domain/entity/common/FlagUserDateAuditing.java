package org.hit.chiikaiwabe.domain.entity.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Setter
@Getter
public abstract class FlagUserDateAuditing extends UserDateAuditing {

  @Column(nullable = false)
  private Boolean deleteFlag = Boolean.FALSE;

  @Column(nullable = false)
  private Boolean activeFlag = Boolean.TRUE;

}
