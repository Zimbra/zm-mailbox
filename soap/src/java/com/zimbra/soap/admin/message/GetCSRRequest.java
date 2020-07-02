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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("server", server)
            .add("type", type);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
