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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Lists;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_ALL_MAILBOXES_REQUEST)
@XmlType(propOrder = {})
public class GetAllMailboxesRequest {

    @XmlAttribute(name=AdminConstants.A_LIMIT, required=false)
    private Long limit;
    @XmlAttribute(name=AdminConstants.A_OFFSET, required=false)
    private Long offset;

    public GetAllMailboxesRequest() {
    }
    public void setLimit(Long limit) { this.limit = limit; }
    public void setOffset(Long offset) { this.offset = offset; }

    public Long getLimit() { return limit; }
    public Long getOffset() { return offset; }
}
