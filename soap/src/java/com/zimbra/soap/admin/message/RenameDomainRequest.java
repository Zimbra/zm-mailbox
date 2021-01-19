package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.type.AttributeSelectorImpl;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Rename a domain
 */
@XmlRootElement(name = AdminConstants.E_RENAME_DOMAIN_REQUEST)
public class RenameDomainRequest extends AttributeSelectorImpl {

    /**
     * @zm-api-field-tag new-domain-name
     * @zm-api-field-description Name of new domain
     */
    @XmlAttribute(name = AdminConstants.E_NAME, required = true)
    private String name;

    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name = AdminConstants.E_DOMAIN, required = true)
    private DomainSelector domain;

    public RenameDomainRequest() {
        this(null, null);
    }

    public RenameDomainRequest(DomainSelector domain, String newName) {
        setDomain(domain);
    }

    void setDomain(DomainSelector domain) {
        this.domain = domain;
    }

    public DomainSelector getDomain() {
        return domain;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}