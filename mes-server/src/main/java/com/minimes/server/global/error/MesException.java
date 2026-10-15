package com.minimes.server.global.error;

/**
 * 도메인 규칙 위반 예외. API 응답 변환은 3단계에서 @RestControllerAdvice로 처리한다.
 */
public class MesException extends RuntimeException {

    private final ErrorCode errorCode;

    public MesException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
