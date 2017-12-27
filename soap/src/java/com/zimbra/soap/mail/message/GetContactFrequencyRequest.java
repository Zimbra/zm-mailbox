package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=MailConstants.E_GET_CONTACT_FREQUENCY_REQUEST)
public class GetContactFrequencyRequest {

    public GetContactFrequencyRequest() {}

    public GetContactFrequencyRequest(String email, String frequencyBy) {
        this(email, frequencyBy, null);
    }

    public GetContactFrequencyRequest(String email, String frequencyBy, Integer offsetInMinutes) {
        setEmail(email);
        setFrequencyBy(frequencyBy);
        setOffsetInMinutes(offsetInMinutes);
    }
    /**
     * @zm-api-field-description Email address of the contact to fetch contact frequency for
     */
    @XmlAttribute(name=MailConstants.A_EMAIL, required=true)
    private String contactEmail;

    /**
     * @zm-api-field-description list of frequency graphs to return separated by space.
     * Values are one or more of "day,week,month".
     */
    @XmlAttribute(name=MailConstants.A_CONTACT_FREQUENCY_BY, required=true)
    private String frequencyBy;

    /**
     * @zm-pai-field-description offset in minutes of user's current timezone.
     */
    @XmlAttribute(name=MailConstants.A_CONTACT_FREQUENCY_OFFSET_IN_MINUTES)
    private Integer offsetInMinutes;

    public String getEmail() { return contactEmail; }
    public void setEmail(String email) { this.contactEmail = email; }

    public String getFrequencyBy() { return frequencyBy; }
    public void setFrequencyBy(String frequencyBy) { this.frequencyBy = frequencyBy; }

    public Integer getOffsetInMinutes() { return offsetInMinutes; }
    public void setOffsetInMinutes(Integer offsetInMinutes) { this.offsetInMinutes = offsetInMinutes; }
}
