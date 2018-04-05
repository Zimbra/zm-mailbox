package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class CreateItemNotification {

    /**
     * @zm-api-field-description message info of created item
     */
    @XmlElement(name=MailConstants.E_MSG, /* m */ required=true)
    private ImapMessageInfo msgInfo;

    public CreateItemNotification() {}

    public CreateItemNotification(ImapMessageInfo msgInfo) {
        setMessageInfo(msgInfo);
    }

    public void setMessageInfo(ImapMessageInfo msgInfo) { this.msgInfo = msgInfo; }
    public ImapMessageInfo getMessageInfo() { return msgInfo; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("msgInfo", msgInfo)
                .toString();
    }
}
