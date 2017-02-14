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

    /**
     * @zm-api-field-description Information about locales
     */
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
