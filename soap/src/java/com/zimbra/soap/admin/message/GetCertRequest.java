/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.CertMgrConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("server", server)
            .add("type", type)
            .add("option", option);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
