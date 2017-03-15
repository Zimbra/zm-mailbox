package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ImapMessageInfo implements Comparable<ImapMessageInfo> {

    public ImapMessageInfo() {}

    /**
     * @zm-api-field-tag item-id
     * @zm-api-field-description ID for item
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private Integer id;

    /**
     * @zm-api-field-tag imap-uid
     * @zm-api-field-description IMAP UID
     */
    @XmlAttribute(name=MailConstants.A_IMAP_UID /* i4uid */, required=true)
    private Integer imapUid;

    /**
     * @zm-api-field-tag item-type
     * @zm-api-field-description Item type
     */
    @XmlAttribute(name=MailConstants.A_TYPE /* t */, required=true)
    private String type;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=true)
    private Integer flags;

    /**
     * @zm-api-field-tag tag-names
     * @zm-api-field-description Comma separated list of name of tags associated with this item
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=true)
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
