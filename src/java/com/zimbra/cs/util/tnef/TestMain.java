/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.cs.util.tnef;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.util.SharedFileInputStream;

import net.fortuna.ical4j.data.ContentHandler;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Property;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.tnef.DefaultTnefToICalendar;
import com.zimbra.cs.util.tnef.PlainTextFinder;
import com.zimbra.cs.util.tnef.TnefToICalendar;
import com.zimbra.common.util.Log;
import com.zimbra.cs.util.tnef.TNEFtoIcalendarServiceException;
import com.zimbra.cs.util.tnef.TNEFtoIcalendarServiceException.UnsupportedTnefCalendaringMsgException;
import com.zimbra.cs.util.tnef.mapi.RecurrenceDefinition;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.util.JMSession;
import java.io.FileOutputStream;

public class TestMain {

    static Log sLog = ZimbraLog.tnef;

    private static TnefToICalendar getConverter() {
        return new DefaultTnefToICalendar();
    }

    private static boolean doConversion(MimeMessage mm, Writer out,
            TnefToICalendar converter, File tnefFile, boolean debug)
    throws ServiceException, IOException, MessagingException {
        boolean successful = false;
        // Find the TNEF part.
        TNEFPartFinder finder = new TNEFPartFinder();
        finder.accept(mm);
        MimePart tnefPart = finder.getTNEFPart();
        if (tnefPart == null)
            throw ServiceException.FAILURE("No TNEF part in the message!", null);

        if (tnefFile != null) {
            FileOutputStream wout = null;
            try {
                byte[] bytes = ByteUtil.getContent(tnefPart.getInputStream(), tnefPart.getSize());
                wout = new FileOutputStream(tnefFile);
                wout.write(bytes);
                wout.flush();
            } finally {
                if (wout != null) {
                    wout.close();
                }
            }
        }

        if (debug) {
            // Test text part extraction.
            String desc = getPlainText(mm);
            if (desc != null) {
                System.out.println("<<< TEXT PART BEGIN >>>");
                System.out.println(desc);
                System.out.println("<<< TEXT PART END >>>");
            }
        }

        if (converter == null)
            throw ServiceException.FAILURE("No converter!", null);

        TestContentHandler icalGen = new TestContentHandler(debug);

        successful = converter.convert(mm, tnefPart.getInputStream(), icalGen);
        if (successful) {
            for (ZVCalendar cal : icalGen.getCals()) {
                cal.toICalendar(out);
            }
        }
        out.flush();
        return successful;
    }

    // for finding the (first) TNEF part in a MimeMessage
    private static class TNEFPartFinder extends MimeVisitor {
        private MimePart mTNEFPart;

        public TNEFPartFinder() {}

        public MimePart getTNEFPart() { return mTNEFPart; }

        private static boolean matchingType(Part part) throws MessagingException {
            String type = part.getContentType();
            return
                type != null &&
                ((type = type.toLowerCase()).startsWith("application/ms-tnef") ||
                 type.startsWith("application/vnd.ms-tnef"));
        }

        @Override
        protected boolean visitBodyPart(MimeBodyPart bp) throws MessagingException {
            if (mTNEFPart == null && matchingType(bp))
                mTNEFPart = bp;
            return false;
        }

        @Override
        protected boolean visitMessage(MimeMessage mm, VisitPhase visitKind) throws MessagingException {
            if (mTNEFPart == null && matchingType(mm))
                mTNEFPart = mm;
            return false;
        }

        @Override
        protected boolean visitMultipart(MimeMultipart mp, VisitPhase visitKind) throws MessagingException {
            return false;
        }
    }

    // one way to get plain/text meeting description from the message
    private static String getPlainText(MimeMessage mm) throws MessagingException, IOException {
        PlainTextFinder finder = new PlainTextFinder();
        finder.accept(mm);
        return finder.getPlainText();
    }

    private static final String UTF8 = "utf-8";

