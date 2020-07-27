package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_RENAME_DOMAIN_RESPONSE)
public class RenameDomainResponse {
    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    public RenameDomainResponse() {
        
    }

}
