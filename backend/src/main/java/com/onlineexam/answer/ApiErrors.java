package com.onlineexam.answer;

import com.onlineexam.common.BizException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.*;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger log=LoggerFactory.getLogger(ApiErrors.class);
    @ExceptionHandler(BizException.class)
    ResponseEntity<AnswerController.Response<Object>> authBusiness(BizException e) {
        return response(e.getCode(), e.getMessage());
    }
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<AnswerController.Response<Object>> business(BusinessException e) {return response(e.status,e.getMessage());}
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<AnswerController.Response<Object>> invalid(Exception e) {return response(400,"请求参数格式无效");}
    @ExceptionHandler(Exception.class)
    ResponseEntity<AnswerController.Response<Object>> unexpected(Exception e) {
        log.error("请求处理失败", e);
        return response(500,"服务暂不可用，请稍后重试");
    }
    private ResponseEntity<AnswerController.Response<Object>> response(int code,String message) {
        return ResponseEntity.status(code).body(new AnswerController.Response<>(code,message,null));
    }
}
