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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("folder", folder)
            .add("content", content)
            .add("color", color)
            .add("bounds", bounds);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
