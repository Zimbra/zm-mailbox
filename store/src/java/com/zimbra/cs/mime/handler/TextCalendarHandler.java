/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.activation.DataSource;

import net.fortuna.ical4j.data.ParserException;

import org.apache.solr.common.SolrInputDocument;

import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZICalendarParseHandler;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
import com.zimbra.cs.mime.MimeHandlerManager;

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
        if (source == null) {
            mContent = "";
            return;
        }
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

    @Override public void addFields(SolrInputDocument doc) {
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

        @Override
        public void startCalendar() throws ParserException {
            mInZCalendar = true;
        }

        @Override
        public void endCalendar() throws ParserException {
            mInZCalendar = false;
            mNumCals++;
        }

        @Override
        public boolean inZCalendar() { return mInZCalendar; }
        @Override
        public int getNumCals() { return mNumCals; }

        @Override
        public void startComponent(String name) {
            // nothing to do
        }

        @Override
        public void endComponent(String name) throws ParserException {
            // nothing to do
        }

        @Override
        public void startProperty(String name) {
            mCurProp = name != null ? name.toUpperCase() : null;
        }

        @Override
        public void propertyValue(String value) throws ParserException {
            if (!mMaxedOut && sIndexedProps.contains(mCurProp)) {
                appendContent(value);
            }
        }

        @Override
        public void endProperty(String name) {
            mCurProp = null;
        }

        @Override
        public void parameter(String name, String value) {
            // nothing to do
        }
    }
}
