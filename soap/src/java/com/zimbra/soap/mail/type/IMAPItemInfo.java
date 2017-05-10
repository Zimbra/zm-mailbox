package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class IMAPItemInfo {
    /**
     * @zm-api-field-tag msg-id
     * @zm-api-field-description Message ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final int id;

    /**
     * @zm-api-field-tag imap-uid
     * @zm-api-field-description IMAP UID
     */
    @XmlAttribute(name=MailConstants.A_IMAP_UID /* i4uid */, required=true)
    private final Integer imapUid;
    @SuppressWarnings("unused")
    private IMAPItemInfo() {
        this(0, 0);
    }

    public IMAPItemInfo(int id, int imapUid) {
        this.id = id;
        this.imapUid = imapUid;
    }

    public int getId() {
        return id;
    }

    public int getImapUid() {
        return imapUid;
    }
}
