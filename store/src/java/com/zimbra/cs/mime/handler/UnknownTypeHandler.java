/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mime.handler;

import org.apache.solr.common.SolrInputDocument;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mime.MimeHandler;

/**
 * {@link MimeHandler} for no conversion.
 *
 * @since Apr 1, 2004
 * @author schemers
 */
public class UnknownTypeHandler extends MimeHandler {

    private String contentType;

    @Override
    protected boolean runsExternally() {
        return false;
    }

    @Override
    public void addFields(SolrInputDocument doc) {
        // do nothing
    }

    @Override
    protected String getContentImpl() {
        return "";
    }

    @Override
    public boolean isIndexingEnabled() {
        return true;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String convert(AttachmentInfo doc, String baseURL) {
        throw new IllegalStateException("conversion not allowed for content of unknown type");
    }

    @Override
    public boolean doConversion() {
        return false;
    }

    @Override
    public void setContentType(String value) {
        contentType = value;
    }
}
