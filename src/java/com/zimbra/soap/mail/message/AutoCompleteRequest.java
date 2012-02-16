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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.GalSearchType;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description AutoComplete
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_AUTO_COMPLETE_REQUEST)
public class AutoCompleteRequest {

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag gal-search-type
     * @zm-api-field-description GAL Search type - default value is <b>"account"</b>
     */
    @XmlAttribute(name=MailConstants.A_TYPE /* t */, required=false)
    private GalSearchType type;

    /**
     * @zm-api-field-tag need-exp
     * @zm-api-field-description Set if the "exp" flag is needed in the response for group entries.  Default is unset.
     */
    @XmlAttribute(name=MailConstants.A_NEED_EXP /* needExp */, required=false)
    private ZmBoolean needCanExpand;

    // Comma separated list of integers
    /**
     * @zm-api-field-tag comma-sep-folder-ids
     * @zm-api-field-description Comma separated list of folder IDs
     */
    @XmlAttribute(name=MailConstants.A_FOLDERS /* folders */, required=false)
    private String folderList;

    /**
     * @zm-api-field-tag include-GAL
     * @zm-api-field-description Flag whether to include Global Address Book (GAL)
     */
    @XmlAttribute(name=MailConstants.A_INCLUDE_GAL /* includeGal */, required=false)
    private ZmBoolean includeGal;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AutoCompleteRequest() {
        this((String) null);
    }

    public AutoCompleteRequest(String name) {
        this.name = name;
    }

    public void setType(GalSearchType type) { this.type = type; }
    public void setNeedCanExpand(Boolean needCanExpand) {
        this.needCanExpand = ZmBoolean.fromBool(needCanExpand);
    }
    public void setFolderList(String folderList) {
        this.folderList = folderList;
    }
    public void setIncludeGal(Boolean includeGal) {
        this.includeGal = ZmBoolean.fromBool(includeGal);
    }
    public String getName() { return name; }
    public GalSearchType getType() { return type; }
    public Boolean getNeedCanExpand() { return ZmBoolean.toBool(needCanExpand); }
    public String getFolderList() { return folderList; }
    public Boolean getIncludeGal() { return ZmBoolean.toBool(includeGal); }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("type", type)
            .add("needCanExpand", needCanExpand)
            .add("folderList", folderList)
            .add("includeGal", includeGal);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
