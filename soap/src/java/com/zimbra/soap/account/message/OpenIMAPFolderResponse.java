package com.zimbra.soap.account.message;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlRootElement(name=AccountConstants.E_OPEN_IMAP_FOLDER_RESPONSE)
public class OpenIMAPFolderResponse {

    public OpenIMAPFolderResponse() {}

    /**
     * @zm-api-field-description list of ImapMessageInfo elements
     */
    @XmlElementWrapper(name=MailConstants.E_FOLDER)
    @XmlElement(name=MailConstants.E_MSG, /* m */ required=true)
    private List<ImapMessageInfo> messages = new LinkedList<ImapMessageInfo>();

    /**
     * @zm-api-field-description whether there are more imap messages remaining in the folder
     */
    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=false)
    private ZmBoolean hasMore;

    /**
     * @zm-api-field-description Cursor to be used by the next request, if more results exist
     */
    @XmlElement(name=MailConstants.E_CURSOR /* cursor */, required=false)
    private ImapCursorInfo cursor;

    public void addImapMessageInfo(ImapMessageInfo info) { messages.add(info); }

    public List<ImapMessageInfo> getImapMessageInfo() { return messages; }

    public void setHasMore(Boolean bool) { hasMore = ZmBoolean.fromBool(bool);
    }
    public boolean getHasMore() {return ZmBoolean.toBool(hasMore, false); }

    public void setCursor(ImapCursorInfo cursor) { this.cursor = cursor; }

    public ImapCursorInfo getCursor() { return cursor; }
}
