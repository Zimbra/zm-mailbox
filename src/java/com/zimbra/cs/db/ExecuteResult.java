package com.zimbra.cs.db;
/**
 * SQL execute return wrapper 
 */
public class ExecuteResult<T> {
    public T result;
    
    public ExecuteResult(T result) {
        this.result = result;
    }
    
    public T getResult() {
        return result;
    }
}
