package com.zimbra.soap.mail.message;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.RelatedContactsTarget;

@XmlRootElement(name=MailConstants.E_GET_RELATED_CONTACTS_REQUEST)
public class GetRelatedContactsRequest {

    /**
     * @zm-api-field-description The maximum number of results to return. Defaults to 10 if not specified; capped at 500.
     */
    @XmlAttribute(name=MailConstants.A_QUERY_LIMIT, required=false)
    private Integer limit;

    /**
     * @zm-api-field-description The type of contact affinity to use. If not specified,
     * the combined affinity will be used.
     */
    @XmlAttribute(name=MailConstants.A_REQUESTED_AFFINITY_FIELD, required=false)
    private String affinity;

    @XmlElements({
        @XmlElement(name=MailConstants.E_AFFINITY_TARGET, type=RelatedContactsTarget.class, required=true),
    })
    private List<RelatedContactsTarget> contacts;

    public GetRelatedContactsRequest() {}

    public List<RelatedContactsTarget> getTargets() { return contacts; }
    public void setTargets(List<RelatedContactsTarget> contacts) { this.contacts = contacts; }

    public String getRequestedAffinity() { return affinity; }
    public void setRequestedAffinity(String affinity) { this.affinity = affinity; }

    public Integer getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
