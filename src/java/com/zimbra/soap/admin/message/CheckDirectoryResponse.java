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
