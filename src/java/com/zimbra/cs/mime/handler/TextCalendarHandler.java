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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mime.handler;

import java.io.InputStreamReader;
import java.io.Reader;

import javax.activation.DataSource;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.apache.lucene.document.Document;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
import com.zimbra.common.util.ZimbraLog;

public class TextCalendarHandler extends MimeHandler {
    private String mContent;
    private ZVCalendar miCalendar;

    @Override
    public void init(DataSource source) throws MimeHandlerException {
        super.init(source);
        mContent = null;
        miCalendar = null;
    }

    @Override
    public ZVCalendar getICalendar() throws MimeHandlerException {
        analyze();
        return miCalendar;
    }

    @Override
    protected String getContentImpl() throws MimeHandlerException {
        analyze();
        return mContent;
    }

    private void analyze() throws MimeHandlerException {
        if (mContent != null)
            return;
        try {
            DataSource source = getDataSource();
            String charset = Mime.P_CHARSET_DEFAULT;
            String ctStr = source.getContentType();
            if (ctStr != null) {
                try {
                    ContentType ct = new ContentType(ctStr);
                    String p = ct.getParameter(Mime.P_CHARSET);
                    if (p != null) charset = p;
                } catch (ParseException e) {}            
            }
            Reader reader =
                new InputStreamReader(source.getInputStream(), charset);
            miCalendar = ZCalendarBuilder.build(reader);
            ICalTok types[] = { ICalTok.VEVENT, ICalTok.VTODO };
            mContent = "";
            for (ICalTok type : types) {
                ZComponent comp = miCalendar.getComponent(type);
                if (comp != null) {
                    String content = comp.getPropVal(ICalTok.DESCRIPTION, "");
                    content = content.trim();
                    if (content.equals("")) {
                        content = comp.getPropVal(ICalTok.SUMMARY, null);
                        content = content.trim();
                    }
                    if (!content.equals("")) {
                        if (!mContent.equals(""))
                            mContent += " ";
                        mContent += content;
                    }
                }
            }
        } catch (Exception e) {
            mContent = "";
            ZimbraLog.index.warn("error reading text/calendar mime part", e);
            throw new MimeHandlerException(e);
        }
    }

    @Override
    public void addFields(Document doc) {
        
    }

    @Override
    public String convert(AttachmentInfo doc, String baseURL) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean doConversion() {
        return false;
    }

}
