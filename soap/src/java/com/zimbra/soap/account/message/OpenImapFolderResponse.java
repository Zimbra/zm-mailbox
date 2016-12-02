package com.zimbra.soap.account.message;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlRootElement(name=AccountConstants.E_OPEN_IMAP_FOLDER_RESPONSE)
public class OpenImapFolderResponse {

    public OpenImapFolderResponse() {}

    /**
     * @zm-api-field-description list of ImapMessageInfo elements
     */
    @XmlElement(name=MailConstants.E_MSG, /* msg */ required=true)
    private List<ImapMessageInfo> messages = new LinkedList<ImapMessageInfo>();

    /**
     * @zm-api-field-description whether there are more imap messages remaining in the folder
     */
    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=false)
    private ZmBoolean hasMore;

    public void addImapMessageInfo(ImapMessageInfo info) {
        messages.add(info);
    }

    public List<ImapMessageInfo> getImapMessageInfo() {
        return messages;
    }

    public void setHasMore(Boolean bool) {
        hasMore = ZmBoolean.fromBool(bool);
    }
    public boolean getHasMore() {
        return ZmBoolean.toBool(hasMore, false);
    }
}
