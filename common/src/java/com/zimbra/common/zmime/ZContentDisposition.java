/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.zmime;

public class ZContentDisposition extends ZCompoundHeader {
    private static final String ATTACHMENT = "attachment";
    private static final String INLINE     = "inline";

    private String disposition;

    public ZContentDisposition(String value) {
        super("Content-Disposition", value);
        normalizeDisposition();
    }

    public ZContentDisposition(String value, boolean use2231) {
        super("Content-Disposition", value, use2231);
        normalizeDisposition();
    }

    ZContentDisposition(String name, byte[] content, int start, String defaultType) {
        super(name, content, start);
        normalizeDisposition();
    }

    public ZContentDisposition(ZInternetHeader header) {
        super(header);
        normalizeDisposition();
    }

    @Override
    protected ZContentDisposition clone() {
        return new ZContentDisposition(this);
    }


    public ZContentDisposition setDisposition(String disposition) {
        return setPrimaryValue(disposition);
    }

    @Override
    public ZContentDisposition setPrimaryValue(String value) {
        super.setPrimaryValue(value);
        normalizeDisposition();
        return this;
    }

    @Override
    public ZContentDisposition setParameter(String name, String value) {
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

    @Override
    protected void reserialize() {
        if (content == null) {
            super.setPrimaryValue(getDisposition());
            super.reserialize();
        }
    }

    @Override
    public ZContentDisposition cleanup() {
        super.setPrimaryValue(getDisposition());
        super.cleanup();
        return this;
    }
}