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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class ZimletInfo {

    // Borrowed from ZimletMeta
    // TODO: move to ZimbraCommon?
    public static final String ZIMLET_TAG_CONFIG           = "zimletConfig";

    @XmlElement(name=AccountConstants.E_ZIMLET_CONTEXT, required=true)
    private final ZimletContext zimletContext;
    @XmlElement(name=AccountConstants.E_ZIMLET, required=false)
    private final ZimletDesc zimlet;
    @XmlElement(name=ZIMLET_TAG_CONFIG, required=false)
    private final ZimletConfigInfo zimletConfig;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private ZimletInfo () {
        this((ZimletContext) null, (ZimletDesc) null,
                (ZimletConfigInfo) null);
    }

    public ZimletInfo (ZimletContext zimletContext,
            ZimletDesc zimlet, ZimletConfigInfo zimletConfig) {
        this.zimletContext = zimletContext;
        this.zimlet = zimlet;
        this.zimletConfig = zimletConfig;
    }

    public ZimletContext getZimletContext() { return zimletContext; }
    public ZimletDesc getZimlet() { return zimlet; }
    public ZimletConfigInfo getZimletConfig() { return zimletConfig; }
}
