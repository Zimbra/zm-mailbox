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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
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
public class AdminZimletConfigInfo
implements ZimletConfigInfo {

    /**
     * @zm-api-field-tag zimlet-name
     * @zm-api-field-description Zimlet name
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag version-string
     * @zm-api-field-description Version string
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
     * @zm-api-field-tag is-extension
     * @zm-api-field-description Valid values <b>true|false</b>
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
     * @zm-api-field-description Zimlet global configuration information
     */
    @XmlElement(name=ZimletConstants.ZIMLET_TAG_GLOBAL /* global */, required=false)
    private AdminZimletGlobalConfigInfo global;

    /**
     * @zm-api-field-description Zimlet host configuration information
     */
    @XmlElement(name=ZimletConstants.ZIMLET_TAG_HOST /* host */, required=false)
    private AdminZimletHostConfigInfo host;

    public AdminZimletConfigInfo() {
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
    public void setGlobal(AdminZimletGlobalConfigInfo global) { this.global = global; }
    public void setHost(AdminZimletHostConfigInfo host) { this.host = host; }
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
    public AdminZimletGlobalConfigInfo getGlobal() { return global; }
    @Override
    public AdminZimletHostConfigInfo getHost() { return host; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }

    @Override
    public void setGlobal(ZimletGlobalConfigInfo global) {
        setGlobal((AdminZimletGlobalConfigInfo) global);
    }

    @Override
    public void setHost(ZimletHostConfigInfo host) {
        setHost((AdminZimletHostConfigInfo) host);
    }
}
