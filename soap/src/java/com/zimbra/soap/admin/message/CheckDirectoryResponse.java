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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DirPathInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CHECK_DIRECTORY_RESPONSE)
public class CheckDirectoryResponse {

    /**
     * @zm-api-field-description Information for directories
     */
    @XmlElement(name=AdminConstants.E_DIRECTORY, required=false)
    private List <DirPathInfo> paths = Lists.newArrayList();

    public CheckDirectoryResponse() {
    }

    public CheckDirectoryResponse(Collection<DirPathInfo> paths) {
        setPaths(paths);
    }

    public CheckDirectoryResponse setPaths(Collection<DirPathInfo> paths) {
        this.paths.clear();
        if (paths != null) {
            this.paths.addAll(paths);
        }
        return this;
    }

    public CheckDirectoryResponse addPath(DirPathInfo path) {
        paths.add(path);
        return this;
    }

    public List<DirPathInfo> getPaths() {
        return Collections.unmodifiableList(paths);
    }
}
