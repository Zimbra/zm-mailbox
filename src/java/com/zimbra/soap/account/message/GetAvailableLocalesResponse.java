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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.LocaleInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_AVAILABLE_LOCALES_RESPONSE)
public class GetAvailableLocalesResponse {

    @XmlElement(name=AccountConstants.E_LOCALE, required=false)
    private List<LocaleInfo> locales = Lists.newArrayList();

    public GetAvailableLocalesResponse() {
    }

    public void setLocales(Iterable <LocaleInfo> locales) {
        this.locales.clear();
        if (locales != null) {
            Iterables.addAll(this.locales,locales);
        }
    }

    public GetAvailableLocalesResponse addLocal(LocaleInfo local) {
        this.locales.add(local);
        return this;
    }

    public List<LocaleInfo> getLocales() {
        return Collections.unmodifiableList(locales);
    }
}
