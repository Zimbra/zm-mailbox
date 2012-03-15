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

package com.zimbra.soap.account.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.account.type.CalTZInfo;
import com.zimbra.soap.base.CalTZInfoInterface;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.CursorInfo;
import com.zimbra.soap.type.GalSearchType;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Perform an autocomplete for a name against the Global Address List
 * <p>
 * The number of entries in the response is limited by Account/COS attribute zimbraContactAutoCompleteMaxResults with
 * default value of 20.
 * </p>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_AUTO_COMPLETE_GAL_REQUEST)
public class AutoCompleteGalRequest {

    /**
     * @zm-api-field-tag need-can-expand
     * @zm-api-field-description flag whether the <b>{exp}</b> flag is needed in the response for group entries.<br />
     *     default is 0 (false)
     */
    @XmlAttribute(name=AccountConstants.A_NEED_EXP /* needExp */, required=false)
    private ZmBoolean needCanExpand;

    /**
     * @zm-api-field-description The name to test for autocompletion
     */
    @XmlAttribute(name=AccountConstants.E_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-description type of addresses to auto-complete on
     * <ul>
     * <li>     "account" for regular user accounts, aliases and distribution lists
     * <li>     "resource" for calendar resources
     * <li>     "group" for groups
     * <li>     "all" for combination of both types
     * </ul>
     * if omitted, defaults to "accounts"
     */
    @XmlAttribute(name=AccountConstants.A_TYPE /* type */, required=false)
    private GalSearchType type;

    // TODO: is this appropriate for AutoCompleteGal?
    /**
     * @zm-api-field-tag gal-account-id
     * @zm-api-field-description GAL Account ID
     */
    @XmlAttribute(name=AccountConstants.A_GAL_ACCOUNT_ID /* galAcctId */, required=false)
    private String galAccountId;

    /**
     * @zm-api-field-tag limit
     * @zm-api-field-description An integer specifying the maximum number of results to return
     */
    @XmlAttribute(name=MailConstants.A_QUERY_LIMIT /* limit */, required=false)
    private Integer limit;

    /**
     * no-argument constructor wanted by JAXB
     */
    private AutoCompleteGalRequest() {
        this((String) null);
    }

    private AutoCompleteGalRequest(String name) {
        this.name = name;
    }

    public static AutoCompleteGalRequest createForName(String name) {
        return new AutoCompleteGalRequest(name);
    }

    public void setNeedCanExpand(Boolean needCanExpand) { this.needCanExpand = ZmBoolean.fromBool(needCanExpand); }
    public void setName(String name) {this.name = name; }
    public void setType(GalSearchType type) { this.type = type; }
    public void setGalAccountId(String galAccountId) { this.galAccountId = galAccountId; }
    public void setLimit(Integer limit) { this.limit = limit; }

    public Boolean getNeedCanExpand() { return ZmBoolean.toBool(needCanExpand); }
    public String getName() { return name; }
    public GalSearchType getType() { return type; }
    public String getGalAccountId() { return galAccountId; }
    public Integer getLimit() { return limit; }

    // Not a JAXB related method
    public void setCalTz(CalTZInfoInterface calTz) { this.setCalTz((CalTZInfo)calTz); }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("type", type)
            .add("needCanExpand", needCanExpand)
            .add("galAccountId", galAccountId)
            .add("limit", limit);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
