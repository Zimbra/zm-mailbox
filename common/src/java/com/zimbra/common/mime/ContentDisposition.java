/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.mime;

public class ContentDisposition extends MimeCompoundHeader {
    private static final String ATTACHMENT = "attachment";
    private static final String INLINE     = "inline";

    private String disposition;

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

    @Override protected ContentDisposition clone() {
        return new ContentDisposition(this);
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
        return disposition;
    }

    private void normalizeDisposition() {
        String value = getPrimaryValue() == null ? "" : getPrimaryValue().trim().toLowerCase();
        this.disposition = value.equals(ATTACHMENT) || value.equals(INLINE) ? value : ATTACHMENT;
    }

    @Override protected void reserialize() {
        if (content == null) {
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