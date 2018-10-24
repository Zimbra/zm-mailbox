package com.zimbra.cs.mailbox;

public abstract interface TransactionListener {

    public void transactionBegin(boolean startChange);

    public void transactionEnd(boolean success, boolean endChange);

    public void commitCache();

    public void rollbackCache();

}
