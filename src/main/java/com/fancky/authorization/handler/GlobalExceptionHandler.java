package com.fancky.authorization.handler;



import com.fancky.authorization.model.response.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * 只能捕捉进入controller里异常的代码。
 *
 * extends ResponseEntityExceptionHandler
 *
 * @ControllerAdvice :注解定义全局异常处理类
 * @ExceptionHandler :注解声明异常处理方法
 *
 *
 * @ResponseBody 的作用：把方法的返回值，直接写进 HTTP 响应体（Body），而不是当成“视图名”去解析。
 *
 *
 *
 *
 */
@Slf4j
// 指定最高优先级，避免多个异常处理类
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private HttpServletResponse httpServletResponse;
    @Autowired
    private HttpServletRequest httpServletRequest;


    /**
     * 处理404 Not Found异常
     * 需配合ErrorController使用
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public MessageResult<String> handle404(NoHandlerFoundException ex) {
        log.error("404异常 - 请求URL: {}", httpServletRequest.getRequestURL());
        String errorMsg = "资源不存在: " + httpServletRequest.getRequestURI();
        MessageResult<Void> result = new MessageResult<>();
        result.setCode(HttpStatus.NOT_FOUND.value());
        result.setMessage(errorMsg);
        return MessageResult.faile();

    }


    /**
     * ResponseEntity<MessageResult<Void>> 前端收到后端返回的json  和 MessageResult<Void>一样
     * 404无法进入此方法。404被tomcat拦截了
     * 自定义返回数据格式
     *
     * ResponseEntity:
     * 需要精确控制HTTP状态码
     * 需要设置自定义响应头
     * RESTful API设计严格遵循HTTP语义
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResult<Void>> exceptionHandler(Exception ex, WebRequest request) {
        logger.error("", ex);
        // 检查响应是否已经被提交,AuthenticationFilter 异常已返回
        if (httpServletResponse.isCommitted()) {
            logger.warn("Response already committed, cannot handle exception properly", ex);
            // 返回一个空对象
            return null;
        }
        //ResponseEntity.ok 会覆盖状态
//        httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        MessageResult<Void> messageResult = new MessageResult<>();
        messageResult.setCode(500);
        messageResult.setMessage(ex.getMessage());
        messageResult.setSuccess(false);
//        Void.class
        //     MDC.put("traceId", traceId);//traceId在过滤器的destroy()中清除
        //   messageResult.setTraceId(MDC.get("traceId"));
//        return ResponseEntity.ok(messageResult);
//        logger.error(ex.toString());// 不会打出异常的堆栈信息

    /**

            ResponseEntity:
             需要精确控制HTTP状态码
             需要设置自定义响应头
            RESTful API设计严格遵循HTTP语义
         */
        return ResponseEntity
                .status(status)
                .body(messageResult);

//        return ResponseEntity.ok(messageResult);
    }
//
//    @ExceptionHandler({
//            FeignException.class,
//            FeignException.Unauthorized.class,
//            FeignException.Forbidden.class,
//            FeignException.NotFound.class  // 新增404处理
//    })
//    public ResponseEntity<MessageResult<Void>> handleFeignException(FeignException ex) {
//
//        //用此重载，打印异常的所有信息
//        logger.error("", ex);
//        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
//        String errorMsg = "服务调用异常:" + ex.getMessage();
//
//        // 根据具体异常类型细化处理
//        if (ex instanceof FeignException.Unauthorized) {
//            status = HttpStatus.UNAUTHORIZED;
//            errorMsg = "认证失败，请检查访问凭证";
//        } else if (ex instanceof FeignException.Forbidden) {
//            status = HttpStatus.FORBIDDEN;
//            errorMsg = "权限不足，禁止访问";
//        } else if (ex instanceof FeignException.NotFound) {
//            status = HttpStatus.NOT_FOUND;
//            errorMsg = "请求资源不存在: " + ex.request().url();
//        }
//        UserInfoHolder.clearUser();
//        // 统一响应构造
//        MessageResult<Void> result = new MessageResult<>();
//        result.setCode(status.value());
//        result.setMessage(errorMsg);
////        return MessageResult.faile();
//        return ResponseEntity
//                .status(status)
//                .body(result);
//
//
////        ResponseEntity 是Spring提供的完整响应封装器，可以精确控制：
////        return ResponseEntity
////                .status(404)                // HTTP状态码
////                .header("X-Custom", "123")  // 自定义头
////                .body(result);              // 响应体
//
////        直接返回MessageResult时，Spring会自动包装为200 OK响应，状态码不可控
//    }


    /**
     * @Validated 注解验证
     * @param ex
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public MessageResult<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();

        String errorMessage = bindingResult.getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                // 兜底：处理 @Validated 类级别校验
                .orElseGet(() -> bindingResult.getGlobalErrors().stream()
                        .findFirst()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .orElse("Validation failed"));
        return MessageResult.faile(errorMessage);
    }
}
