package com.zimbra.soap.mail.message;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.RelatedContactResult;

@XmlRootElement(name=MailConstants.E_GET_RELATED_CONTACTS_RESPONSE)
public class GetRelatedContactsResponse {

    @XmlElementWrapper(name=MailConstants.E_RELATED_CONTACTS)
    @XmlElement(name=MailConstants.E_RELATED_CONTACT, type=RelatedContactResult.class)
    private List<RelatedContactResult> relatedContacts = Lists.newArrayList();

    public List<RelatedContactResult> getRelatedContacts() {return relatedContacts; }
    public void addRelatedContact(RelatedContactResult result) { this.relatedContacts.add(result); }
}
