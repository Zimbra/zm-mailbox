package com.zimbra.common.mailbox;

public class MailboxLockContext {

    private MailboxStore mailbox;
    private String caller;

    public MailboxLockContext(MailboxStore mailbox) {
        this(mailbox, null);
    }

    public MailboxLockContext(MailboxStore mailbox, String caller) {
        this.mailbox = mailbox;
        this.caller = caller;
    }

    public MailboxStore getMailboxStore() {
        return mailbox;
    }

    public String getCaller() {
        return caller;
    }
}
