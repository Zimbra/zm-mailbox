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
 * @zm-api-command-description Get a certificate signing request (CSR)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=CertMgrConstants.E_GET_CSR_REQUEST)
public class GetCSRRequest {

    /**
     * @zm-api-field-tag server-id
     * @zm-api-field-description Server ID.  Can be "--- All Servers ---" or the ID of a server
     */
    @XmlAttribute(name=AdminConstants.A_SERVER /* server */, required=false)
    private String server;

    /**
     * @zm-api-field-tag type
     * @zm-api-field-description Type of CSR (required)
     * <table>
     * <tr> <td> <b>self</b> </td> <td> self-signed certificate </td> </tr>
     * <tr> <td> <b>comm</b> </td> <td> commercial certificate </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=false)
    private String type;

    public GetCSRRequest() {
    }

    private GetCSRRequest(String server, String type) {
        setServer(server);
        setType(type);
    }

    public static GetCSRRequest createForServerAndType(String server, String type) {
        return new GetCSRRequest(server, type);
    }

    public void setServer(String server) { this.server = server; }
    public void setType(String type) { this.type = type; }
    public String getServer() { return server; }
    public String getType() { return type; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("server", server)
            .add("type", type);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
