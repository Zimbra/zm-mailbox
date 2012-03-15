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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.DistributionListBy;

@XmlAccessorType(XmlAccessType.NONE)
public class DistributionListSelector {

    /**
     * @zm-api-field-tag dl-selector-by
     * @zm-api-field-description Select the meaning of <b>{dl-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY, required=true)
    private final DistributionListBy dlBy;

    /**
     * @zm-api-field-tag dl-selector-key
     * @zm-api-field-description The key used to identify the account. Meaning determined by <b>{dl-selector-by}</b>
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

    public String getKey() { return key; }

    public DistributionListBy getBy() { return dlBy; }

    public static DistributionListSelector fromId(String id) {
        return new DistributionListSelector(DistributionListBy.id, id);
    }

    public static DistributionListSelector fromName(String name) {
        return new DistributionListSelector(DistributionListBy.name, name);
    }
}
