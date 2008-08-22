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
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.zimbra.cs.redolog.op.StoreIncomingBlob;

/**
 * @author jhahm
 */
public class RedoLogVerify {

    private static Options sOptions = new Options();

    private static final String OPT_HELP = "h";
    private static final String OPT_QUIET = "q";
    private static final String OPT_SHOW_BLOB = "show-blob";
    private static final String OPT_NO_OFFSET = "no-offset";
    private static final String OPT_MAILBOX_IDS = "m";

    static {
        sOptions.addOption(OPT_HELP, "help", false, "show this output");
        sOptions.addOption(OPT_QUIET, "quiet",   false,
                "quiet mode.  Only print the log filename and any errors.  This option can be used to " +
                "verify the integrity of redologs with minimal output.");
        sOptions.addOption(null, OPT_NO_OFFSET, false, "don't show file offsets and size for each redo op");
        sOptions.addOption(null, OPT_MAILBOX_IDS, true,
                "one or more mailbox ids separated by comma or white space.  The entire list must be " +
                "quoted if using space as separator.  If this option is given, only redo ops for the " +
                "specified mailboxes are dumped.  Omit this option to dump redo ops for all mailboxes.");
        sOptions.addOption(null, OPT_SHOW_BLOB, false,
                "show blob content.  Item's blob is printed, surrounded by <START OF BLOB> and <END OF BLOB> " +
                "markers.  The last newline before end marker is not part of the blob.");
    }

    private static void usage(String errmsg) {
        if (errmsg != null) {
            System.err.println(errmsg);
            System.err.println();
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("zmredodump [options] <redolog file/directory> [...]",
            "where [options] are:\n", sOptions,
            "\nMultiple log files/directories can be specified.  For each directory, all redolog files "+
            "directly under it are processed, sorted in ascending redolog sequence order.");
        System.exit((errmsg == null) ? 0 : 1);
    }

    private static CommandLine parseArgs(String args[]) {
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(sOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }
        return cl;
    }

    private static class Params {
        public Set<Integer> mboxIds = new HashSet<Integer>();
        public boolean quiet = false;
        public boolean hideOffset = false;
        public boolean showBlob = false;
        public boolean help = false;
    }

    private static Params initParams(CommandLine cl) {
        Params params = new Params();
        params.help = cl.hasOption(OPT_HELP);
        if (params.help)
            return params;

        params.quiet = cl.hasOption(OPT_QUIET);
        params.hideOffset = cl.hasOption(OPT_NO_OFFSET);
        params.showBlob = cl.hasOption(OPT_SHOW_BLOB);

        String mboxIdList = cl.getOptionValue(OPT_MAILBOX_IDS);
        if (mboxIdList != null) {
            String[] ids = mboxIdList.split("[, ]+");
            if (ids != null && ids.length > 0) {
                for (String val : ids) {
                    if (val != null && val.length() > 0) {
                        try {
                            int i = Integer.parseInt(val);
                            if (i > 0)
                                params.mboxIds.add(i);
                            else
                                usage("Invalid mailbox id \"" + val + "\"");
                        } catch (NumberFormatException e) {
                            usage("Invalid mailbox id \"" + val + "\"");
                        }
                    }
                }
            }
        }

        return params;
    }


    private static class BadFile {
        public File file;
        public Throwable error;
        public BadFile(File f, Throwable e) { file = f; error = e; }
    }

    private PrintStream mOut;
    private Params mParams;
    private List<BadFile> mBadFiles;

    public RedoLogVerify(Params params, PrintStream out) {
        mOut = out;
        mParams = params;
        if (mParams == null)
            mParams = new Params();
        mBadFiles = new ArrayList<BadFile>();
    }

    public boolean scanLog(File logfile) throws IOException {
        boolean good = false;
        FileLogReader logReader = new FileLogReader(logfile, false);
        logReader.open();
        if (!mParams.quiet) {
            FileHeader header = logReader.getHeader();
            mOut.println("HEADER");
            mOut.println("------");
            mOut.print(header);
            mOut.println("------");
        }

        boolean hasMailboxIdsFilter = !mParams.mboxIds.isEmpty();

        RedoableOp op = null;
        long lastPosition = 0;
        long lastOpStartOffset = 0;
        try {
            while ((op = logReader.getNextOp()) != null) {
                lastOpStartOffset = logReader.getLastOpStartOffset();
                lastPosition = logReader.position();
                if (hasMailboxIdsFilter) {
                    int mboxId = op.getMailboxId();
                    if (op instanceof StoreIncomingBlob) {
                        List<Integer> list = ((StoreIncomingBlob) op).getMailboxIdList();
                        if (list != null) {
                            boolean match = false;
                            for (Integer mid : list) {
                                if (mParams.mboxIds.contains(mid)) {
                                    match = true;
                                    break;
                                }
                            }
                            if (!match)
                                continue;
                        }
                        // If list==null, it's a store incoming blob op targeted at unknown set of mailboxes.
                        // It applies to our filtered mailboxes.
                    } else if (!mParams.mboxIds.contains(mboxId)) {
                        continue;
                    }
                }
                if (!mParams.quiet) {
                    printOp(mOut, op, mParams.hideOffset, lastOpStartOffset, lastPosition - lastOpStartOffset);
                    if (mParams.showBlob) {
                        InputStream dataStream = op.getAdditionalDataStream();
                        if (dataStream != null) {
                            mOut.println("<START OF BLOB>");
                            ByteUtil.copy(dataStream, true, mOut, false);
                            mOut.println();
                            mOut.println("<END OF BLOB>");
                        }
                    }
                }
            }
            good = true;
        } catch (IOException e) {
            // The IOException could be a real I/O problem or it could mean
            // there was a server crash previously and there were half-written
            // log entries.
            mOut.println();
            mOut.printf("Error while parsing data starting at offset 0x%08x", lastPosition);
            mOut.println();
            long size = logReader.getSize();
            long diff = size - lastPosition;
            mOut.printf("%d bytes remaining in the file", diff);
            mOut.println();
            mOut.println();
            if (op != null) {
                mOut.println("Last suceessfully parsed redo op:");
                printOp(mOut, op, false, lastOpStartOffset, lastPosition - lastOpStartOffset);
                mOut.println();
            }

            // hexdump data around the bad bytes
            int bytesPerLine = 16;
            int linesBefore = 10;
            int linesAfter = 10;
            long startPos = Math.max(lastPosition - (lastPosition % bytesPerLine) - linesBefore * bytesPerLine, 0);
            int count = (int) Math.min((linesBefore + linesAfter + 1) * bytesPerLine, lastPosition - startPos + diff);
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(logfile, "r");
                raf.seek(startPos);
                byte buf[] = new byte[count];
                raf.read(buf, 0, count);
                mOut.printf("Data near error offset %08x:", lastPosition);
                mOut.println();
                hexdump(mOut, buf, 0, count, startPos, lastPosition);
                mOut.println();
            } catch (IOException eh) {
                mOut.println("Error opening log file " + logfile.getAbsolutePath() + " for hexdump");
                eh.printStackTrace(mOut);
            } finally {
                if (raf != null)
                    raf.close();
            }
            

            throw e;
        } finally {
            logReader.close();
        }
        return good;
    }