    private static void usage() {
        System.err.println("Usage: java com.zimbra.cs.util.tnef.TestMain [-v] \n");
        System.err.println("            -i <MIME file> [-o <iCalendar file>] [-t <tnef file>] [-r <recurInfo file>]");
        System.err.println("-v: verbose output");
        System.err.println("-i: MIME input file TNEF part");
        System.err.println("-o: iCalendar output file; if unspecified, output goes to stdout");
        System.err.println("-t: TNEF output file; The main TNEF attachment will be written here if present");
        System.err.println("-r: A diagnostic string representing any recurrence property will be written here");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        ZimbraLog.toolSetupLog4jConsole("INFO", true, false);

        boolean verbose = false;
        boolean inputWasIcs = false;
        File mimeFile = null;
        File icalFile = null;
        File tnefFile = null;
        File recurInfoFile = null;
        String outDirName = null;
        int firstTestFileArgIndex = 0;
        // Thread.sleep(30000);
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg != null) {
                if (arg.equalsIgnoreCase("-v")) {
                    verbose = true;
                } else if (arg.equalsIgnoreCase("-D")) {
                    if (i >= args.length - 2)
                        usage();
                    outDirName = args[i+1];
                    firstTestFileArgIndex = i + 2;;
                    break;
                } else if (arg.equalsIgnoreCase("-i")) {
                    if (i >= args.length - 1)
                        usage();
                    String fname = args[i+1];
                    mimeFile = new File(fname);
                    inputWasIcs = fname.endsWith(".ics");
                    ++i;
                } else if (arg.equalsIgnoreCase("-o")) {
                    if (i >= args.length - 1)
                        usage();
                    icalFile = new File(args[i+1]);
                    ++i;
                } else if (arg.equalsIgnoreCase("-t")) {
                    if (i >= args.length - 1) {
                        usage();
                    }
                    tnefFile = new File(args[i+1]);
                    ++i;
                } else if (arg.equalsIgnoreCase("-r")) {
                    if (i >= args.length - 1) {
                        usage();
                    }
                    recurInfoFile = new File(args[i+1]);
                    ++i;
                } else {
                    usage();
                }
            }
        }

        if (inputWasIcs) {
            SharedFileInputStream sfisMime = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
            ByteUtil.copy(sfisMime, false, baos, true);
            writeIcalendarData(mimeFile, baos, icalFile);
            validateIcalendarData(mimeFile, baos);
            return;
        }

        if (outDirName != null) {
            File outDir = new File(outDirName);
            if (!outDir.exists()) {
                sLog.error("Output directory %s does not exist.", outDirName);
                return;
            }

            for (int i = firstTestFileArgIndex ; i < args.length; ++i) {
                mimeFile = new File(args[i]);
                String prefix = mimeFile.getName().replace(".eml", "");
                icalFile = new File(outDir, prefix + ".ics");
                tnefFile = new File(outDir, prefix + ".tnef");
                recurInfoFile = new File(outDir, prefix + ".recurState");
                processMimeFile(mimeFile, icalFile, tnefFile, recurInfoFile, verbose);
            }
            return;
        }

        // Thread.sleep(30000);
        if (mimeFile == null)
            usage();

        processMimeFile(mimeFile, icalFile, tnefFile, recurInfoFile, verbose);
