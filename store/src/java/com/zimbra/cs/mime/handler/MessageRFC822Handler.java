/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Apr 1, 2004
 */
package com.zimbra.cs.mime.handler;

import java.io.InputStream;

import javax.activation.DataSource;
import javax.mail.internet.InternetHeaders;

import org.apache.solr.common.SolrInputDocument;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
import com.zimbra.cs.mime.MimeHandlerManager;

/**
 * @author schemers
 *
 *  class that creates a Lucene document from a JavaMail Message
 */
public class MessageRFC822Handler extends MimeHandler {

    @Override
    protected boolean runsExternally() {
        return false;
    }

    @Override
    public void addFields(SolrInputDocument doc) {
    }

    /**
     * Returns the subject of the attached message.
     */
    @Override
    protected String getContentImpl() throws MimeHandlerException {
        DataSource ds = getDataSource();
        if (ds == null) {
            return null;
        }
        InputStream is = null;
        String content = null;
        try {
            is = ds.getInputStream();
            if (is == null) {
                return null;
            }
            InternetHeaders headers = new InternetHeaders(is);
            String[] subject = headers.getHeader("Subject");
            if (subject == null || subject.length == 0 || subject[0] == null) {
                return null;
            }
            int maxLength = MimeHandlerManager.getIndexedTextLimit();
            if (subject[0].length() > maxLength) {
                content = subject[0].substring(0, maxLength);
            } else {
                content = subject[0];
            }
        } catch (Exception e) {
            throw new MimeHandlerException(e);
        } finally {
            ByteUtil.closeStream(is);
        }

        return content;
    }

    @Override
    public boolean doConversion() {
        return false;
    }

    @Override
    public String convert(AttachmentInfo doc, String baseURL) {
        throw new IllegalStateException("no need to convert message/rfc822 content");
    }
}
