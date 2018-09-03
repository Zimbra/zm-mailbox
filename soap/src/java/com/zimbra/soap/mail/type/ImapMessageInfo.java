package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ImapMessageInfo extends IMAPItemInfo implements Comparable<ImapMessageInfo> {

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

    public ImapMessageInfo() {}

    public ImapMessageInfo(int id, int imapUid, String type, Integer flags, String tags) {
        super(id, imapUid);
        this.type = type;
        this.flags = flags;
        this.tags = tags;
    }

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

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        super.addToStringInfo(helper);
        return helper
                .add("type", type)
                .add("flags", flags)
                .add("tags", tags);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
