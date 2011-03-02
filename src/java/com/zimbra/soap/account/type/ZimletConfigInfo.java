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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class ZimletConfigInfo {
    // Borrowed from ZimletMeta
    // TODO: move to ZimbraCommon?
    public static final String ZIMLET_ATTR_VERSION         = "version";
    public static final String ZIMLET_ATTR_DESCRIPTION     = "description";
    public static final String ZIMLET_ATTR_NAME            = "name";
    public static final String ZIMLET_ATTR_EXTENSION       = "extension";

    public static final String ZIMLET_TAG_GLOBAL           = "global";
    public static final String ZIMLET_TAG_HOST             = "host";

    // ZimletMeta validate() checks these 4 attributes
    @XmlAttribute(name=ZIMLET_ATTR_NAME, required=false)
    private String name;
    @XmlAttribute(name=ZIMLET_ATTR_VERSION, required=false)
    private String version;
    @XmlAttribute(name=ZIMLET_ATTR_DESCRIPTION, required=false)
    private String description;
    @XmlAttribute(name=ZIMLET_ATTR_EXTENSION, required=false)
    private Boolean extension;

    // ZimletConfig validateElement() checks these
    // #### @XmlElementWrapper(name=ZIMLET_TAG_GLOBAL, required=false);
    // #### @XmlElement(name="property", required=false);
    // #### private List <ZimletConfigGlobalProperty> properties = Lists.newArrayList();
    // generic elements that have attribute "name" and value.  Element name not specified
    // #### @XmlElementWrapper(name=ZIMLET_TAG_HOST, required=false);
    // #### private List <ZimletConfigGlobalProperty> properties = Lists.newArrayList();

    public ZimletConfigInfo () {
    }
}
