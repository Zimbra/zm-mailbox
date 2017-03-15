package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

/**
 * Cursor info for OpenImapFolderRequest. Since the goal of OpenImapFolderRequest is to
 * retrieve all the items in a given folder, the sort direction is not a factor in this cursor,
 * like it is with the search cursor.
 * @author iraykin
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ImapCursorInfo {

    public ImapCursorInfo() {}

    public ImapCursorInfo(String id) {
        this.id = id;
    }

    /**
    * @zm-api-field-tag cursor-prev-id
    * @zm-api-field-description ID of the last IMAP item of the last page returned by OpenImapFolderRequest.
    * If this item is deleted, the cursor is cleared.
    */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private String id;

    public String getId() { return id; }
}
