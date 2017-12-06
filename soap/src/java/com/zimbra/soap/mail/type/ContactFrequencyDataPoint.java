package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ContactFrequencyDataPoint {

    public ContactFrequencyDataPoint() {}

    public ContactFrequencyDataPoint(String label, int value) {
        this.label = label;
        this.value = value;
    }
    @XmlAttribute(name=MailConstants.A_CONTACT_FREQUENCY_LABEL, required=true)
    private String label;

    @XmlAttribute(name=MailConstants.A_CONTACT_FREQUENCY_VALUE, required=true)
    private int value;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}
