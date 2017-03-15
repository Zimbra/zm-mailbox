package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Resets the mailbox's "recent message count" to 0.  A message is considered "recent" if:
     *     (a) it's not a draft or a sent message, and
     *     (b) it was added since the last write operation associated with any SOAP session.
 */

@XmlRootElement(name=AccountConstants.E_RESET_RECENT_MESSAGE_COUNT_REQUEST)
public class ResetRecentMessageCountRequest {
    public ResetRecentMessageCountRequest() {};
}
