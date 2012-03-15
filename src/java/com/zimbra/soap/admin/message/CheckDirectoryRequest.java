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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CheckDirSelector;

/**
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
