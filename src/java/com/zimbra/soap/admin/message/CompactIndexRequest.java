package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MailboxByAccountIdSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description CompactIndex
 * <br />
 * <b>Access</b>: domain admin sufficient
 * <br />
 * note: this request is by default proxied to the account's home server
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_COMPACT_INDEX_REQUEST)
public class CompactIndexRequest {

    /**
     * @zm-api-field-tag "start|status"
     * @zm-api-field-description Action to perform
     * <table>
     * <tr> <td> <b>start</b> </td> <td> start compact indexing </td> </tr>
     * <tr> <td> <b>status</b> </td> <td> show compact indexing status </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.E_ACTION, required=false)
    private final String action;

    /**
     * @zm-api-field-description Mailbox
     */
    @XmlElement(name=AdminConstants.E_MAILBOX /* mbox */, required=true)
    private final MailboxByAccountIdSelector mbox;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CompactIndexRequest() {
        this((String)null, (MailboxByAccountIdSelector) null);
    }

    public CompactIndexRequest(String accountId) {
        this((String)null, new MailboxByAccountIdSelector(accountId));
    }

    public CompactIndexRequest(String action, MailboxByAccountIdSelector mbox) {
        this.action = action;
        this.mbox = mbox;
    }

    public String getAction() { return action; }
    public MailboxByAccountIdSelector getMbox() { return mbox; }

}
