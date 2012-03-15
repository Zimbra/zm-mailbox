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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.InheritedFlaggedValue;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ADMIN_CONSOLE_UI_COMP_RESPONSE)
public class GetAdminConsoleUICompResponse {

    /**
     * @zm-api-field-description  zimbraAdminConsoleUIComponents values
     */
    @XmlElement(name=AdminConstants.E_A, required=false)
    private List<InheritedFlaggedValue> values = Lists.newArrayList();

    public GetAdminConsoleUICompResponse() {
    }

    public void setValues(Iterable <InheritedFlaggedValue> values) {
        this.values.clear();
        if (values != null) {
            Iterables.addAll(this.values,values);
        }
    }

    public GetAdminConsoleUICompResponse addValue(InheritedFlaggedValue value) {
        this.values.add(value);
        return this;
    }


    public List<InheritedFlaggedValue> getValues() {
        return Collections.unmodifiableList(values);
    }
}
