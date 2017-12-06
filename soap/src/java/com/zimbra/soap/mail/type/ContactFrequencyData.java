package com.zimbra.soap.mail.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ContactFrequencyData {

    public ContactFrequencyData() {}

    public ContactFrequencyData(String frequencyBy, List<ContactFrequencyDataPoint> dataPoints) {
        setFrequencyBy(frequencyBy);
        setDataPoints(dataPoints);
    }

    @XmlAttribute(name=MailConstants.A_CONTACT_FREQUENCY_BY, required=true)
    private String frequencyBy;

    @XmlElement(name=MailConstants.E_CONTACT_FREQUENCY_DATA_POINT, type=ContactFrequencyDataPoint.class)
    private List<ContactFrequencyDataPoint> dataPoints;

    public String getFrequencyBy() { return frequencyBy; }
    public void setFrequencyBy(String frequencyBy) { this.frequencyBy = frequencyBy; }

    public List<ContactFrequencyDataPoint> getDataPoints() { return dataPoints; }
    public void setDataPoints(List<ContactFrequencyDataPoint> dataPoints) { this.dataPoints = dataPoints; }
}