//        List<ZVCalendar> icals = ZCalendarBuilder.buildMulti(bais, UTF8);
//        Account dummyAcct = new Account();
//        List<Invite> invites = Invite.createFromCalendar(dummyAcct, null, icals, false);
//        boolean allGood = true;
//        for (Invite inv : invites) {
//            try {
//                // This doesn't check much...
//                inv.sanitize(true);
//            } catch (ServiceException e) {
//                ZimbraLog.tnef.error(e.getMessage());
//                allGood = false;
//            }
//        }
//        if (!allGood)
//            System.exit(1);
    }

    /**
     * @param mimeFile Name of original test file - used for diagnostic reporting
     * @param icalFile the file to write the ICALENDAR data to.
     * @param tnefFile the file to write TNEF data to.
     * @param recurInfoFile the file to write recurrence diagnostics data to.
     * @return true if successfully written data.
     */
    private static boolean processMimeFile(File mimeFile,
            File icalFile, File tnefFile, File recurInfoFile, boolean verbose) {
        if (!mimeFile.exists()) {
            sLog.warn("Can't find MIME file %s", mimeFile.getPath());
            return false;
        }

        // Prepare the input and output.
        SharedFileInputStream sfisMime = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
        Writer baosOut = null;
        boolean doneConversion = false;
        try {
            sfisMime = new SharedFileInputStream(mimeFile);
            baosOut = new OutputStreamWriter(baos, UTF8);
    
            // Do the conversion.
            MimeMessage mm = new MimeMessage(JMSession.getSession(), sfisMime);
            TnefToICalendar converter = getConverter();
            doneConversion = doConversion(mm, baosOut, converter, tnefFile, verbose);
            if (recurInfoFile != null) {
                if (converter instanceof DefaultTnefToICalendar) {
                    DefaultTnefToICalendar tnef2ical = (DefaultTnefToICalendar) converter;
                    RecurrenceDefinition recurDef = tnef2ical.getRecurDef();
                    if (recurDef != null) {
                        FileWriter rsFileWriter = null;
                        try {
                            rsFileWriter = new FileWriter(recurInfoFile);
                            rsFileWriter.write(recurDef.toString());
                        } finally {
                            try {
                                if (rsFileWriter != null) {
                                    rsFileWriter.close();
                                }
                            } catch (IOException e) {
                                sLog.error("Problem writing to recurInfo file %s", recurInfoFile, e);
                            }
                        }
                    }
                }
            }
        } catch (UnsupportedTnefCalendaringMsgException ex) {
            sLog.warn("Unable to map %s to ICALENDAR", mimeFile.getPath(), ex);
            return false;
        } catch (TNEFtoIcalendarServiceException ex) {
            sLog.warn("Problem encountered mapping %s to ICALENDAR", mimeFile.getPath(), ex);
            return false;
        } catch (MessagingException ex) {
            sLog.warn("Problem encountered mapping %s to ICALENDAR", mimeFile.getPath(), ex);
            return false;
        } catch (ServiceException ex) {
            sLog.warn("Problem encountered mapping %s to ICALENDAR", mimeFile.getPath(), ex);
            return false;
        } catch (IOException ex) {
            sLog.warn("IO Problem encountered mapping %s to ICALENDAR", mimeFile.getPath(), ex);
            return false;
        } finally {
            try {
                if (sfisMime != null)
                    sfisMime.close();
            } catch (IOException e) {sLog.error("Problem closing mime stream", e);}
        }
        if (!doneConversion) {
            return false;
        }

        if (!writeIcalendarData(mimeFile, baos, icalFile)) {
            return false;
        }

        if (!validateIcalendarData(mimeFile, baos)) {
            return false;
        }
        return true;
    }

    /**
     * @param mimeFile Name of original test file - used for diagnostic reporting
     * @param icalBaos contains the ICALENDAR data
     * @param icalFile the file to write the ICALENDAR data to.
     * @return true if successfully written data.
     */
    private static boolean writeIcalendarData(File mimeFile, ByteArrayOutputStream icalBaos,
            File icalFile) {
        String ical = null;
        Writer wout = null;
        try {
            ical = icalBaos.toString(UTF8);
            if (icalFile != null)
                wout = new FileWriter(icalFile);
            else
                wout = new OutputStreamWriter(System.out, UTF8);
            wout.write(ical);
            wout.flush();
        } catch (UnsupportedEncodingException e) {
            sLog.warn("Problem Writing ICALENDAR for %s", mimeFile.getPath(), e);
            return false;
        } catch (IOException e) {
            sLog.warn("Problem Writing ICALENDAR for %s", mimeFile.getPath(), e);
            return false;
        } finally {
            if (wout != null && icalFile != null)
                try {
                    wout.close();
                } catch (IOException e) {
                    sLog.warn("Problem closing Writer for ICALENDAR for %s", mimeFile.getPath(), e);
                    return false;
                }
        }
        return true;
    }

    /**
     * @param mimeFile Name of original test file - used for diagnostic reporting
     * @param icalBaos contains the ICALENDAR data
     * @return true if successfully written data.
     */
    private static boolean validateIcalendarData(File mimeFile, ByteArrayOutputStream icalBaos) {
        // Parse the iCalendar object and verify.
        ByteArrayInputStream bais = new ByteArrayInputStream(icalBaos.toByteArray());
        try {
            ZCalendarBuilder.buildMulti(bais, UTF8);
        } catch (ServiceException e) {
            sLog.warn("Problem validating ICALENDAR for %s", mimeFile.getPath(), e);
            return false;
        }
        return true;
    }

    private static class TestContentHandler implements ContentHandler {

        boolean mDebug;
        List<ZVCalendar> mCals = new ArrayList<ZVCalendar>(1);
        ZVCalendar mCurCal = null;
        List<ZComponent> mComponents = new ArrayList<ZComponent>();
        ZProperty mCurProperty = null;
        private int mNumCals;
        private boolean mInZCalendar;
        private int mIndentLevel;

        public TestContentHandler(boolean debug) { mDebug = debug; }
        public List<ZVCalendar> getCals() { return mCals; }

        public boolean inZCalendar() { return mInZCalendar; }
        public int getNumCals() { return mNumCals; }

        private void printDebug(String format, Object ... objects) {
            if (mDebug) {
                int indentChars = mIndentLevel * 2;
                for (int i = 0; i < indentChars; ++i)
                    System.out.print(' ');
                System.out.println(String.format(format, objects));
            }
        }

        // ContentHandler methods

        public void startCalendar() {
            printDebug("<calendar>");
            ++mIndentLevel;

            mInZCalendar = true;
            mCurCal = new ZVCalendar();
            mCals.add(mCurCal);
        }

        public void endCalendar() {
            --mIndentLevel;
            if (mIndentLevel < 0) {
                printDebug("[UNMATCHED endCalendar/endComponent/endProperty]");
                mIndentLevel = 0;
            }
            printDebug("</calendar>");

            mCurCal = null;
            mInZCalendar = false;
            mNumCals++;
        }

        // name = "VEVENT", "VALARM", "VTIMEZONE", "STANDARD", or "DAYLIGHT"
        public void startComponent(String name) {
            printDebug("<component:%s>", name);
            ++mIndentLevel;

            ZComponent newComponent = new ZComponent(name);
            if (mComponents.size() > 0) {
                mComponents.get(mComponents.size()-1).addComponent(newComponent);
            } else {
                mCurCal.addComponent(newComponent); 
            }
            mComponents.add(newComponent);  
        }
        
        public void endComponent(String name) {
            --mIndentLevel;
            if (mIndentLevel < 0) {
                printDebug("[UNMATCHED endCalendar/endComponent/endProperty]");
                mIndentLevel = 0;
            }
            printDebug("</component:%s>", name);

            mComponents.remove(mComponents.size()-1);
        }

        public void startProperty(String name) {
            printDebug("<property:%s>", name);
            ++mIndentLevel;

            mCurProperty = new ZProperty(name);
            
            if (mComponents.size() > 0) {
                mComponents.get(mComponents.size()-1).addProperty(mCurProperty);
            } else {
                mCurCal.addProperty(mCurProperty);
            }
        }

        // Value should not have any encoding/escaping.  Email address should be prefixed with "mailto:" or "MAILTO:".
        public void propertyValue(String value) throws ParserException {
            printDebug("<value>%s</value>", value);

            String propName = mCurProperty.getName();
            if ((propName.equalsIgnoreCase(Property.CATEGORIES)) ||
                (propName.equalsIgnoreCase(Property.RESOURCES))) {
                mCurProperty.setValueList(ZCalendar.parseCommaSepText(value));
            } else {
                mCurProperty.setValue(value);
            }
        }

        public void endProperty(String name) {
            --mIndentLevel;
            if (mIndentLevel < 0) {
                printDebug("[UNMATCHED endCalendar/endComponent/endProperty]");
                mIndentLevel = 0;
            }
            printDebug("</property:%s>", name);

            mCurProperty = null;
        }

        // Value should not have any encoding/escaping.  Email address should be prefixed with "mailto:" or "MAILTO:".
        public void parameter(String name, String value) {
            printDebug("<parameter name=\"%s\">%s</parameter>", name, value);

            ZParameter param = new ZParameter(name, null);
            param.setValue(value);
            if (mCurProperty != null) {
                mCurProperty.addParameter(param);
            } else {
                ZimbraLog.calendar.debug("ERROR: got parameter " + name + "," + value + " outside of Property");
            }
        }
    }
}
