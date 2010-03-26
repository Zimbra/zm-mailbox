/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mime.handler;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.activation.DataSource;

import net.fortuna.ical4j.data.ParserException;

import org.apache.lucene.document.Document;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZICalendarParseHandler;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
import com.zimbra.cs.mime.MimeHandlerManager;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.mime.MimeConstants;

public class TextCalendarHandler extends MimeHandler {
    private String mContent;
    private ZVCalendar miCalendar;

    @Override protected boolean runsExternally() {
        return false;
    }

    @Override public ZVCalendar getICalendar() throws MimeHandlerException {
        analyze(true);
        return miCalendar;
    }

    @Override protected String getContentImpl() throws MimeHandlerException {
        analyze(false);
        return mContent;
    }

    private void analyze(boolean needCal) throws MimeHandlerException {
        if (mContent != null)
            return;

        DataSource source = getDataSource();
        InputStream is = null;
        try {
            is = source.getInputStream();
            String charset = MimeConstants.P_CHARSET_UTF8;
            String ctStr = source.getContentType();
            if (ctStr != null) {
                String cs = Mime.getCharset(ctStr);
                if (cs != null)
                    charset = cs;
            }
            if (needCal) {
                miCalendar = ZCalendarBuilder.build(is, charset);
                StringBuilder buf = new StringBuilder(1024);
                int maxLength = MimeHandlerManager.getIndexedTextLimit();
                for (Iterator<ZComponent> compIter = miCalendar.getComponentIterator();
                     compIter.hasNext() && buf.length() < maxLength; ) {
                    ZComponent comp = compIter.next();
                    for (Iterator<ZProperty> propIter = comp.getPropertyIterator(); propIter.hasNext(); ) {
                        ZProperty prop = propIter.next();
                        if (sIndexedProps.contains(prop.getName())) {
                            String value = prop.getValue();
                            if (value != null && value.length() > 0) {
                                if (buf.length() > 0)
                                    buf.append(' ');
                                buf.append(value);
                            }
                        }
                    }
                }
                mContent = buf.toString();
            } else {
                IcsParseHandler handler = new IcsParseHandler();
                ZCalendarBuilder.parse(is, charset, handler);
                mContent = handler.getContent();
            }
        } catch (Exception e) {
            mContent = "";
            ZimbraLog.index.warn("error reading text/calendar mime part", e);
            throw new MimeHandlerException(e);
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    @Override public void addFields(Document doc) {
    }

    @Override public String convert(AttachmentInfo doc, String baseURL) {
        throw new UnsupportedOperationException();
    }

    @Override public boolean doConversion() {
        return false;
    }

    private static final Set<String> sIndexedProps;
    static {
        sIndexedProps = new HashSet<String>();
        sIndexedProps.add(ICalTok.SUMMARY.toString());
        sIndexedProps.add(ICalTok.DESCRIPTION.toString());
        sIndexedProps.add(ICalTok.COMMENT.toString());
        sIndexedProps.add(ICalTok.LOCATION.toString());
    }

    public class IcsParseHandler implements ZICalendarParseHandler {

        private StringBuilder mContentBuf;
        private int mMaxLength;
        private boolean mMaxedOut;

        private String mCurProp;
        private int mNumCals;
        private boolean mInZCalendar;

        public IcsParseHandler() {
            mContentBuf = new StringBuilder(1024);
            mMaxLength = MimeHandlerManager.getIndexedTextLimit();
        }

        public String getContent() {
            return mContentBuf.toString();
        }

        private void appendContent(String str) {
            if (str != null && str.length() > 0) {
                if (mContentBuf.length() > 0)
                    mContentBuf.append(' ');
                mContentBuf.append(str);
                mMaxedOut = mContentBuf.length() >= mMaxLength;
            }
        }

        public void startCalendar() throws ParserException {
            mInZCalendar = true;
        }

        public void endCalendar() throws ParserException {
            mInZCalendar = false;
            mNumCals++;
        }

        public boolean inZCalendar() { return mInZCalendar; }
        public int getNumCals() { return mNumCals; }

        public void startComponent(String name) {
            // nothing to do
        }

        public void endComponent(String name) throws ParserException {
            // nothing to do
        }

        public void startProperty(String name) {
            mCurProp = name != null ? name.toUpperCase() : null;
        }

        public void propertyValue(String value) throws ParserException {
            if (!mMaxedOut && sIndexedProps.contains(mCurProp)) {
                appendContent(value);
            }
        }

        public void endProperty(String name) {
            mCurProp = null;
        }

        public void parameter(String name, String value) {
            // nothing to do
        }
    }
}
