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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_MODIFY_ZIMLET_PREFS_RESPONSE)
@XmlType(propOrder = {})
public class ModifyZimletPrefsResponse {

    /**
     * @zm-api-field-description Zimlets
     */
    @XmlElement(name=AccountConstants.E_ZIMLET /* zimlet */, required=false)
    private List<String> zimlets = Lists.newArrayList();

    public ModifyZimletPrefsResponse() {
    }

    public void setZimlets(Iterable <String> zimlets) {
        this.zimlets.clear();
        if (zimlets != null) {
            Iterables.addAll(this.zimlets,zimlets);
        }
    }

    public void addZimlet(String zimlet) {
        this.zimlets.add(zimlet);
    }

    public List<String> getZimlets() {
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
