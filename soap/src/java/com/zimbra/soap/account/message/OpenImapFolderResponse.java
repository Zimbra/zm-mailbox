package com.zimbra.soap.account.message;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=AccountConstants.E_OPEN_IMAP_FOLDER_RESPONSE)
public class OpenImapFolderResponse {

    public OpenImapFolderResponse() {}

    @XmlElement(name=MailConstants.E_MSG, required=true)
    private List<ImapMessageInfo> messages = new LinkedList<ImapMessageInfo>();

    public void addImapMessageInfo(ImapMessageInfo info) {
        messages.add(info);
    }

    public List<ImapMessageInfo> getImapMessageInfo() {
        return messages;
    }
}
