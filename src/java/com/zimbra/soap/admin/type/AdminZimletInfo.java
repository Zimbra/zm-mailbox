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

package com.zimbra.soap.admin.type;

import org.w3c.dom.Element;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.ZimletConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"zimletContext", "zimletElement", "zimletHandlerConfig"})
public class AdminZimletInfo {

    @XmlElement(name=AccountConstants.E_ZIMLET_CONTEXT /* zimletContext */,
                    required=false)
    private AdminZimletContext zimletContext;

    @XmlElements({
        @XmlElement(name=ZimletConstants.ZIMLET_TAG_ZIMLET /* zimlet */,
            type=AdminZimletDesc.class),
        @XmlElement(name=ZimletConstants.ZIMLET_TAG_CONFIG /* zimletConfig */,
            type=AdminZimletConfigInfo.class)
    })
    private Object zimletElement;   // TODO: use an interface with shared methods

    @XmlAnyElement
    private Element zimletHandlerConfig;

    public AdminZimletInfo() {
    }

    public void setZimletContext(AdminZimletContext zimletContext) {
        this.zimletContext = zimletContext;
    }
    public void setZimletElement(Object zimletElement) {
        this.zimletElement = zimletElement;
    }
    public void setZimletHandlerConfig(Element zimletHandlerConfig) {
        this.zimletHandlerConfig = zimletHandlerConfig;
    }
    public AdminZimletContext getZimletContext() { return zimletContext; }
    public Object getZimletElement() { return zimletElement; }
    public Element getZimletHandlerConfig() { return zimletHandlerConfig; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("zimletContext", zimletContext)
            .add("zimletElement", zimletElement)
            .add("zimletHandlerConfig", zimletHandlerConfig);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
