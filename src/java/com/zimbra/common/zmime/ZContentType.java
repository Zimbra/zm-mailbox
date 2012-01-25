/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.common.zmime;

import com.zimbra.common.util.StringUtil;

public class ZContentType extends ZCompoundHeader {
    private String primaryType, subType;
    private final String defaultType;

    public static final String TEXT_PLAIN = "text/plain";
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String MESSAGE_RFC822 = "message/rfc822";
    public static final String DEFAULT = TEXT_PLAIN;

    public ZContentType(String value) {
        this(value, DEFAULT);
    }

    public ZContentType(String value, String defaultType) {
        super("Content-Type", value);
        this.defaultType = defaultType == null || defaultType.isEmpty() ? DEFAULT : defaultType;
        normalizeType();
    }

    public ZContentType(String value, boolean use2231) {
        super("Content-Type", value, use2231);
        this.defaultType = DEFAULT;
        normalizeType();
    }

    ZContentType(String name, byte[] content, int start, String defaultType) {
        super(name, content, start);
        this.defaultType = defaultType == null || defaultType.isEmpty() ? DEFAULT : defaultType;
        normalizeType();
    }

    public ZContentType(ZContentType ctype) {
        super(ctype);
        this.defaultType = ctype == null ? DEFAULT : ctype.defaultType;
        normalizeType();
    }

    ZContentType(ZInternetHeader header, String defaultType) {
        super(header);
        this.defaultType = defaultType == null || defaultType.isEmpty() ? DEFAULT : defaultType;
        normalizeType();
    }

    @Override
    protected ZContentType clone() {
        return new ZContentType(this);
    }


    public ZContentType setZContentType(String contentType) {
        return setPrimaryValue(contentType);
    }

    @Override
    public ZContentType setPrimaryValue(String value) {
        super.setPrimaryValue(value);
        normalizeType();
        return this;
    }

    public ZContentType setSubType(String subtype) {
        if (!subType.equals(subtype)) {
            super.setPrimaryValue(primaryType + '/' + subtype);
            normalizeType();
        }
        return this;
    }

    @Override
    public ZContentType setParameter(String name, String value) {
        super.setParameter(name, value);
        return this;
    }

    public String getBaseType() {
        return primaryType + '/' + subType;
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public String getSubType() {
        return subType;
    }

    private void normalizeType() {
        String primary = getPrimaryValue();
        int cutoff = StringUtil.indexOfAny(primary, "()<>@,;:\\\"[]?=");  // TSPECIALS minus '/'
        if (cutoff != -1) {
            primary = primary.substring(0, cutoff);
        }
        String value = primary == null || primary.isEmpty() ? defaultType : primary.trim().toLowerCase();

        this.primaryType = this.subType = "";
        int slash = value.indexOf('/');
        if (slash != -1) {
            this.primaryType = value.substring(0, slash).trim();
            cutoff = value.indexOf('/', slash + 1);  // second '/' not allowed
            this.subType = value.substring(slash + 1, cutoff == -1 ? value.length() : cutoff).trim();
        }

        if (primaryType.isEmpty() || subType.isEmpty()) {
            // malformed content-type; default as best we can
            if ("text".equals(slash == -1 ? value : primaryType)) {
                this.primaryType = "text";
                this.subType     = "plain";
            } else {
                this.primaryType = "application";
                this.subType     = "octet-stream";
            }
        }
    }

    @Override
    protected void reserialize() {
        if (content == null) {
            super.setPrimaryValue(getBaseType());
            super.reserialize();
        }
    }

    @Override
    public ZContentType cleanup() {
        super.setPrimaryValue(getBaseType());
        super.cleanup();
        return this;
    }
}
