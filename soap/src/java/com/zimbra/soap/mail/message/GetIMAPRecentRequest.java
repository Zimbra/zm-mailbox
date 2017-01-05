package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Return the count of recent items in the specified folder
 */

@XmlRootElement(name=MailConstants.E_GET_IMAP_RECENT_REQUEST)
public class GetIMAPRecentRequest {

	/**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetIMAPRecentRequest() {
        this((String) null);
    }

    public GetIMAPRecentRequest(String id) {
        this.id = id;
    }

    public String getId() { return id; }

}
