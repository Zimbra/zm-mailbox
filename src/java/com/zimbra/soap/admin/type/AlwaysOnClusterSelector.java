/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
public class AlwaysOnClusterSelector {

    @XmlEnum
    public enum AlwaysOnClusterBy {
        // case must match protocol
        id, name;

        public static AlwaysOnClusterBy fromString(String s) throws ServiceException {
            try {
                return AlwaysOnClusterBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    /**
     * @zm-api-field-tag alwaysOnCluster-key
     * @zm-api-field-description Key for choosing alwaysOnCluster
     * <table>
     * <tr> <td> <b>{server-by}="id"</b> </td> <td> Use cluster's zimbraId as the Key </td> </tr>
     * <tr> <td> <b>{server-by}="name"</b> </td> <td> Use name of the cluster as the Key </td> </tr>
     * </table>
     */
    @XmlValue private final String key;

    /**
     * @zm-api-field-tag alwaysOnCluster-key
     * @zm-api-field-description Selects the meaning of <b>{alwaysOnCluster-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY) private final AlwaysOnClusterBy clusterBy;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AlwaysOnClusterSelector() {
        this(null, null);
    }

    public AlwaysOnClusterSelector(AlwaysOnClusterBy by, String key) {
        this.clusterBy = by;
        this.key = key;
    }

    public String getKey() { return key; }
    public AlwaysOnClusterBy getBy() { return clusterBy; }

    public static AlwaysOnClusterSelector fromId(String id) {
        return new AlwaysOnClusterSelector(AlwaysOnClusterBy.id, id);
    }

    public static AlwaysOnClusterSelector fromName(String name) {
        return new AlwaysOnClusterSelector(AlwaysOnClusterBy.name, name);
    }
}
