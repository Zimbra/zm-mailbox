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
import com.zimbra.soap.type.DistributionListGranteeType;

@XmlAccessorType(XmlAccessType.FIELD)
public class DistributionListGranteeSelector {

    @XmlAttribute(name=AccountConstants.A_TYPE, required=true)
    private final DistributionListGranteeType type;
    @XmlAttribute(name=AccountConstants.A_BY, required=true)
    private final DistributionListGranteeBy by;
    @XmlValue
    private final String key;
    
    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DistributionListGranteeSelector() {
        this((DistributionListGranteeType) null, 
                (DistributionListGranteeBy) null, (String) null);
    }

    public DistributionListGranteeSelector(DistributionListGranteeType type,
            DistributionListGranteeBy by, String key) {
        this.type = type;
        this.by = by;
        this.key = key;
    }

    public DistributionListGranteeType getType() { return type; }
    public DistributionListGranteeBy getBy() { return by; }
    public String getKey() { return key; }

}
