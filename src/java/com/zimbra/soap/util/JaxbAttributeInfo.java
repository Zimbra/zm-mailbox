/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.util;

import java.lang.reflect.Type;
import javax.xml.bind.annotation.XmlAttribute;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

public class JaxbAttributeInfo {
    private static final Log LOG = ZimbraLog.soap;
    private String name;
    private boolean required;
    private String fieldName;
    private String stamp;
    private Class<?> atomClass;

    public JaxbAttributeInfo(JaxbInfo jaxbInfo, XmlAttribute annot, String fieldName, Type defaultGenericType) {
        this.required = annot.required();
        this.fieldName = fieldName;
        stamp = jaxbInfo.getStamp() + "[attr=" + name + "]:";
        name = annot.name();
        if ((name == null) || JaxbInfo.DEFAULT_MARKER.equals(name)) {
            name = fieldName;
        }
        if (name == null) {
            LOG.debug("%s Ignoring element with annotation '%s' unable to determine name", stamp, annot);
        }
        atomClass = jaxbInfo.classFromType(defaultGenericType);
        if (atomClass == null) {
            LOG.debug("%s Ignoring attribute with annotation '%s' unable to determine class", stamp, annot);
        }
    }

    public String getName() { return name; }
    public boolean isRequired() { return required; }
    public String getFieldName() { return fieldName; }
    public Class<?> getAtomClass() { return atomClass; }
}
