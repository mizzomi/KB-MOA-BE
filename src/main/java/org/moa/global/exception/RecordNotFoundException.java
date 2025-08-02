package org.moa.global.exception;

import org.moa.global.handler.BusinessException;
import org.moa.global.type.StatusCode;

public class RecordNotFoundException extends BusinessException {
    public RecordNotFoundException() {
        super(StatusCode.NOT_FOUND, "해당 여행 기록을 찾을 수 없습니다.");
    }
}