package com.zimbra.soap.mail.message;

import java.util.Collections;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Save a list of folder names subscribed to via IMAP
 */
@XmlRootElement(name=MailConstants.E_SAVE_IMAP_SUBSCRIPTIONS_REQUEST)
public class SaveIMAPSubscriptionsRequest {
    /**
     * @zm-api-field-description list of folder paths subscribed via IMAP
     */
    @XmlElement(name=AccountConstants.E_SUBSCRIPTION, required=true)
    private final Set<String> subscriptions;

    public Set<String> getSubscriptions() {
        if (subscriptions == null) {
            return Collections.unmodifiableSet(Sets.newHashSet());
        }
        return Collections.unmodifiableSet(subscriptions);
    }

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    public SaveIMAPSubscriptionsRequest() {
        this(Sets.newHashSet());
    }

    public SaveIMAPSubscriptionsRequest(Set<String> subs) {
        subscriptions = subs;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("subscriptions", subscriptions)
            .toString();
    }
}