package com.zimbra.soap.mail.message;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ContactInfo;
import com.zimbra.soap.mail.type.IMAPItemInfo;
import com.zimbra.soap.mail.type.MessageInfo;
import com.zimbra.soap.type.SearchHit;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_IMAP_COPY_RESPONSE)
public class IMAPCopyResponse {
    /**
     * @zm-api-field-description new items
     */
    @XmlElements(
        @XmlElement(name=MailConstants.E_ITEM /* m */, type=IMAPItemInfo.class)
    )
    private final List<IMAPItemInfo> newItems = Lists.newArrayList();

    public IMAPCopyResponse() {        
    }

    public void setItems(Iterable <IMAPItemInfo> item) {
        this.newItems.clear();
        Iterables.addAll(this.newItems,item);
    }

    public IMAPCopyResponse addItem(IMAPItemInfo item) {
        this.newItems.add(item);
        return this;
    }

    public List<IMAPItemInfo> getItems() {
        return newItems;
    }
}
