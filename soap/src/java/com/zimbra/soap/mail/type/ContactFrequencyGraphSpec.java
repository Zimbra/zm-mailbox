package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ContactFrequencyGraphSpec {

    @XmlAttribute(name=MailConstants.A_CONTACT_FREQUENCY_GRAPH_RANGE, required=true)
    private String range;

    @XmlAttribute(name=MailConstants.A_CONTACT_FREQUENCY_GRAPH_INTERVAL, required=true)
    private String interval;

    public ContactFrequencyGraphSpec(String range, String interval) {
        this.range = range;
        this.interval = interval;
    }

    public String getRange() { return range; }
    public void setRange(String range) { this.range = range; }

    public String getInterval() { return interval; }
    public void setInterval(String interval) { this.interval = interval; }
}
