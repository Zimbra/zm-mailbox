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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AdminZimletInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ADMIN_EXTENSION_ZIMLETS_RESPONSE)
@XmlType(propOrder = {})
public class GetAdminExtensionZimletsResponse {

    /**
     * @zm-api-field-description Information about Admin Extension Zimlets
     */
    @XmlElementWrapper(name=AccountConstants.E_ZIMLETS, required=true)
    @XmlElement(name=AccountConstants.E_ZIMLET /* zimlet */, required=false)
    private List<AdminZimletInfo> zimlets = Lists.newArrayList();

    public GetAdminExtensionZimletsResponse() {
    }

    public void setZimlets(Iterable <AdminZimletInfo> zimlets) {
        this.zimlets.clear();
        if (zimlets != null) {
            Iterables.addAll(this.zimlets,zimlets);
        }
    }

    public void addZimlet(AdminZimletInfo zimlet) {
        this.zimlets.add(zimlet);
    }

    public List<AdminZimletInfo> getZimlets() {
        return Collections.unmodifiableList(zimlets);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("zimlets", zimlets);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
