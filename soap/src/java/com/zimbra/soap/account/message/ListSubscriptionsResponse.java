package com.zimbra.soap.account.message;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_LIST_SUBSCRIPTIONS_RESPONSE)
public class ListSubscriptionsResponse {

    /**
     * @zm-api-field-description Identities
     */
    @XmlElement(name=AccountConstants.E_SUBSCRIPTION)
    Set<String> subs = new HashSet<String>();

    public Set<String> getSubscriptions() {
        return Collections.unmodifiableSet(subs);
    }

    public void setSubscriptions(Iterable<String> subs) {
        this.subs.clear();
        if (subs != null) {
            Iterables.addAll(this.subs, subs);
        }
    }
}