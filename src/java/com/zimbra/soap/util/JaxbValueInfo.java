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
