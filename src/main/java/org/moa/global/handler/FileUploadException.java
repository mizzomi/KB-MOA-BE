package org.moa.global.handler;

import org.moa.global.type.StatusCode;

public class FileUploadException extends BusinessException {
    public FileUploadException(String message, Throwable cause) {
        super(StatusCode.INTERNAL_ERROR, message);
        initCause(cause);
    }
}
