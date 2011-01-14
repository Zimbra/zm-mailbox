/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.formatter;

import com.zimbra.common.soap.Element;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;

public class JsonFormatter extends XmlFormatter {

    @Override
    public FormatType getType() {
        return FormatType.JSON;
    }

    @Override
    Element.ElementFactory getFactory() {
        return Element.JSONElement.mFactory;
    }
}
