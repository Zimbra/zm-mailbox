package com.zimbra.cs.mailbox.cache;

public interface SharedStateAccessor {

    public <T> T get(String fieldName);

    public void set(String fieldName, Object value);

    public void unset(String fieldName);

    public void delete();
}
