package com.zimbra.cs.mailbox;

import com.zimbra.cs.mailbox.MailItem.UnderlyingData;

public class TagState extends MailItemState {

    private boolean imapVisible;
    private boolean listed;

    public static final String F_IMAP_VISIBLE = "imapVisible";
    public static final String F_LISTED = "listed";

    public TagState(UnderlyingData data) {
        super(data);
    }

    public boolean isImapVisible() {
        return getBoolField(F_IMAP_VISIBLE).get();
    }

    public void setImapVisible(boolean imapVisible) {
        setImapVisible(imapVisible, AccessMode.DEFAULT);
    }

    public void setImapVisible(boolean imapVisible, AccessMode setMode) {
        getField(F_IMAP_VISIBLE).set(imapVisible, setMode);
    }

    public boolean isListed() {
        return getBoolField(F_LISTED).get();
    }

    public void setListed(boolean listed) {
        setListed(listed, AccessMode.DEFAULT);
    }

    public void setListed(boolean listed, AccessMode setMode) {
        getField(F_LISTED).set(listed, setMode);
    }

    @Override
    protected void initFields() {
        super.initFields();
        initTagFields();
    }

    private void initTagFields() {
        addField(new ItemField<Boolean>(F_IMAP_VISIBLE) {

            @Override
            protected void setLocal(Boolean value) { imapVisible = value; }

            @Override
            protected Boolean getLocal() { return imapVisible; }
        });

        addField(new ItemField<Boolean>(F_LISTED) {

            @Override
            protected void setLocal(Boolean value) { listed = value; }

            @Override
            protected Boolean getLocal() { return listed; }
        });
    }
}

