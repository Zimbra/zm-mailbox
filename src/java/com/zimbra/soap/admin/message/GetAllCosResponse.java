/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_ALL_COS_RESPONSE)
@XmlType(propOrder = {})
public class GetAllCosResponse {

    @XmlElement(name=AdminConstants.E_COS)
    private List<CosInfo> cosList = new ArrayList <CosInfo> ();

    public GetAllCosResponse() {
    }

    public List<CosInfo> getCosList() {
        return Collections.unmodifiableList(cosList);
    }

    public GetAllCosResponse setCosList(Collection<CosInfo> cosList) {
        this.cosList.clear();
        if (cosList != null) {
            this.cosList.addAll(cosList);
        }
        return this;
    }

    public GetAllCosResponse addCos(CosInfo cos) {
        cosList.add(cos);
        return this;
    }
}
