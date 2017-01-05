package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class DeleteItemNotification {

    /**
     * @zm-api-field-description ID of deleted item
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private int id;

    /**
     * @zm-api-field-description type of deleted item
     */
    @XmlAttribute(name=MailConstants.A_TYPE /* t */, required=true)
    private String type;

    public DeleteItemNotification() {}

    public DeleteItemNotification(int id, String type) {
        this.id = id;
        this.type = type;
    }

    public void setId(int id) { this.id = id; }
    public int getId() { return id; }

    public void setType(String type) { this.type = type; }
    public String getType() { return type; }
}
