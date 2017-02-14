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
 * @zm-api-command-auth-required false
 * @zm-api-command-admin-auth-required false
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
