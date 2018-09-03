package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Return the count of recent items in the folder
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_IMAP_RECENT_CUTOFF_RESPONSE)
public class GetIMAPRecentCutoffResponse {

    /**
     * @zm-api-field-tag imap-recent-cutoff
     * @zm-api-field-description The last recorded assigned item ID in the enclosing
     * Mailbox the last time the folder was accessed via a read/write IMAP session.
     * <br />Note that this value is only updated on session closes
     */
    @XmlAttribute(name=MailConstants.A_IMAP_RECENT_CUTOFF /* cutoff */, required=true)
    private final int cutoff;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetIMAPRecentCutoffResponse() {
        this(0);
    }

    public GetIMAPRecentCutoffResponse(int recentCutoff) {
        this.cutoff = recentCutoff;
    }

    public int getCutoff() { return cutoff; }
}
