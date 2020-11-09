package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.ServerSelector;

/**
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Fetch all users from the remote zimbra system.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_FETCH_ALL_REMOTE_ACCOUNTS_REQUEST)
public class FetchAllRemoteAccountsRequest {

    /**
     * @zm-api-field-description Server
     */
    @XmlElement(name=AdminConstants.E_SERVER, required=false)
    private ServerSelector server;

    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
    private DomainSelector domain;

    public FetchAllRemoteAccountsRequest() {
        this(null, null);
    }

    public FetchAllRemoteAccountsRequest(ServerSelector server, DomainSelector domain) {
        setServer(server);
        setDomain(domain);
    }

    public void setServer(ServerSelector server) {
        this.server = server;
    }

    public void setDomain(DomainSelector domain) {
        this.domain = domain;
    }

    public ServerSelector getServer() { return server; }
    public DomainSelector getDomain() { return domain; }
}
