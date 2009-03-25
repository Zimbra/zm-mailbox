/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.mime;

public class ContentDisposition extends MimeCompoundHeader {
    private static final String ATTACHMENT = "attachment";
    private static final String INLINE = "inline";

    public ContentDisposition(String header)                   { super(header);  normalizeValue(); }
    public ContentDisposition(String header, boolean use2231)  { super(header, use2231);  normalizeValue(); }
    public ContentDisposition(ContentDisposition cdisp)        { super(cdisp);  normalizeValue(); }

    public ContentDisposition setValue(String value)                   { super.setValue(value);  normalizeValue();  return this; }
    public ContentDisposition setParameter(String name, String value)  { super.setParameter(name, value);  return this; }

    private void normalizeValue() {
        String value = getValue();
        if (value == null || value.trim().equals("")) {
            setValue(ATTACHMENT);
        } else {
            if (!value.equals(value.trim().toLowerCase()))
                setValue(value.trim().toLowerCase());
            else if (!value.equals(ATTACHMENT) && !value.equals(INLINE))
                setValue(ATTACHMENT);
        }
    }

    public String toString()  { return toString(21); }
}