package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.account.message.ImapMessageInfo;

@XmlAccessorType(XmlAccessType.NONE)
public class CreateItemNotification {

    public CreateItemNotification() {}

    public CreateItemNotification(ImapMessageInfo msgInfo) {
        setMessageInfo(msgInfo);
    }

    /**
     * @zm-api-field-description message info of created item
     */
    @XmlElement(name=MailConstants.E_MSG, /* m */ required=true)
    private ImapMessageInfo msgInfo;

    public void setMessageInfo(ImapMessageInfo msgInfo) { this.msgInfo = msgInfo; }
    public ImapMessageInfo getMessageInfo() { return msgInfo; }
}
