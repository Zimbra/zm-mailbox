package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=AccountConstants.E_OPEN_IMAP_FOLDER_REQUEST)
public class OpenImapFolderRequest {
    public OpenImapFolderRequest() {};

    public OpenImapFolderRequest(String folderId) {
        setFolderId(folderId);
    }

    /**
     * @zm-api-field-description The ID of the folder to open
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=true)
    private String folderId;

    /**
     * @zm-api-field-description The maximum number of results to return
     */
    @XmlAttribute(name=MailConstants.A_QUERY_LIMIT /* limit */, required=true)
    private int limit;

    /**
     * @zm-api-field-description Cursor specifying the last item on the previous results page
     */
    @XmlElement(name=MailConstants.E_CURSOR /* cursor */, required=false)
    private ImapCursorInfo cursor;

    public void setFolderId(String id) { this.folderId = id; }

    public String getFolderId() { return folderId; }

    public void setLimit(Integer limit) { this.limit = limit; }

    public int getLimit() { return limit; }

    public void setCursor(ImapCursorInfo cursor) { this.cursor = cursor; }

    public ImapCursorInfo getCursor() { return cursor; }
}
