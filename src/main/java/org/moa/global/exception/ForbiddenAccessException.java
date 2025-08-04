package org.moa.global.exception;

import org.moa.global.handler.BusinessException;
import org.moa.global.type.StatusCode;

public class ForbiddenAccessException extends BusinessException {

    public ForbiddenAccessException() {
        super(StatusCode.FORBIDDEN, "해당 작업에 대한 권한이 없습니다.");
    }

    public ForbiddenAccessException(String message) {
        super(StatusCode.FORBIDDEN, message);
    }
}