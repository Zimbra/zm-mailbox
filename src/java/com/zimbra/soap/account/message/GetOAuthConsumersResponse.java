package com.zimbra.soap.account.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_OAUTH_CONSUMERS_RESPONSE)
public class GetOAuthConsumersResponse {
    @XmlElement(name=AccountConstants.E_OAUTH_CONSUMER, required=false)
    private List<OAuthConsumer> consumers = new ArrayList<OAuthConsumer>();

    public void addConsumer(OAuthConsumer consumer) {
        consumers.add(consumer);
    }

    public List<OAuthConsumer> getConsumers() {return consumers; }
}
