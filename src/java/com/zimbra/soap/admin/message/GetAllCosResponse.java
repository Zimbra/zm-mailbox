/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AnnotatedCosInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_COS_RESPONSE)
@XmlType(propOrder = {})
public class GetAllCosResponse {

    /**
     * @zm-api-field-description Information on Classes of Service (COS)
     */
    @XmlElement(name=AdminConstants.E_COS)
    private List<AnnotatedCosInfo> cosList = Lists.newArrayList();

    public GetAllCosResponse() {
    }

    public List<AnnotatedCosInfo> getCosList() {
        return Collections.unmodifiableList(cosList);
    }

    public GetAllCosResponse setCosList(Iterable<AnnotatedCosInfo> cosList) {
        this.cosList.clear();
        if (cosList != null) {
            Iterables.addAll(this.cosList,cosList);
        }
        return this;
    }

    public GetAllCosResponse addCos(AnnotatedCosInfo cos) {
        cosList.add(cos);
        return this;
    }
}
