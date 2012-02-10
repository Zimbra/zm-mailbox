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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class DistributionListSelector {
    @XmlEnum
    public enum DistributionListBy {
        // case must match protocol
        id, name;

        public static DistributionListBy fromString(String s) throws ServiceException {
            try {
                return DistributionListBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    /**
     * @zm-api-field-tag dl-selector-by
     * @zm-api-field-description Select the meaning of <b>{dl-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY)
    private final DistributionListBy dlBy;

    /**
     * @zm-api-field-tag dl-selector-key
     * @zm-api-field-description The key used to identify the distribution list.
     * Meaning determined by <b>{dl-selector-by}</b>
     */
    @XmlValue
    private final String key;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DistributionListSelector() {
        this.dlBy = null;
        this.key = null;
    }

    public DistributionListSelector(DistributionListBy by, String key) {
        this.dlBy = by;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public DistributionListBy getBy() {
        return dlBy;
    }

    public static DistributionListSelector fromId(String id) {
        return new DistributionListSelector(DistributionListBy.id, id);
    }

    public static DistributionListSelector fromName(String name) {
        return new DistributionListSelector(DistributionListBy.name, name);
    }
}
