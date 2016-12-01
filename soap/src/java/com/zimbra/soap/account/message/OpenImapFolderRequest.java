package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=AccountConstants.E_OPEN_IMAP_FOLDER_REQUEST)
public class OpenImapFolderRequest {
    public OpenImapFolderRequest() {};

    public OpenImapFolderRequest(String folderId) {
        setFolderId(folderId);
    }

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=true)
    private String folderId;

    public void setFolderId(String id) {
        this.folderId = id;
    }

    public String getFolderId() {
        return folderId;
    }

}
