/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import java.util.Collection;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
public class AccountInfo extends AdminObjectInfo {

    @XmlAttribute(name=AccountConstants.A_IS_EXTERNAL, required=false)
    private final ZmBoolean isExternal;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountInfo() {
        this((String)null, (String)null, (Boolean)null, (Collection <Attr>)null);
    }

    public AccountInfo(String id, String name) {
        this(id, name, (Boolean)null, (Collection <Attr>)null);
    }

    public AccountInfo(String id, String name, Boolean isExternal) {
        this(id, name, isExternal, (Collection <Attr>)null);
    }

    public AccountInfo(String id, String name, Boolean isExternal, Collection <Attr> attrs) {
        super(id, name, attrs);
        this.isExternal = ZmBoolean.fromBool(isExternal);
    }

    public Boolean getIsExternal() { return ZmBoolean.toBool(isExternal); }
}
