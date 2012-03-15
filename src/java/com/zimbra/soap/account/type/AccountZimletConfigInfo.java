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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.ZimletConstants;
import com.zimbra.soap.base.ZimletConfigInfo;
import com.zimbra.soap.base.ZimletGlobalConfigInfo;
import com.zimbra.soap.base.ZimletHostConfigInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class AccountZimletConfigInfo
implements ZimletConfigInfo {

    // ZimletMeta validate() checks:
    //     ZIMLET_ATTR_NAME, ZIMLET_ATTR_VERSION, ZIMLET_ATTR_DESCRIPTION, ZIMLET_ATTR_EXTENSION
    // ZimletConfig validateElement() checks: ZIMLET_TAG_GLOBAL, ZIMLET_TAG_HOST

    /**
     * @zm-api-field-tag zimlet-name
     * @zm-api-field-description Zimlet Name
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag zimlet-version
     * @zm-api-field-description Zimlet Version
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_VERSION /* version */, required=false)
    private String version;

    /**
     * @zm-api-field-tag zimlet-description
     * @zm-api-field-description Zimlet description
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_DESCRIPTION /* description */, required=false)
    private String description;

    /**
     * @zm-api-field-tag zimlet-extension
     * @zm-api-field-description Zimlet extension
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_EXTENSION /* extension */, required=false)
    private String extension;

    /**
     * @zm-api-field-tag zimlet-target
     * @zm-api-field-description Zimlet target
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_TAG_TARGET /* target */, required=false)
    private String target;

    /**
     * @zm-api-field-tag zimlet-label
     * @zm-api-field-description Zimlet label
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_TAG_LABEL /* label */, required=false)
    private String label;

    /**
     * @zm-api-field-description Zimlet Global configuration information
     */
    @XmlElement(name=ZimletConstants.ZIMLET_TAG_GLOBAL /* global */, required=false)
    private AccountZimletGlobalConfigInfo global;

    /**
     * @zm-api-field-description Zimlet host specific configuration information
     */
    @XmlElement(name=ZimletConstants.ZIMLET_TAG_HOST /* host */, required=false)
    private AccountZimletHostConfigInfo host;

    public AccountZimletConfigInfo() {
    }

    @Override
    public void setName(String name) { this.name = name; }
    @Override
    public void setVersion(String version) { this.version = version; }
    @Override
    public void setDescription(String description) { this.description = description; }
    @Override
    public void setExtension(String extension) { this.extension = extension; }
    @Override
    public void setTarget(String target) { this.target = target; }
    @Override
    public void setLabel(String label) { this.label = label; }
    public void setGlobal(AccountZimletGlobalConfigInfo global) { this.global = global; }
    public void setHost(AccountZimletHostConfigInfo host) { this.host = host; }
    @Override
    public String getName() { return name; }
    @Override
    public String getVersion() { return version; }
    @Override
    public String getDescription() { return description; }
    @Override
    public String getExtension() { return extension; }
    @Override
    public String getTarget() { return target; }
    @Override
    public String getLabel() { return label; }
    @Override
    public AccountZimletGlobalConfigInfo getGlobal() { return global; }
    @Override
    public AccountZimletHostConfigInfo getHost() { return host; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("version", version)
            .add("description", description)
            .add("extension", extension)
            .add("target", target)
            .add("label", label)
            .add("global", global)
            .add("host", host);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }

    @Override
    public void setGlobal(ZimletGlobalConfigInfo global) {
        setGlobal((AccountZimletGlobalConfigInfo) global);
    }

    @Override
    public void setHost(ZimletHostConfigInfo host) {
        setHost((AccountZimletHostConfigInfo) host);
    }
}
