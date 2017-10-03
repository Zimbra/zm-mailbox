/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import java.io.InputStream;
import java.io.Reader;

import javax.activation.DataSource;

import org.apache.solr.common.SolrInputDocument;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
import com.zimbra.cs.mime.MimeHandlerManager;

/**
 * {@link MimeHandler} that creates a Lucene document from a {@code text/plain} part.
 *
 * @since Apr 1, 2004
 * @author schemers
 */
public class TextPlainHandler extends MimeHandler {

    private String content;

    @Override
    protected boolean runsExternally() {
        return false;
    }

    @Override
    public void addFields(SolrInputDocument doc) {
        // we add no type-specific fields to the doc
    }

    @Override
    protected String getContentImpl() throws MimeHandlerException {
        if (content == null) {
            DataSource source = getDataSource();
            if (source != null) {
                String ctype = source.getContentType();
                InputStream is = null;
                try {
                    Reader reader = Mime.getTextReader(is = source.getInputStream(), ctype, getDefaultCharset());
                    content = ByteUtil.getContent(reader, MimeHandlerManager.getIndexedTextLimit(), false);
                } catch (IOException e) {
                    throw new MimeHandlerException(e);
                } finally {
                    ByteUtil.closeStream(is);
                }
            }
        }
        if (content == null) {
            content = "";
        }
        return content;
    }

    /** No need to convert plain text document ever. */
    @Override
    public boolean doConversion() {
        return false;
    }

    @Override
    public String convert(AttachmentInfo doc, String baseURL) {
        throw new UnsupportedOperationException();
    }

}
