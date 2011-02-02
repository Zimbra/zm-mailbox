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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;
import com.zimbra.soap.admin.type.LimitedQuery;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_CHECK_GAL_CONFIG_REQUEST)
@XmlType(propOrder = {"query", "action"})
public class CheckGalConfigRequest extends AdminAttrsImpl {

    @XmlElement(name=AdminConstants.E_QUERY)
    private LimitedQuery query;
    @XmlElement(name=AdminConstants.E_ACTION)
    private String action;

    public CheckGalConfigRequest() {
        this((LimitedQuery)null, (String)null);
    }

    public CheckGalConfigRequest(LimitedQuery query, String action) {
        this.query = query;
        this.action = action;
    }

    public void setQuery(LimitedQuery query) { this.query = query; }

    public LimitedQuery getQuery() { return query; }
    public void setAction(String action) { this.action = action; }

    public String getAction() { return action; }
}
