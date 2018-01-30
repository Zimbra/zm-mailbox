package com.zimbra.soap.mail.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ContactFrequencyData {

    public ContactFrequencyData() {}

    public ContactFrequencyData(ContactFrequencyGraphSpec graphSpec, List<ContactFrequencyDataPoint> dataPoints) {
        setGraphSpec(graphSpec);
        setDataPoints(dataPoints);
    }

    @XmlElement(name=MailConstants.A_CONTACT_FREQUENCY_GRAPH_SPEC, required=true)
    private ContactFrequencyGraphSpec spec;

    @XmlElement(name=MailConstants.E_CONTACT_FREQUENCY_DATA_POINT, type=ContactFrequencyDataPoint.class)
    private List<ContactFrequencyDataPoint> dataPoints;

    public ContactFrequencyGraphSpec getGraphSpec() { return spec; }
    public void setGraphSpec(ContactFrequencyGraphSpec graphSpec) { this.spec = graphSpec; }

    public List<ContactFrequencyDataPoint> getDataPoints() { return dataPoints; }
    public void setDataPoints(List<ContactFrequencyDataPoint> dataPoints) { this.dataPoints = dataPoints; }
}
