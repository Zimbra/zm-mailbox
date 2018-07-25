package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=MailConstants.E_RECORD_IMAP_SESSION_RESPONSE)
public class RecordIMAPSessionResponse {
    /**
     * @zm-api-field-description ID of last item created in mailbox
     */
    @XmlElement(name=MailConstants.A_ID /* id */, required=true)
    private int lastItemId;

    /**
     * @zm-api-field-description UUID of the affected Folder
     */
    @XmlElement(name=MailConstants.A_FOLDER_UUID /* luuid */, required=true)
    private String folderUuid;

    public RecordIMAPSessionResponse() { }

    public RecordIMAPSessionResponse(int lastItemId, String folderUuid) {
        setLastItemId(lastItemId);
        setFolderUuid(folderUuid);
    }

    public String getFolderUuid() {
        return folderUuid;
    }

    public int getLastItemId() {
        return lastItemId;
    }

    public void setFolderUuid (String folderUuid) {
        this.folderUuid = folderUuid;
    }

    public void setLastItemId(int lastItemId) {
        this.lastItemId = lastItemId;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
                .add(MailConstants.A_ID, getLastItemId())
                .add(MailConstants.A_FOLDER_UUID, getFolderUuid());

    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
