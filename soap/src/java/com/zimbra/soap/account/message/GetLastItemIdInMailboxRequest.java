package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Returns the last ID assigned to an item successfully created in the mailbox
 */
@XmlRootElement(name=AccountConstants.E_GET_LAST_ITEM_ID_IN_MAILBOX_REQUEST)
public class GetLastItemIdInMailboxRequest {

    public GetLastItemIdInMailboxRequest() {}

}
