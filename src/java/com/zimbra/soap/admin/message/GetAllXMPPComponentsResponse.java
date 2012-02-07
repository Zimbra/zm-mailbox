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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.XMPPComponentInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_XMPPCOMPONENTS_RESPONSE)
public class GetAllXMPPComponentsResponse {

    /**
     * @zm-api-field-description Information on XMPP components
     */
    @XmlElement(name=AccountConstants.E_XMPP_COMPONENT, required=false)
    private List<XMPPComponentInfo> components = Lists.newArrayList();

    public GetAllXMPPComponentsResponse() {
    }

    public void setComponents(Iterable <XMPPComponentInfo> components) {
        this.components.clear();
        if (components != null) {
            Iterables.addAll(this.components,components);
        }
    }

    public GetAllXMPPComponentsResponse addComponent(
                            XMPPComponentInfo component) {
        this.components.add(component);
        return this;
    }

    public List<XMPPComponentInfo> getComponents() {
        return Collections.unmodifiableList(components);
    }
}
