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

public class ContentDisposition extends MimeCompoundHeader {
    private static final String ATTACHMENT = "attachment";
    private static final String INLINE     = "inline";

    private String mDisposition;

    public ContentDisposition(String value) {
        super("Content-Disposition", value);
        normalizeDisposition();
    }

    public ContentDisposition(String value, boolean use2231) {
        super("Content-Disposition", value, use2231);
        normalizeDisposition();
    }

    ContentDisposition(String name, byte[] content, int start, String defaultType) {
        super(name, content, start);
        normalizeDisposition();
    }

    public ContentDisposition(MimeHeader header) {
        super(header);
        normalizeDisposition();
    }


    public ContentDisposition setDisposition(String disposition) {
        return setPrimaryValue(disposition);
    }

    @Override public ContentDisposition setPrimaryValue(String value) {
        super.setPrimaryValue(value);
        normalizeDisposition();
        return this;
    }

    @Override public ContentDisposition setParameter(String name, String value) {
        super.setParameter(name, value);
        return this;
    }

    public String getDisposition() {
        return mDisposition;
    }

    private void normalizeDisposition() {
        String value = getPrimaryValue() == null ? "" : getPrimaryValue().trim().toLowerCase();
        mDisposition = value.equals(ATTACHMENT) || value.equals(INLINE) ? value : ATTACHMENT;
    }

    @Override protected void reserialize() {
        if (mContent == null) {
            super.setPrimaryValue(getDisposition());
            super.reserialize();
        }
    }

    @Override public ContentDisposition cleanup() {
        super.setPrimaryValue(getDisposition());
        super.cleanup();
        return this;
    }
}