    private static void printOp(PrintStream out, RedoableOp op, boolean hideOffset, long beginOffset, long size) {
        if (!hideOffset)
            out.printf("[%08x - %08x: %d bytes] ", beginOffset, beginOffset + size - 1, size);
        out.println(op.toString());
    }

    public boolean verifyFile(File file) {
        mOut.println("VERIFYING: " + file.getAbsolutePath());
        boolean good = false;
        try {
            good = scanLog(file);
        } catch (IOException e) {
            mBadFiles.add(new BadFile(file, e));
            mOut.println("Exception while verifying " + file.getAbsolutePath());
            e.printStackTrace(mOut);
        }
        if (!mParams.quiet)
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
        if (!mParams.quiet)
            mOut.println("VERIFYING DIRECTORY: " + dir.getAbsolutePath());
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
            mOut.println(bf.file.getAbsolutePath());
            mOut.println("    " + bf.error.getMessage());
        }
    }

    private static void hexdump(PrintStream out, byte[] data, int offset, int length, long offsetOffsetBy, long badBytePos) {
        int end = Math.min(offset + length, data.length);
        int bytesPerLine = 16;
        while (offset < end) {
            int bytes = Math.min(bytesPerLine, end - offset);  // bytes for this line
            long offsetLineStart = offset + offsetOffsetBy;
            long offsetLineEnd = offsetLineStart + bytes;
            out.printf("%08x: ", offsetLineStart);
            for (int i = 0; i < bytesPerLine; i++) {
                if (i < bytes)
                    out.printf("%02x", ((int) data[offset + i]) & 0x000000ff);
                else
                    out.print("  ");
                out.print(" ");
                if (i == 7)
                    out.print(" ");
            }
            out.print(" ");
            for (int i = 0; i < bytesPerLine; i++) {
                if (i < bytes) {
                    int ch = ((int) data[offset + i]) & 0x000000ff;
                    if (ch >= 33 && ch <= 126)  // printable ASCII range
                        out.printf("%c", (char) ch);
                    else
                        out.print(".");
                } else {
                    out.print(" ");
                }
            }

            if (offsetLineStart <= badBytePos && badBytePos < offsetLineEnd)
                out.print(" **");
            out.println();

            offset += bytes;
        }
    }
    
    public static void main(String[] cmdlineargs) {
        CliUtil.toolSetup();
        CommandLine cl = parseArgs(cmdlineargs);
        Params params = initParams(cl);
        if (params.help)
            usage(null);

        String[] args = cl.getArgs();

        if (args.length < 1)
            usage("No redolog file/directory list specified");

        boolean allGood = true;
        RedoLogVerify verify = new RedoLogVerify(params, System.out);

        for (int i = 0; i < args.length; i++) {
            File f = new File(args[i]);
            boolean good = false;
            if (f.isDirectory())
                good = verify.verifyDirectory(f);
            else
                good = verify.verifyFile(f);
            allGood = allGood && good;
        }

        if (!allGood) {
            verify.listErrors();
            System.exit(1);
        }
    }
}
