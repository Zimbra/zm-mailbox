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
import com.zimbra.soap.type.NamedElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_AVAILABLE_CSV_FORMATS_RESPONSE)
public class GetAvailableCsvFormatsResponse {

    /**
     * @zm-api-field-description The known CSV formats that can be used for import and export of addressbook.
     */
    @XmlElement(name=AccountConstants.E_CSV, required=false)
    private List<NamedElement> csvFormats = Lists.newArrayList();

    public GetAvailableCsvFormatsResponse() {
    }

    public void setCsvFormats(Iterable <NamedElement> csvFormats) {
        this.csvFormats.clear();
        if (csvFormats != null) {
            Iterables.addAll(this.csvFormats,csvFormats);
        }
    }

    public GetAvailableCsvFormatsResponse addCsvFormat(NamedElement csvFormat) {
        this.csvFormats.add(csvFormat);
        return this;
    }

    public List<NamedElement> getCsvFormats() {
        return Collections.unmodifiableList(csvFormats);
    }
}
