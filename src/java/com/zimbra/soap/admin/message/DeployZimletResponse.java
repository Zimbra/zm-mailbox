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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ZimletDeploymentStatus;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_DEPLOY_ZIMLET_RESPONSE)
public class DeployZimletResponse {

    @XmlElement(name=AdminConstants.E_PROGRESS, required=false)
    private List<ZimletDeploymentStatus> progresses = Lists.newArrayList();

    public DeployZimletResponse() {
    }

    public void setProgresses(Iterable <ZimletDeploymentStatus> progresses) {
        this.progresses.clear();
        if (progresses != null) {
            Iterables.addAll(this.progresses,progresses);
        }
    }

    public DeployZimletResponse addProgress(ZimletDeploymentStatus progress) {
        this.progresses.add(progress);
        return this;
    }

    public List<ZimletDeploymentStatus> getProgresses() {
        return Collections.unmodifiableList(progresses);
    }
}
