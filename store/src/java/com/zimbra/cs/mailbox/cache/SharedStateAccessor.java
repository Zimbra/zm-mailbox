package com.zimbra.cs.mailbox.cache;

public interface SharedStateAccessor {

    public <T> T get(String fieldName);

    public <T> void set(String fieldName, T value);

    public void unset(String fieldName);

    public void delete();

    public boolean isInUse();
}
