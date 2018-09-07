package com.zimbra.cs.mailbox;

public abstract interface TransactionListener {

    public void transactionBegin();

    public void transactionEnd(boolean success);

}
