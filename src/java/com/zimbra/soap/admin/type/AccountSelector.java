/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

@XmlAccessorType(XmlAccessType.FIELD)
public class AccountSelector {

    @XmlEnum
    public enum AccountBy {
        // case must match protocol
        name, id, foreignPrincipal, adminName, appAdminName, krb5Principal;
        
        public static AccountBy fromString(String s) throws ServiceException {
            try {
                return AccountBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    @XmlValue private final String key;
    @XmlAttribute(name=AdminConstants.A_BY) private final AccountBy accountBy;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountSelector() {
        this.accountBy = null;
        this.key = null;
    }

    public AccountSelector(AccountBy by, String key) {
        this.accountBy = by;
        this.key = key;
    }

    public String getKey() { return key; }

    public AccountBy getBy() { return accountBy; }
}
