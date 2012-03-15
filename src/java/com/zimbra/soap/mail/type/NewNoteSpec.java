/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class NewNoteSpec {

    /**
     * @zm-api-field-tag parent-folder-id
     * @zm-api-field-description Parent Folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=true)
    private final String folder;

    /**
     * @zm-api-field-tag content
     * @zm-api-field-description Content
     */
    @XmlAttribute(name=MailConstants.E_CONTENT /* content */, required=true)
    private final String content;

    /**
     * @zm-api-field-tag color
     * @zm-api-field-description color numeric; range 0-127; defaults to 0 if not present; client can display only 0-7
     */
    @XmlAttribute(name=MailConstants.A_COLOR /* color */, required=false)
    private Byte color;

    /**
     * @zm-api-field-tag bounds-x,y[width,height]
     * @zm-api-field-description Bounds - <b>x,y[width,height]</b> where x,y,width and height are all integers
     */
    @XmlAttribute(name=MailConstants.A_BOUNDS /* pos */, required=false)
    private String bounds;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private NewNoteSpec() {
        this((String) null, (String) null);
    }

    public NewNoteSpec(String folder, String content) {
        this.folder = folder;
        this.content = content;
    }

    public void setColor(Byte color) { this.color = color; }
    public void setBounds(String bounds) { this.bounds = bounds; }
    public String getFolder() { return folder; }
    public String getContent() { return content; }
    public Byte getColor() { return color; }
    public String getBounds() { return bounds; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("folder", folder)
            .add("content", content)
            .add("color", color)
            .add("bounds", bounds);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
