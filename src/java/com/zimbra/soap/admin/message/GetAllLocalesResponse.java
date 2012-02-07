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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.admin.type.LocaleInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_LOCALES_RESPONSE)
@XmlType(propOrder = {})
public class GetAllLocalesResponse {

    /**
     * @zm-api-field-description Information for system locales
     */
    @XmlElement(name=AccountConstants.E_LOCALE, required=false)
    private List <LocaleInfo> locales = Lists.newArrayList();

    public GetAllLocalesResponse() {
    }

    public GetAllLocalesResponse setLocales(Collection<LocaleInfo> locales) {
        this.locales.clear();
        if (locales != null) {
            this.locales.addAll(locales);
        }
        return this;
    }

    public GetAllLocalesResponse addLocale(LocaleInfo locale) {
        locales.add(locale);
        return this;
    }

    public List<LocaleInfo> getLocales() {
        return Collections.unmodifiableList(locales);
    }
}
