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
