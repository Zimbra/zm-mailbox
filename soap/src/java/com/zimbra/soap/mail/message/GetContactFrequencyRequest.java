package com.zimbra.soap.mail.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ContactFrequencyGraphSpec;

@XmlRootElement(name=MailConstants.E_GET_CONTACT_FREQUENCY_REQUEST)
public class GetContactFrequencyRequest {
    /**
     * @zm-api-field-description Email address of the contact to fetch contact frequency for
     */
    @XmlAttribute(name=MailConstants.A_EMAIL, required=true)
    private String contactEmail;

    /**
     * @zm-api-field-description List of contact frequency graph specifications to return
     */
    @XmlElement(name=MailConstants.A_CONTACT_FREQUENCY_GRAPH_SPEC, type=ContactFrequencyGraphSpec.class, required=true)
    private List<ContactFrequencyGraphSpec> specs = new ArrayList<>();

    /**
     * @zm-api-field-description offset in minutes from UTC to user's current timezone.
     */
    @XmlAttribute(name=MailConstants.A_CONTACT_FREQUENCY_OFFSET_IN_MINUTES)
    private Integer offsetInMinutes;

    public GetContactFrequencyRequest() {}

    public GetContactFrequencyRequest(String email) {
        this(email, null);
    }

    public GetContactFrequencyRequest(String email, Integer offsetInMinutes) {
        setEmail(email);
        setOffsetInMinutes(offsetInMinutes);
    }

    public void addGraphSpec(ContactFrequencyGraphSpec graphSpec) { specs.add(graphSpec); }
    public List<ContactFrequencyGraphSpec> getGraphSpecs() { return specs; }

    public String getEmail() { return contactEmail; }
    public void setEmail(String email) { this.contactEmail = email; }

    public Integer getOffsetInMinutes() { return offsetInMinutes; }
    public void setOffsetInMinutes(Integer offsetInMinutes) { this.offsetInMinutes = offsetInMinutes; }
}
