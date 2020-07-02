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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.ZimletConstants;
import com.zimbra.soap.base.ZimletDesc;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class AdminZimletDesc implements ZimletDesc {

    // Turning schema validation on for WSDL clients using WSDL derived
    // from this class appears to hit problems.  Believe that @XmlAnyElement
    // and @XmlMixed are fundamentally incompatible with schema validation

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

    // See wiki.zimbra.com/wiki/Basic_Zimlet_Definition_Tags

    // ZimletConstants.ZIMLET_TAG_CONTENT_OBJECT will map to org.w3c.dom.Element
    // ZimletConstants.ZIMLET_TAG_PANEL_ITEM will map to org.w3c.dom.Element
    // ZimletConstants.ZIMLET_DISABLE_UI_UNDEPLOY will map to org.w3c.dom.Element

    /**
     * @zm-api-field-description Elements
     */
    @XmlElementRefs({
        @XmlElementRef(/* serverExtension */ type=ZimletServerExtension.class),
        @XmlElementRef(/* include */ type=AdminZimletInclude.class),
        @XmlElementRef(/* includeCSS */ type=AdminZimletIncludeCSS.class),
        @XmlElementRef(/* target */ type=AdminZimletTarget.class)
    })
    @XmlAnyElement
    private List<Object> elements = Lists.newArrayList();

    public AdminZimletDesc() {
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
    @Override
    public void setElements(Iterable <Object> elements) {
        if (this.elements == null) {
            this.elements = Lists.newArrayList();
        }
        this.elements.clear();
        if (elements != null) {
            Iterables.addAll(this.elements,elements);
        }
    }

    @Override
    public void addElement(Object element) {
        if (this.elements == null) {
            this.elements = Lists.newArrayList();
        }
        this.elements.add(element);
    }

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
    public List<Object> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("version", version)
            .add("description", description)
            .add("extension", extension)
            .add("target", target)
            .add("label", label)
            .add("elements", elements);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
