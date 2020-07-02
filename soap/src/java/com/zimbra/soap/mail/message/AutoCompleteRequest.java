/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.GalSearchType;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
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
     * @zm-api-field-description type of addresses to auto-complete on
     * <ul>
     * <li>     "account" for regular user accounts, aliases and distribution lists
     * <li>     "resource" for calendar resources
     * <li>     "group" for groups
     * <li>     "all" for combination of all types
     * </ul>
     * if omitted, defaults to "account"
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("type", type)
            .add("needCanExpand", needCanExpand)
            .add("folderList", folderList)
            .add("includeGal", includeGal);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
