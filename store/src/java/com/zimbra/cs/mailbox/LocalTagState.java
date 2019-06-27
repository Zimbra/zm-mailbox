package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;

public class LocalTagState extends LocalMailItemState implements ITagState {

    private Boolean imapVisible = null;
    private Boolean listed = null;

    public LocalTagState(UnderlyingData data) {
        super(data);
    }

    @Override
    public boolean isImapVisible() {
        return (imapVisible == null) ? false : imapVisible;
    }

    @Override
    public void setImapVisible(boolean imapVisible) {
        this.imapVisible = imapVisible;
    }

    @Override
    public void setImapVisible(boolean imapVisible, AccessMode setMode) {
        this.imapVisible = imapVisible;
    }

    @Override
    public boolean isListed() {
        if (listed == null) {
            try {
                Metadata metadata = new Metadata(data.metadata);
                listed = metadata.getBool(Metadata.FN_LISTED, false);
            } catch (ServiceException e) {
                ZimbraLog.cache.error("error decoding metadata for %s to get isTagListed", data.id);
                return false;
            }
        }
        return listed;
    }

    @Override
    public void setListed(boolean listed) {
        this.listed = listed;
    }

    @Override
    public void setListed(boolean listed, AccessMode setMode) {
        this.listed = listed;
    }
}
