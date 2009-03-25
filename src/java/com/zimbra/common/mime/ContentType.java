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

public class ContentType extends MimeCompoundHeader {
    private String mPrimaryType, mSubType;
    private final String mDefault;

    public static final String TEXT_PLAIN = "text/plain";
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String MESSAGE_RFC822 = "message/rfc822";
    public static final String DEFAULT = TEXT_PLAIN;

    public ContentType(String header)                   { super(header);  mDefault = DEFAULT;  normalizeValue(); }
    public ContentType(String header, String def)       { super(header);  mDefault = def;  normalizeValue(); }
    public ContentType(String header, boolean use2231)  { super(header, use2231);  mDefault = DEFAULT;  normalizeValue(); }
    public ContentType(ContentType ctype)               { super(ctype);  mDefault = ctype == null ? DEFAULT : ctype.mDefault;  normalizeValue(); }

    public ContentType setSubType(String subtype)                         { super.setValue(mPrimaryType + '/' + subtype);  normalizeValue();  return this; }
    @Override public ContentType setValue(String value)                   { super.setValue(value);  normalizeValue();  return this; }
    @Override public ContentType setParameter(String name, String value)  { super.setParameter(name, value);  return this; }

    public String getPrimaryType()  { return mPrimaryType; }
    public String getSubType()      { return mSubType; }

    private void normalizeValue() {
        String value = getValue();
        if (value == null || value.trim().equals("")) {
            // default to "text/plain" if no content-type specified
            setValue(mDefault);
        } else {
            if (!value.equals(value.trim().toLowerCase())) {
                // downcase the content-type if necessary
                setValue(value.trim().toLowerCase());
            } else {
                int slash = value.indexOf('/');
                if (slash <= 0 || slash >= value.length() - 1) {
                    // malformed content-type; default as best we can
                    setValue(value.equals("text") ? TEXT_PLAIN : APPLICATION_OCTET_STREAM);
                } else {
                    mPrimaryType = value.substring(0, slash).trim();
                    mSubType = value.substring(slash + 1).trim();
                    if (mPrimaryType.equals("") || mSubType.equals(""))
                        setValue(mPrimaryType.equals("text") ? TEXT_PLAIN : APPLICATION_OCTET_STREAM);
                }
            }
        }
    }

    public String toString()  { return toString(14); }
}