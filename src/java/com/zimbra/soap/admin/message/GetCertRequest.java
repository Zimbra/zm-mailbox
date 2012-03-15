/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.CertMgrConstants;

/**
 * @zm-api-command-description Get Certificate
 * <br />
 * Currently, GetCertRequest/Response only handle 2 types "staged" and "all".  May need to support other options in
 * the future
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=CertMgrConstants.E_GET_CERT_REQUEST)
public class GetCertRequest {

    /**
     * @zm-api-field-tag server-id
     * @zm-api-field-description The server's ID whose cert is to be got
     */
    @XmlAttribute(name=AdminConstants.A_SERVER /* server */, required=true)
    private final String server;

    /**
     * @zm-api-field-description Certificate type
     * <br />
     * staged - view the staged crt
     * <br />
     * other options (all, mta, ldap, mailboxd, proxy) are used to view the deployed crt
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    private final String type;

    /**
     * @zm-api-field-tag
     * @zm-api-field-description Required only when type is "staged".
     * <br />
     * Could be "self" (self-signed cert) or "comm" (commerical cert)
     */
    @XmlAttribute(name=CertMgrConstants.A_OPTION /* option */, required=false)
    private String option;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetCertRequest() {
        this((String) null, (String) null);
    }

    public GetCertRequest(String server, String type) {
        this.server = server;
        this.type = type;
    }

    public void setOption(String option) { this.option = option; }
    public String getServer() { return server; }
    public String getType() { return type; }
    public String getOption() { return option; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("server", server)
            .add("type", type)
            .add("option", option);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
