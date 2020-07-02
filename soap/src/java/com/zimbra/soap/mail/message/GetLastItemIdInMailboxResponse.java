package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=MailConstants.E_GET_LAST_ITEM_ID_IN_MAILBOX_RESPONSE)
public class GetLastItemIdInMailboxResponse {
    /**
     * @zm-api-field-description ID of last item created in mailbox
     */
    @XmlElement(name=MailConstants.A_ID /* id */, required=true)
    private int id;

    public GetLastItemIdInMailboxResponse() {
        this(0);
    }

    public GetLastItemIdInMailboxResponse(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add(MailConstants.A_ID, id);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
