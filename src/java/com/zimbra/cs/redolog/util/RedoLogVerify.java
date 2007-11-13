/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 11. 3.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.redolog.RolloverManager;
import com.zimbra.cs.redolog.logger.FileHeader;
import com.zimbra.cs.redolog.logger.FileLogReader;
import com.zimbra.cs.redolog.op.RedoableOp;

/**
 * @author jhahm
 */
public class RedoLogVerify {

    private static Options mOptions = new Options();
    private PrintStream mOut = System.out;

    static {
        mOptions.addOption("q", "quiet",   false, "quiet mode");
        mOptions.addOption("m", "message",   false, "show message body data");
    }

    private static void usage(String errmsg) {
        if (errmsg != null) {
            System.err.println(errmsg);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("RedoLogVerify [options] [log files/directories]",
            "where [options] are:", mOptions,
            "and [log files] are redo log files.");
        System.exit((errmsg == null) ? 0 : 1);
    }

    private static CommandLine parseArgs(String args[]) {
        StringBuffer gotCL = new StringBuffer("cmdline: ");
        for (int i = 0; i < args.length; i++) {
            gotCL.append("'").append(args[i]).append("' ");
        }

        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }
        return cl;
    }


    private static class BadFile {
        public File file;
        public Throwable error;
        public BadFile(File f, Throwable e) { file = f; error = e; }
    }

    private boolean mQuiet;
    private boolean mDumpMessageBody;
    private List<BadFile> mBadFiles;

    public RedoLogVerify(boolean quiet, boolean dumpMsgBody, OutputStream out) {
        mQuiet = quiet;
        mDumpMessageBody = dumpMsgBody;
        mBadFiles = new ArrayList<BadFile>();
        if (out != null) {
            mOut = new PrintStream(out);
        }
    }

    public boolean scanLog(File logfile) throws IOException {
        boolean good = false;
        FileLogReader logReader = new FileLogReader(logfile, false);
        logReader.open();
        FileHeader header = logReader.getHeader();
        mOut.println("HEADER");
        mOut.println("------");
        mOut.println(header);
        mOut.println("------");
        long lastPosition = 0;

        try {
            RedoableOp op = null;
            while ((op = logReader.getNextOp()) != null) {
                lastPosition = logReader.position();
                if (!mQuiet)
                    mOut.println(op);
                InputStream dataStream = op.getAdditionalDataStream();
                if (mDumpMessageBody && dataStream != null) {
                    ByteUtil.copy(dataStream, true, mOut, false);
                    mOut.println("<END OF MESSAGE>");
                }
            }
            good = true;
        } catch (IOException e) {
            // The IOException could be a real I/O problem or it could mean
            // there was a server crash previously and there were half-written
            // log entries.  We can't really tell which case it is, so just
            // assume the second case and truncate the file after the last
            // successfully read item.

            long size = logReader.getSize();
            if (lastPosition < size) {
                long diff = size - lastPosition;
                mOut.println("There were " + diff + " bytes of junk data at the end.");
                throw e;
            }
        } finally {
            logReader.close();
        }
        return good;
    }

    public boolean verifyFile(File file) {
        mOut.println("VERIFYING: " + file.getName());
        boolean good = false;
        try {
            good = scanLog(file);
        } catch (IOException e) {
            mBadFiles.add(new BadFile(file, e));
            mOut.println("Exception while verifying " + file.getName());
            e.printStackTrace();
        }
        mOut.println();
        return good;
    }

    private boolean verifyFiles(File[] files) {
        boolean allGood = true;
        for (File log : files) {
            boolean b = verifyFile(log);
            allGood = allGood && b;
        }
        return allGood;
    }

    private boolean verifyDirectory(File dir) {
        mOut.println("VERIFYING DIRECTORY: " + dir.getName());
        File[] all = dir.listFiles();
        if (all == null || all.length == 0)
            return true;

        List<File> fileList = new ArrayList<File>(all.length);
        for (File f : all) {
            if (!f.isDirectory()) {
                String fname = f.getName();
                if (fname.lastIndexOf(".log") == fname.length() - 4)
                    fileList.add(f);
            }
        }

        File[] files = new File[fileList.size()];
        fileList.toArray(files);
        RolloverManager.sortArchiveLogFiles(files);
        return verifyFiles(files);
    }

    private void listErrors() {
        if (mBadFiles.size() == 0)
            return;
        mOut.println();
        mOut.println();
        mOut.println("-----------------------------------------------");
        mOut.println();
        mOut.println("The following files had errors:");
        mOut.println();
        for (BadFile bf : mBadFiles) {
            mOut.println(bf.file.getName());
            mOut.println("    " + bf.error.getMessage());
        }
    }

    public static void main(String[] cmdlineargs) throws Exception {
        CliUtil.toolSetup();
        CommandLine cl = parseArgs(cmdlineargs);
        String[] args = cl.getArgs();

        if (args.length < 1)
            usage(null);

        //FileLogReader.setSkipBadBytes(true);

        boolean allGood = true;
        RedoLogVerify verify =
            new RedoLogVerify(cl.hasOption('q'), cl.hasOption('m'), null);

        for (int i = 0; i < args.length; i++) {
            File f = new File(args[i]);
            boolean good = false;
            if (f.isDirectory())
                good = verify.verifyDirectory(f);
            else
                good = verify.verifyFile(f);
            allGood = allGood && good;
            System.out.println();
        }

        if (!allGood) {
            verify.listErrors();
            System.exit(1);
        }
    }
}
