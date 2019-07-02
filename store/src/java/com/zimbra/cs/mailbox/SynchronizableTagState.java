package com.zimbra.cs.mailbox;

import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;

public class SynchronizableTagState extends SynchronizableMailItemState implements TagState {

    private Boolean imapVisible = null;
    private Boolean listed = null;

    public static final String F_IMAP_VISIBLE = "imapVisible";
    public static final String F_LISTED = "listed";

    public SynchronizableTagState(UnderlyingData data) {
        super(data);
    }

    @Override
    public boolean isImapVisible() {
        Boolean val = getBoolField(F_IMAP_VISIBLE).get();
        if (val != null) {
            return val;
        }
        ZimbraLog.cache.info("isImapVisible() would have returned null (imapVisible=%s) %s",
                imapVisible, this);
        return (imapVisible != null) ? imapVisible : false;
    }

    @Override
    public void setImapVisible(boolean imapVisible) {
        setImapVisible(imapVisible, AccessMode.DEFAULT);
    }

    @Override
    public void setImapVisible(boolean imapVisible, AccessMode setMode) {
        getField(F_IMAP_VISIBLE).set(imapVisible, setMode);
    }

    @Override
    public boolean isListed() {
        Boolean val =  getBoolField(F_LISTED).get();
        if (val != null) {
            return val;
        }
        ZimbraLog.cache.info("isListed() would have returned null (listed=%s) %s", listed, this);
        return (listed != null) ? listed : false;
    }

    @Override
    public void setListed(boolean listed) {
        setListed(listed, AccessMode.DEFAULT);
    }

    @Override
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
            protected void setLocal(Boolean value) {
                if (value == null) {
                    ZimbraLog.cache.info("setLocal(%s) (name=%s) %s", value, name, this);
                    imapVisible = false;
                } else {
                    imapVisible = value;
                }
            }

            @Override
            protected Boolean getLocal() {
                return (imapVisible == null) ? false : imapVisible;
            }
        });

        addField(new ItemField<Boolean>(F_LISTED) {

            @Override
            protected void setLocal(Boolean value) {
                listed = value;
            }

            @Override
            protected Boolean getLocal() {
                if (listed != null) {
                    return listed;
                }
                String encMetadata = getUnderlyingData().metadata;
                if (encMetadata == null) {
                    ZimbraLog.cache.info("getLocal() (name=%s) no metadata %s", name, this);
                    return false;
                }
                try {
                    Metadata metadata = new Metadata(encMetadata);
                    listed = metadata.getBool(Metadata.FN_LISTED, false);
                } catch (ServiceException e) {
                    ZimbraLog.cache.error("getLocal() (name=%s) problem decoding metadata %s",
                            name, this, e);
                    return false;
                }
                return listed;
            }
        });
    }

    @Override
    protected MoreObjects.ToStringHelper toStringHelper() {
        return super.toStringHelper()
                .add("listed", listed)
                .add("imapVisible", imapVisible);
    }
}

