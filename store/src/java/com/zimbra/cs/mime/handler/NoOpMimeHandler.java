/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;

import org.apache.solr.common.SolrInputDocument;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
/**
 * A mime handler that does nothing. Unlike the unknown type handler
 * this won't throw any exceptions for calling the wrong method.
 *
 * This handler is returned when indexing is turned off
 * @author jpowers
 *
 */
public class NoOpMimeHandler extends MimeHandler {

    @Override
    protected void addFields(SolrInputDocument doc) throws MimeHandlerException {
    }

    @Override
    public boolean isIndexingEnabled() {
        return false;
    }

    @Override
    public String convert(AttachmentInfo doc, String urlPart)
            throws IOException, ConversionException {
        return "";
    }

    @Override
    public boolean doConversion() {
        return false;
    }

    @Override
    public String getContentType() {
        return super.getContentType();
    }

    @Override
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    public String getPartName() {
        return super.getPartName();
    }

    @Override
    protected String getContentImpl() throws MimeHandlerException {
        return "";
    }

    @Override
    protected boolean runsExternally() {
        return false;
    }
}
