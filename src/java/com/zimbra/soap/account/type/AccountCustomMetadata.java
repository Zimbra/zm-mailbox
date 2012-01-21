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

package com.zimbra.soap.account.type;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CustomMetadataInterface;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_METADATA)
public class AccountCustomMetadata
extends AccountKeyValuePairs
implements CustomMetadataInterface {

    // Normally present but sometimes an empty element is created to show that
    // CustomMetadata info is present but there are no sections to report on.
    /**
     * @zm-api-field-tag metadata-section-key
     * @zm-api-field-description Metadata section key
     */
    @XmlAttribute(name=MailConstants.A_SECTION /* section */, required=false)
    private String section;

    public AccountCustomMetadata() {
    }

    @Override
    public void setSection(String section) { this.section = section; }
    @Override
    public String getSection() { return section; }

    public static List <AccountCustomMetadata> fromInterfaces(Iterable <CustomMetadataInterface> params) {
        if (params == null)
            return null;
        List <AccountCustomMetadata> newList = Lists.newArrayList();
        for (CustomMetadataInterface param : params) {
            newList.add((AccountCustomMetadata) param);
        }
        return newList;
    }

    public static List <CustomMetadataInterface> toInterfaces(Iterable <AccountCustomMetadata> params) {
        if (params == null)
            return null;
        List <CustomMetadataInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return Collections.unmodifiableList(newList);
    }

    @Override
    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("section", section);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
