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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class ZimletDesc {
    // Borrowed from ZimletMeta
    // TODO: move to ZimbraCommon?
    public static final String ZIMLET_ATTR_VERSION         = "version";
    public static final String ZIMLET_ATTR_DESCRIPTION     = "description";
    public static final String ZIMLET_ATTR_NAME            = "name";
    public static final String ZIMLET_ATTR_EXTENSION       = "extension";

    public static final String ZIMLET_TAG_CONTENT_OBJECT   = "contentObject";
    public static final String ZIMLET_TAG_PANEL_ITEM       = "panelItem";
    public static final String ZIMLET_TAG_SERVER_EXTENSION = "serverExtension";
    public static final String ZIMLET_TAG_SCRIPT           = "include";
    public static final String ZIMLET_TAG_CSS              = "includeCSS";
    public static final String ZIMLET_TAG_TARGET           = "target";
    public static final String ZIMLET_DISABLE_UI_UNDEPLOY   = "disableUIUndeploy";

    // ZimletMeta validate() checks these 4 attributes
    @XmlAttribute(name=ZIMLET_ATTR_NAME, required=false)
    private String name;
    @XmlAttribute(name=ZIMLET_ATTR_VERSION, required=false)
    private String version;
    @XmlAttribute(name=ZIMLET_ATTR_DESCRIPTION, required=false)
    private String description;
    @XmlAttribute(name=ZIMLET_ATTR_EXTENSION, required=false)
    private Boolean extension;

    // ZimletDescription validateElement() checks these
    // #### @XmlElement(name=ZIMLET_TAG_CONTENT_OBJECT, required=false)
    // #### private ZimletContentObject contentObject;
    // TODO: Looks like this should be zimletPanelItem instead of panelItem???
    // @XmlElement(name=ZIMLET_TAG_PANEL_ITEM, required=false);
    // private ZimletPanelItem panelItem;
    // #### @XmlElement(name="zimletPanelItem", required=false);
    // #### private ZimletPanelItem zimletPanelItem;
    // #### @XmlElement(name=ZIMLET_TAG_SERVER_EXTENSION, required=false);
    // #### private ZimletServerExtension serverExtension;
    @XmlElement(name=ZIMLET_TAG_SCRIPT, required=false)
    private String include;
    @XmlElement(name=ZIMLET_TAG_CSS, required=false)
    private String css;
    @XmlElement(name=ZIMLET_TAG_TARGET, required=false)
    private String target;
    @XmlElement(name=ZIMLET_DISABLE_UI_UNDEPLOY, required=false)
    private String disableUIUndeploy;

    // Seen in GetInfoRequest responses
    @XmlElement(name="handlerObject", required=false)
    private String handlerObject;
    // #### @XmlElementWrapper(name="userProperties", required=false)
    // #### @XmlElement(name="property", required=false)
    // #### private List <ZimletUserProperty> properties = Lists.newArrayList();

    public ZimletDesc () {
    }

    public void setName(String name) { this.name = name; }
    public void setVersion(String version) { this.version = version; }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setExtension(Boolean extension) { this.extension = extension; }
    public void setInclude(String include) { this.include = include; }
    public void setCss(String css) { this.css = css; }
    public void setTarget(String target) { this.target = target; }
    public void setDisableUIUndeploy(String disableUIUndeploy) { 
        this.disableUIUndeploy = disableUIUndeploy;
    }
    public void setHandlerObject(String handlerObject) { 
        this.handlerObject = handlerObject;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public Boolean getExtension() { return extension; }
    public String getInclude() { return include; }
    public String getCss() { return css; }
    public String getTarget() { return target; }
    public String getDisableUIUndeploy() { return disableUIUndeploy; }
    public String getHandlerObject() { return handlerObject; }
}
