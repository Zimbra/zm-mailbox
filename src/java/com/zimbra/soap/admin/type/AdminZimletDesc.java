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

import com.google.common.base.Objects;
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
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.ZimletConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class AdminZimletDesc {

    // Turning schema validation on for WSDL clients using WSDL derived
    // from this class appears to hit problems.  Believe that @XmlAnyElement
    // and @XmlMixed are fundamentally incompatible with schema validation
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_NAME /* name */,
                    required=false)
    private String name;

    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_VERSION /* version */,
                    required=false)
    private String version;

    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_DESCRIPTION /* description */,
                    required=false)
    private String description;

    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_EXTENSION /* extension */,
                    required=false)
    private String extension;

    @XmlAttribute(name=ZimletConstants.ZIMLET_TAG_TARGET /* target */,
                    required=false)
    private String target;

    @XmlAttribute(name=ZimletConstants.ZIMLET_TAG_LABEL /* label */,
                    required=false)
    private String label;

    // See wiki.zimbra.com/wiki/Basic_Zimlet_Definition_Tags

    // ZimletConstants.ZIMLET_TAG_CONTENT_OBJECT will map to org.w3c.dom.Element
    // ZimletConstants.ZIMLET_TAG_PANEL_ITEM will map to org.w3c.dom.Element
    // ZimletConstants.ZIMLET_DISABLE_UI_UNDEPLOY will map to org.w3c.dom.Element

    @XmlElementRefs({
        @XmlElementRef(name=ZimletConstants.ZIMLET_TAG_SERVER_EXTENSION /* serverExtension */,
            type=ZimletServerExtension.class),
        @XmlElementRef(name=ZimletConstants.ZIMLET_TAG_SCRIPT /* include */,
            type=AdminZimletInclude.class),
        @XmlElementRef(name=ZimletConstants.ZIMLET_TAG_CSS /* includeCSS */,
            type=AdminZimletIncludeCSS.class),
        @XmlElementRef(name=ZimletConstants.ZIMLET_TAG_TARGET /* target */,
            type=AdminZimletTarget.class)
    })
    @XmlAnyElement
    private List<Object> elements = Lists.newArrayList();

    public AdminZimletDesc() {
    }

    public void setName(String name) { this.name = name; }
    public void setVersion(String version) { this.version = version; }
    public void setDescription(String description) { this.description = description; }
    public void setExtension(String extension) { this.extension = extension; }
    public void setTarget(String target) { this.target = target; }
    public void setLabel(String label) { this.label = label; }
    public void setElements(Iterable <Object> elements) {
        if (this.elements == null) {
            this.elements = Lists.newArrayList();
        }
        this.elements.clear();
        if (elements != null) {
            Iterables.addAll(this.elements,elements);
        }
    }

    public void addElement(Object element) {
        if (this.elements == null) {
            this.elements = Lists.newArrayList();
        }
        this.elements.add(element);
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getExtension() { return extension; }
    public String getTarget() { return target; }
    public String getLabel() { return label; }
    public List<Object> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
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
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
