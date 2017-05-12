package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Return a list of subscribed folder names
 */
@XmlRootElement(name=MailConstants.E_LIST_IMAP_SUBSCRIPTIONS_REQUEST)
public class ListIMAPSubscriptionsRequest {
    public ListIMAPSubscriptionsRequest() {}
}