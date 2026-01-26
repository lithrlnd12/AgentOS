package com.android.automation.core;

import java.util.Map;
import java.util.HashMap;

/**
 * Result of action execution
 */
public class ExecutionResult {
    private final boolean success;
    private final String message;
    private final String error;
    private final ActionExecutor.ErrorCode errorCode;
    private final Map<String, Object> data;
    private final long executionTime;
    private final String methodUsed;
    
    private ExecutionResult(Builder builder) {
        this.success = builder.success;
        this.message = builder.message;
        this.error = builder.error;
        this.errorCode = builder.errorCode;
        this.data = new HashMap<>(builder.data);
        this.executionTime = builder.executionTime;
        this.methodUsed = builder.methodUsed;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getError() {
        return error;
    }
    
    public ActionExecutor.ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }
    
    public long getExecutionTime() {
        return executionTime;
    }
    
    public String getMethodUsed() {
        return methodUsed;
    }
    
    public static Builder success() {
        return new Builder(true);
    }
    
    public static Builder failure(String error, ActionExecutor.ErrorCode errorCode) {
        return new Builder(false).error(error).errorCode(errorCode);
    }
    
    public static class Builder {
        private final boolean success;
        private String message = "";
        private String error = "";
        private ActionExecutor.ErrorCode errorCode = ActionExecutor.ErrorCode.SUCCESS;
        private Map<String, Object> data = new HashMap<>();
        private long executionTime = 0;
        private String methodUsed = "";
        
        private Builder(boolean success) {
            this.success = success;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder error(String error) {
            this.error = error;
            return this;
        }
        
        public Builder errorCode(ActionExecutor.ErrorCode errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public Builder data(String key, Object value) {
            this.data.put(key, value);
            return this;
        }
        
        public Builder data(Map<String, Object> data) {
            this.data.putAll(data);
            return this;
        }
        
        public Builder executionTime(long executionTime) {
            this.executionTime = executionTime;
            return this;
        }
        
        public Builder methodUsed(String methodUsed) {
            this.methodUsed = methodUsed;
            return this;
        }
        
        public ExecutionResult build() {
            return new ExecutionResult(this);
        }
    }
    
    @Override
    public String toString() {
        return "ExecutionResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", error='" + error + '\'' +
                ", errorCode=" + errorCode +
                ", executionTime=" + executionTime +
                ", methodUsed='" + methodUsed + '\'' +
                '}';
    }
}
