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
import com.zimbra.soap.admin.type.PackageSelector;

/**
 * @zm-api-command-description Get Rights Document
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_RIGHTS_DOC_REQUEST)
public class GetRightsDocRequest {

    /**
     * @zm-api-field-description Packages
     */
    @XmlElement(name=AdminConstants.E_PACKAGE, required=false)
    private List <PackageSelector> pkgs = Lists.newArrayList();

    public GetRightsDocRequest() {
    }

    public GetRightsDocRequest(Collection <PackageSelector> pkgs) {
        setPkgs(pkgs);
    }

    public GetRightsDocRequest setPkgs(Collection <PackageSelector> pkgs) {
        this.pkgs.clear();
        if (pkgs != null) {
            this.pkgs.addAll(pkgs);
        }
        return this;
    }

    public GetRightsDocRequest addPkg(PackageSelector pkg) {
        pkgs.add(pkg);
        return this;
    }

    public List<PackageSelector> getPkgs() {
        return Collections.unmodifiableList(pkgs);
    }
}
