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

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.RightInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_RIGHTS_RESPONSE)
public class GetAllRightsResponse {

    /**
     * @zm-api-field-description Information for rights
     */
    @XmlElement(name=AdminConstants.E_RIGHT, required=false)
    private List <RightInfo> rights = Lists.newArrayList();

    public GetAllRightsResponse() {
    }

    public GetAllRightsResponse(Collection <RightInfo> rights) {
        setRights(rights);
    }

    public GetAllRightsResponse setRights(Collection <RightInfo> rights) {
        this.rights.clear();
        if (rights != null) {
            this.rights.addAll(rights);
        }
        return this;
    }

    public GetAllRightsResponse addRight(RightInfo right) {
        rights.add(right);
        return this;
    }

    public List<RightInfo> getRights() {
        return Collections.unmodifiableList(rights);
    }
}
