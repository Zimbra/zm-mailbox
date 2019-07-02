package com.zimbra.cs.mailbox;


public interface TagState extends MailItemState {

    public boolean isImapVisible();

    public void setImapVisible(boolean imapVisible);

    public void setImapVisible(boolean imapVisible, AccessMode setMode);

    public boolean isListed();

    public void setListed(boolean listed);

    public void setListed(boolean listed, AccessMode setMode);
}
