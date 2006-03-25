package com.zimbra.cs.mime.handler;

import java.io.InputStreamReader;
import java.io.Reader;

import javax.activation.DataSource;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.apache.lucene.document.Document;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;
import com.zimbra.cs.util.ZimbraLog;

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
            ZComponent vevent = miCalendar.getComponent(ZCalendar.ICalTok.VEVENT);
            if (vevent != null) {
                mContent = vevent.getPropVal(ZCalendar.ICalTok.DESCRIPTION, null);
                if (mContent == null || mContent.trim().equals(""))
                    mContent = vevent.getPropVal(ZCalendar.ICalTok.SUMMARY, "");
            } else
                mContent = "";
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
