/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
