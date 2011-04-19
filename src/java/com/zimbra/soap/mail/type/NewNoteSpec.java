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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class NewNoteSpec {

    @XmlAttribute(name=MailConstants.A_FOLDER, required=true)
    private final String folder;

    @XmlAttribute(name=MailConstants.E_CONTENT, required=true)
    private final String content;

    @XmlAttribute(name=MailConstants.A_COLOR, required=false)
    private Byte color;

    @XmlAttribute(name=MailConstants.A_BOUNDS, required=false)
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

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("folder", folder)
            .add("content", content)
            .add("color", color)
            .add("bounds", bounds)
            .toString();
    }
}
