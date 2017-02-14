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
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.util.ZimbraLog;

public class JaxbValueInfo {
    private String fieldName;
    private Class<?> atomClass;

    public JaxbValueInfo(XmlValue annot, String fieldName, Type defaultGenericType) {
        this.fieldName = fieldName;
        atomClass = JaxbInfo.classFromType(defaultGenericType);
        if (atomClass == null) {
            ZimbraLog.soap.debug("Unable to determine class for value field %s with annotation '%s'", fieldName, annot);
        }
    }

    public String getFieldName() { return fieldName; }
 
    /**
     * @return the class associated with the value
     */
    public Class<?> getAtomClass() { return atomClass; }
}
