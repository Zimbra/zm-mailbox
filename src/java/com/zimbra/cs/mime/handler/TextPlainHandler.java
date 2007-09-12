/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

  /*
 * Created on Apr 1, 2004
 *
 */
package com.zimbra.cs.mime.handler;

import java.io.IOException;

import javax.activation.DataSource;

import org.apache.lucene.document.Document;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;

/**
 * @author schemers
 *
 *  class that creates a Lucene document from a plain text part
 */
public class TextPlainHandler extends MimeHandler {

    private String mContent;

    @Override protected boolean runsExternally() {
        return false;
    }

    @Override public void addFields(Document doc) {
        // we add no type-specific fields to the doc
    }

    @Override protected String getContentImpl() throws MimeHandlerException {
        if (mContent == null) {
            DataSource source = getDataSource();
            String ctype = source.getContentType();
            try {
                // decodeText always closes the input stream
                mContent = Mime.decodeText(source.getInputStream(), ctype);
            } catch (IOException e) {
                throw new MimeHandlerException(e);
            }
        }
        if (mContent == null)
            mContent = "";
        
        return mContent;
    }

    /** No need to convert plain text document ever. */
    @Override public boolean doConversion() {
        return false;
    }

    @Override public String convert(AttachmentInfo doc, String baseURL) {
        throw new IllegalStateException("No need to convert plain text");
    }

}
