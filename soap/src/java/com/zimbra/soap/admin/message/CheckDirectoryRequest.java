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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CheckDirSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Check existence of one or more directories and optionally create them.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CHECK_DIRECTORY_REQUEST)
public class CheckDirectoryRequest {

    /**
     * @zm-api-field-description Directories
     */
    @XmlElement(name=AdminConstants.E_DIRECTORY, required=false)
    private List <CheckDirSelector> paths = Lists.newArrayList();

    public CheckDirectoryRequest() {
    }

    public CheckDirectoryRequest(Collection<CheckDirSelector> paths) {
        setPaths(paths);
    }

    public CheckDirectoryRequest setPaths(Collection<CheckDirSelector> paths) {
        this.paths.clear();
        if (paths != null) {
            this.paths.addAll(paths);
        }
        return this;
    }

    public CheckDirectoryRequest addPath(CheckDirSelector path) {
        paths.add(path);
        return this;
    }

    public List<CheckDirSelector> getPaths() {
        return Collections.unmodifiableList(paths);
    }
}
