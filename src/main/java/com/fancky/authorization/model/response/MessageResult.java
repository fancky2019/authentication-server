package com.fancky.authorization.model.response;

import org.slf4j.MDC;

import java.io.Serializable;


public class MessageResult<T> implements Serializable {
    //    private static final long serialVersionUID = 1L;
    private Integer code;
    /**
     * 执行结果（true:成功，false:失败）
     */
    private Boolean success;
    private String message;
    //  MDC.put("traceId", traceId);//traceId在过滤器的destroy()中生成、清除
    private String traceId = MDC.get("traceId");
    private T data;
    private Long timestamp;   // 时间戳

    public MessageResult() {
        this.success = true;
        this.timestamp = timestamp;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

//    public void setTraceId(String traceId) {
//        this.traceId = traceId;
//    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getTraceId() {
        return traceId;
    }

    public static <T> MessageResult<T> success() {
        MessageResult<T> messageResult = new MessageResult<>();
        messageResult.setSuccess(true);
        messageResult.setCode(200);
        messageResult.setTimestamp(System.currentTimeMillis());
        return messageResult;
    }
    public static <T> MessageResult<T> success(T data) {
        MessageResult<T> messageResult = new MessageResult<>();
        messageResult.setSuccess(true);
        messageResult.setCode(200);
        messageResult.setData(data);
        messageResult.setTimestamp(System.currentTimeMillis());
        return messageResult;
    }

    public static <T> MessageResult<T> faile(T data) {
        MessageResult<T> messageResult = new MessageResult<>();
        messageResult.setSuccess(false);
        messageResult.setCode(500);
        messageResult.setData(data);
        messageResult.setTimestamp(System.currentTimeMillis());
        return messageResult;
    }

    public static <T> MessageResult<T> faile() {
        MessageResult<T> messageResult = new MessageResult<>();
        messageResult.setSuccess(false);
        messageResult.setCode(500);
        messageResult.setTimestamp(System.currentTimeMillis());
        return messageResult;
    }

    public static <T> MessageResult<T> faile(Integer code, String message) {
        MessageResult<T> result = new MessageResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> MessageResult<T> faile(Integer code, String message, T data) {
        MessageResult<T> result = new MessageResult<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> MessageResult<T> getMessageResult( T data,boolean success, int code) {
        MessageResult<T> messageResult = new MessageResult<>();
        messageResult.setSuccess(success);
        messageResult.setCode(code);
        messageResult.setData(data);
        messageResult.setTimestamp(System.currentTimeMillis());
        return messageResult;
    }
}
