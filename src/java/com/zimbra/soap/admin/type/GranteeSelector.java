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
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
public class GranteeSelector {

    @XmlEnum
    public static enum GranteeBy {
        // case must match protocol
        id, name;

        public static GranteeBy fromString(String s) throws ServiceException {
            try {
                return GranteeBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    @XmlAttribute(name=AdminConstants.A_TYPE, required=false)
    private final GranteeInfo.GranteeType type;
    @XmlAttribute(name=AdminConstants.A_BY, required=false)
    private final GranteeBy by;
    @XmlAttribute(name=AdminConstants.A_SECRET, required=false)
    private final String secret;
    @XmlAttribute(name=AdminConstants.A_ALL, required=false)
    private final ZmBoolean all;
    @XmlValue
    private final String key;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GranteeSelector() {
        this((GranteeInfo.GranteeType) null, (GranteeBy) null, (String) null,
                (Boolean) null, (String) null);
    }

    public GranteeSelector(GranteeBy by, String key) {
        this((GranteeInfo.GranteeType) null, by, key,
                (Boolean) null, (String) null);
    }

    public GranteeSelector(GranteeInfo.GranteeType type,
            GranteeBy by, String key) {
        this(type, by, key, (Boolean) null, (String) null);
    }

    public GranteeSelector(GranteeInfo.GranteeType type,
            GranteeBy by, String key, String secret) {
        this(type, by, key, (Boolean) null, secret);
    }

    public GranteeSelector(GranteeInfo.GranteeType type,
            GranteeBy by, String key, Boolean all) {
        this(type, by, key, all, (String) null);
    }

    public GranteeSelector(GranteeInfo.GranteeType type,
            GranteeBy by, String key, Boolean all, String secret) {
        this.type = type;
        this.by = by;
        this.key = key;
        this.all = ZmBoolean.fromBool(all);
        this.secret = secret;
    }

    public GranteeInfo.GranteeType getType() { return type; }
    public GranteeBy getBy() { return by; }
    public String getKey() { return key; }
    public String getSecret() { return secret; }
    public Boolean getAll() { return ZmBoolean.toBool(all); }
}
