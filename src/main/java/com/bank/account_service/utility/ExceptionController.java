package com.bank.account_service.utility;

import com.bank.account_service.exception.MinimumBalanceException;
import com.bank.account_service.exception.NoAccountFoundException;
import org.apache.juli.logging.Log;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.logging.Logger;

@RestControllerAdvice
public class ExceptionController {

//    Logger logger = Logger.getLogger(ExceptionController.class.getName());
    Log logger = org.apache.juli.logging.LogFactory.getLog(ExceptionController.class);


    @ExceptionHandler(NoAccountFoundException.class)
    public ResponseEntity<ErrorInfo> notFoundException(final Exception e) {
        final ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setErrorCode(HttpStatus.NOT_FOUND.value());
        errorInfo.setErrorMessage(e.getMessage());
        errorInfo.setTimestamp(LocalDateTime.now());
        logger.error("NoAccountFoundException: {}", e);

        return new ResponseEntity<>(errorInfo, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MinimumBalanceException.class)
    public ResponseEntity<ErrorInfo> minimumBalanceException(final Exception e) {
        final ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setErrorCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
        errorInfo.setErrorMessage(e.getMessage());
        errorInfo.setTimestamp(LocalDateTime.now());
        logger.error("Exception: {}", e);

        return new ResponseEntity<>(errorInfo, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
