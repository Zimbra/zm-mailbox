package com.zimbra.cs.mailbox.cache;

public interface SharedState {


    public void attach(SharedStateAccessor sharedState);

    public boolean isAttached();

    public void detatch();

    public void sync();
}
