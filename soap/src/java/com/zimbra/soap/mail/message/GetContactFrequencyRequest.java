package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=MailConstants.E_GET_CONTACT_FREQUENCY_REQUEST)
public class GetContactFrequencyRequest {

    public GetContactFrequencyRequest() {}

    public GetContactFrequencyRequest(String email, String frequencyBy) {
        setEmail(email);
        setFrequencyBy(frequencyBy);
    }
    /**
     * @zm-api-field-description Email address of the contact to fetch contact frequency for
     */
    @XmlAttribute(name=MailConstants.A_EMAIL, required=true)
    private String contactEmail;

    /**
     * @zm-api-field-description Comma-separated list of frequency graphs to return.
     * Values are one or more of "day,week,month".
     */
    @XmlAttribute(name=MailConstants.A_CONTACT_FREQUENCY_BY, required=true)
    private String frequencyBy;

    public String getEmail() { return contactEmail; }
    public void setEmail(String email) { this.contactEmail = email; }

    public String getFrequencyBy() { return frequencyBy; }
    public void setFrequencyBy(String frequencyBy) { this.frequencyBy = frequencyBy; }

}
