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
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.ZimletConstants;
import com.zimbra.soap.base.ZimletServerExtensionInterface;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=ZimletConstants.ZIMLET_TAG_SERVER_EXTENSION)
public class ZimletServerExtension
implements ZimletServerExtensionInterface {

    /**
     * @zm-api-field-tag keyword
     * @zm-api-field-description Keyword
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_HAS_KEYWORD /* hasKeyword */, required=false)
    private String hasKeyword;

    /**
     * @zm-api-field-tag extension-class
     * @zm-api-field-description Extension class
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_EXTENSION_CLASS /* extensionClass */, required=false)
    private String extensionClass;

    /**
     * @zm-api-field-tag regex
     * @zm-api-field-description Regex
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_REGEX /* regex */, required=false)
    private String regex;

    public ZimletServerExtension() {
    }

    @Override
    public void setHasKeyword(String hasKeyword) {
        this.hasKeyword = hasKeyword;
    }
    @Override
    public void setExtensionClass(String extensionClass) {
        this.extensionClass = extensionClass;
    }
    @Override
    public void setRegex(String regex) { this.regex = regex; }
    @Override
    public String getHasKeyword() { return hasKeyword; }
    @Override
    public String getExtensionClass() { return extensionClass; }
    @Override
    public String getRegex() { return regex; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("hasKeyword", hasKeyword)
            .add("extensionClass", extensionClass)
            .add("regex", regex);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
