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
package com.zimbra.common.mime;

public class ContentType extends MimeCompoundHeader {
    private String mPrimaryType, mSubType;
    private final String mDefault;

    public static final String TEXT_PLAIN = "text/plain";
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String MESSAGE_RFC822 = "message/rfc822";
    public static final String DEFAULT = TEXT_PLAIN;

    public ContentType(String value) {
        super("Content-Type", value);
        mDefault = DEFAULT;
        normalizeType();
    }

    public ContentType(String value, String defaultType) {
        super("Content-Type", value);
        mDefault = defaultType == null || defaultType.isEmpty() ? DEFAULT : defaultType;
        normalizeType();
    }

    public ContentType(String value, boolean use2231) {
        super("Content-Type", value, use2231);
        mDefault = DEFAULT;
        normalizeType();
    }

    ContentType(String name, byte[] content, int start, String defaultType) {
        super(name, content, start);
        mDefault = defaultType == null || defaultType.isEmpty() ? DEFAULT : defaultType;
        normalizeType();
    }

    public ContentType(ContentType ctype) {
        super(ctype);
        mDefault = ctype == null ? DEFAULT : ctype.mDefault;
        normalizeType();
    }

    ContentType(MimeHeader header, String defaultType) {
        super(header);
        mDefault = defaultType == null || defaultType.isEmpty() ? DEFAULT : defaultType;
        normalizeType();
    }


    public ContentType setContentType(String contentType) {
        return setPrimaryValue(contentType);
    }

    @Override public ContentType setPrimaryValue(String value) {
        super.setPrimaryValue(value);
        normalizeType();
        return this;
    }

    public ContentType setSubType(String subtype) {
        if (!mSubType.equals(subtype)) {
            super.setPrimaryValue(mPrimaryType + '/' + subtype);
            normalizeType();
        }
        return this;
    }

    @Override public ContentType setParameter(String name, String value) {
        super.setParameter(name, value);
        return this;
    }

    public String getContentType() {
        return mPrimaryType + '/' + mSubType;
    }

    public String getPrimaryType() {
        return mPrimaryType;
    }

    public String getSubType() {
        return mSubType;
    }

    private void normalizeType() {
        String primary = getPrimaryValue();
        String value = primary == null || primary.isEmpty() ? mDefault : primary.trim().toLowerCase();

        mPrimaryType = mSubType = "";
        int slash = value.indexOf('/');
        if (slash != -1) {
            mPrimaryType = value.substring(0, slash).trim();
            mSubType = value.substring(slash + 1).trim();
        }

        if (mPrimaryType.isEmpty() || mSubType.isEmpty()) {
            // malformed content-type; default as best we can
            if ("text".equals(slash == -1 ? value : mPrimaryType)) {
                mPrimaryType = "text";
                mSubType     = "plain";
            } else {
                mPrimaryType = "application";
                mSubType     = "octet-stream";
            }
        }
    }

    @Override protected void reserialize() {
        if (mContent == null) {
            super.setPrimaryValue(getContentType());
            super.reserialize();
        }
    }

    @Override public ContentType cleanup() {
        super.setPrimaryValue(getContentType());
        super.cleanup();
        return this;
    }
}