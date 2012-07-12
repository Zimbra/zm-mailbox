/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.UCServiceAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public class VoiceProviderInfo {

    /**
     * @zm-api-field-description UC Service attributes
     */
    @XmlElementWrapper(name=AdminConstants.E_ATTRS /* attrs */, required=true)
    @XmlElement(name=AdminConstants.E_A /* a */, required=false)
    private List<UCServiceAttribute> attrs = Lists.newArrayList();

    public VoiceProviderInfo() {
    }

    public void setAttrs(Iterable <UCServiceAttribute> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    public void addAttr(UCServiceAttribute attr) {
        this.attrs.add(attr);
    }

    public List<UCServiceAttribute> getAttrs() {
        return attrs;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("attrs", attrs);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
