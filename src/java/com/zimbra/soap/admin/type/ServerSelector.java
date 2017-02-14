/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ServerSelector {
    // TODO: Change com.zimbra.cs.account.Provisioning.ServerBy to use this
    @XmlEnum
    public enum ServerBy {
        // case must match protocol
        id, name, serviceHostname;

        public static ServerBy fromString(String s) throws ServiceException {
            try {
                return ServerBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    /**
     * @zm-api-field-tag server-key
     * @zm-api-field-description Key for choosing server
     * <table>
     * <tr> <td> <b>{server-by}="id"</b> </td> <td> Use server's zimbraId as the Key </td> </tr>
     * <tr> <td> <b>{server-by}="name"</b> </td> <td> Use name of the server as the Key </td> </tr>
     * <tr> <td> <b>{server-by}="serviceHostname"</b> </td>
     *               <td> Use server's value for attr <b>zimbraServiceHostname</b> as the Key </td> </tr>
     * </table>
     */
    @XmlValue private final String key;

    /**
     * @zm-api-field-tag server-by
     * @zm-api-field-description Selects the meaning of <b>{server-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY) private final ServerBy serverBy;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ServerSelector() {
        this(null, null);
    }

    public ServerSelector(ServerBy by, String key) {
        this.serverBy = by;
        this.key = key;
    }

    public String getKey() { return key; }
    public ServerBy getBy() { return serverBy; }

    public static ServerSelector fromId(String id) {
        return new ServerSelector(ServerBy.id, id);
    }

    public static ServerSelector fromName(String name) {
        return new ServerSelector(ServerBy.name, name);
    }
}
