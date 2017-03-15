package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ImapMessageInfo implements Comparable<ImapMessageInfo> {

    public ImapMessageInfo() {}

    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private Integer id;

    @XmlAttribute(name=MailConstants.A_IMAP_UID, required=true)
    private Integer imapUid;

    @XmlAttribute(name=MailConstants.A_TYPE, required=true)
    private String type;

    @XmlAttribute(name=MailConstants.A_FLAGS, required=true)
    private Integer flags;

    @XmlAttribute(name=MailConstants.A_TAGS, required=true)
    private String tags;

    public ImapMessageInfo(Integer id, Integer imapUid, String type, Integer flags, String tags) {
        this.id = id;
        this.imapUid = imapUid;
        this.type = type;
        this.flags = flags;
        this.tags = tags;
    }

    public Integer getId() { return id; }
    public Integer getImapUid() { return imapUid; }
    public String getType() { return type; }
    public Integer getFlags() { return flags; }
    public String getTags() { return tags; }

    @Override
    public int compareTo(ImapMessageInfo other) {
        if (id == other.id) {
            return 0;
        }
        return id < other.id ? -1 : 1;
    }
}
