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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-description Get attribute information
 * @zm-api-request-description Only one of <b>attrs</b> or <b>entryTypes</b> can be specified.
 * <br />
 * If both are specified, INVALID_REQUEST will be thrown.
 * <br />
 * If neither is specified, all attributes will be returned.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ATTRIBUTE_INFO_REQUEST)
public class GetAttributeInfoRequest {

    /**
     * @zm-api-field-tag attrs-to-return
     * @zm-api-field-description Comma separated list of attributes to return
     */
    @XmlAttribute(name=AdminConstants.A_ATTRS /* attrs */, required=false)
    private String attrs;

    // Comma Separated list
    /**
     * @zm-api-field-tag entry-types
     * @zm-api-field-description Comma separated list of entry types.  Attributes on the specified entry types will
     * be returned.
     * <br />
     * valid entry types:
     * <pre>
     *     account,alias,distributionList,cos,globalConfig,domain,server,mimeEntry,zimletEntry,
     *     calendarResource,identity,dataSource,pop3DataSource,imapDataSource,rssDataSource,
     *     liveDataSource,galDataSource,signature,xmppComponent,aclTarget
     * </pre>
     */
    @XmlAttribute(name=AdminConstants.A_ENTRY_TYPES /* entryTypes */, required=false)
    private String entryTypes;

    public GetAttributeInfoRequest() {
    }

    public void setAttrs(String attrs) { this.attrs = attrs; }
    public void setEntryTypes(String entryTypes) {
        this.entryTypes = entryTypes;
    }
    public String getAttrs() { return attrs; }
    public String getEntryTypes() { return entryTypes; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("attrs", attrs)
            .add("entryTypes", entryTypes);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
