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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.DistributionListGranteeBy;
import com.zimbra.soap.type.GranteeType;

@XmlAccessorType(XmlAccessType.NONE)
public class DistributionListGranteeSelector {

    /**
     * @zm-api-field-tag dl-grantee-type
     * @zm-api-field-description Grantee type
     */
    @XmlAttribute(name=AccountConstants.A_TYPE, required=true)
    private final GranteeType type;

    /**
     * @zm-api-field-tag dl-grantee-by
     * @zm-api-field-description Selects meaning of <b>{dl-grantee-key}</b>
     */
    @XmlAttribute(name=AccountConstants.A_BY, required=true)
    private final DistributionListGranteeBy by;

    /**
     * @zm-api-field-tag dl-grantee-key
     * @zm-api-field-description Value of selector.  Meaning depends on value of <b>{dl-grantee-by}</b>
     */
    @XmlValue
    private final String key;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DistributionListGranteeSelector() {
        this((GranteeType) null,
                (DistributionListGranteeBy) null, (String) null);
    }

    public DistributionListGranteeSelector(GranteeType type, DistributionListGranteeBy by, String key) {
        this.type = type;
        this.by = by;
        this.key = key;
    }

    public GranteeType getType() { return type; }
    public DistributionListGranteeBy getBy() { return by; }
    public String getKey() { return key; }

}
