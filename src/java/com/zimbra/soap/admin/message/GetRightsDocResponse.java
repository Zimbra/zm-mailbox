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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.PackageRightsInfo;
import com.zimbra.soap.admin.type.DomainAdminRight;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_RIGHTS_DOC_RESPONSE)
public class GetRightsDocResponse {

    /**
     * @zm-api-field-description Information for packages
     */
    @XmlElement(name=AdminConstants.E_PACKAGE, required=false)
    private List <PackageRightsInfo> pkgs = Lists.newArrayList();

    /**
     * @zm-api-field-description Unused Admin rights
     */
    @XmlElement(name="notUsed", required=false)
    private List <String> notUsed = Lists.newArrayList();

    /**
     * @zm-api-field-description Domain admin rights
     */
    @XmlElementWrapper(
            name="domainAdmin-copypaste-to-zimbra-rights-domainadmin-xml-template",
            required=true)
    @XmlElement(name=AdminConstants.E_RIGHT, required=false)
    private List <DomainAdminRight> rights = Lists.newArrayList();

    public GetRightsDocResponse() {
    }

    public GetRightsDocResponse setPackages(Collection <PackageRightsInfo> pkgs) {
        this.pkgs.clear();
        if (pkgs != null) {
            this.pkgs.addAll(pkgs);
        }
        return this;
    }

    public GetRightsDocResponse addPackage(PackageRightsInfo pkg) {
        pkgs.add(pkg);
        return this;
    }

    public List<PackageRightsInfo> getPackages() {
        return Collections.unmodifiableList(pkgs);
    }

    public GetRightsDocResponse setRights(Collection <DomainAdminRight> rights) {
        this.rights.clear();
        if (rights != null) {
            this.rights.addAll(rights);
        }
        return this;
    }

    public GetRightsDocResponse addRight(DomainAdminRight right) {
        rights.add(right);
        return this;
    }

    public List <DomainAdminRight> getRights() {
        return Collections.unmodifiableList(rights);
    }
}
