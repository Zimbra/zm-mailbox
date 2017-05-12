package com.zimbra.soap.mail.message;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=MailConstants.E_LIST_IMAP_SUBSCRIPTIONS_RESPONSE)
public class ListIMAPSubscriptionsResponse {

    /**
     * @zm-api-field-description list of folder paths subscribed via IMAP
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