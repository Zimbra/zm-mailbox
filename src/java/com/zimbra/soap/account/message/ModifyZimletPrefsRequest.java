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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.ModifyZimletPrefsSpec;

/**
 * @zm-api-command-description Modify Zimlet Preferences
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_MODIFY_ZIMLET_PREFS_REQUEST)
public class ModifyZimletPrefsRequest {

    /**
     * @zm-api-field-description Zimlet Preference Specifications
     */
    @XmlElement(name=AccountConstants.E_ZIMLET /* zimlet */, required=false)
    private List<ModifyZimletPrefsSpec> zimlets = Lists.newArrayList();

    public ModifyZimletPrefsRequest() {
    }

    public void setZimlets(Iterable <ModifyZimletPrefsSpec> zimlets) {
        this.zimlets.clear();
        if (zimlets != null) {
            Iterables.addAll(this.zimlets,zimlets);
        }
    }

    public void addZimlet(ModifyZimletPrefsSpec zimlet) {
        this.zimlets.add(zimlet);
    }

    public List<ModifyZimletPrefsSpec> getZimlets() {
        return zimlets;
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
