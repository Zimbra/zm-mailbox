package com.zimbra.soap.mail.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ContactFrequencyData;

@XmlRootElement(name=MailConstants.E_GET_CONTACT_FREQUENCY_RESPONSE)
public class GetContactFrequencyResponse {

    public GetContactFrequencyResponse() {}

    @XmlElements({
        @XmlElement(name=MailConstants.E_CONTACT_FREQUENCY_DATA, type=ContactFrequencyData.class)
    })
    private List<ContactFrequencyData> frequencyGraphs = new ArrayList<>();

    public List<ContactFrequencyData> getFrequencyGraphs() { return frequencyGraphs; }
    public void addFrequencyGraph(ContactFrequencyData data) { this.frequencyGraphs.add(data); }
}
