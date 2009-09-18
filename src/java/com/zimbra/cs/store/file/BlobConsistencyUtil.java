/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.store.file;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.store.file.BlobConsistencyChecker.BlobInfo;

public class BlobConsistencyUtil {

    private static final String LO_HELP = "help";
    private static final String LO_VERBOSE = "verbose";
    private static final String LO_MAILBOXES = "mailboxes";
    private static final String LO_VOLUMES = "volumes";
    private static final String LO_SKIP_SIZE_CHECK = "skip-size-check";
    
    private Options mOptions;
    private List<Long> mMailboxIds = new ArrayList<Long>();
    private List<Short> mVolumeIds = new ArrayList<Short>();
    private boolean mSkipSizeCheck = false;
    private boolean mVerbose = false;
    
    private BlobConsistencyUtil() {
        mOptions = new Options();
        
        mOptions.addOption(new Option("h", LO_HELP, false, "Display this help message."));
        mOptions.addOption(new Option("v", LO_VERBOSE, false, "Display verbose output.  Display stack trace on error."));
        mOptions.addOption(new Option(null, LO_SKIP_SIZE_CHECK, false, "Skip blob size check."));
        
        Option o = new Option(null, LO_VOLUMES, true, "Specify which volumes to check.");
        o.setArgName("volume-ids");
        mOptions.addOption(o);
        
        o = new Option("m", LO_MAILBOXES, true, "Specify which mailboxes to check");
        o.setArgName("mailbox-ids");
        mOptions.addOption(o);
    }
    
    private void usage(String errorMsg) {
        int exitStatus = 0;
        
        if (errorMsg != null) {
            System.err.println(errorMsg);
            exitStatus = 1;
        }
        HelpFormatter format = new HelpFormatter();
        format.printHelp(new PrintWriter(System.err, true), 80,
            "zmblobchk [options] start", null, mOptions, 2, 2, "\nid values are separated by commas.");
        System.exit(exitStatus);
    }

    private void parseArgs(String[] args)
    throws ParseException {
        GnuParser parser = new GnuParser();
        CommandLine cl = parser.parse(mOptions, args);
        
        if (CliUtil.hasOption(cl, LO_HELP)) {
            usage(null);
        }
        // Require the "start" command, so that someone doesn't inadvertently
        // kick of a blob check.
        if (cl.getArgs().length == 0 || !cl.getArgs()[0].equals("start")) {
            usage(null);
        }
        if (CliUtil.hasOption(cl, LO_VERBOSE)) {
            mVerbose = true;
        }
        Option opt = CliUtil.getOption(cl, LO_VOLUMES);
        if (opt != null) {
            for (String id : opt.getValue().split(",")) {
                try {
                    mVolumeIds.add(Short.parseShort(id));
                } catch (NumberFormatException e) {
                    usage("Invalid volume id: " + id);
                }
            }
        }
        opt = CliUtil.getOption(cl, LO_MAILBOXES);
        if (opt != null) {
            for (String id : opt.getValue().split(",")) {
                try {
                    mMailboxIds.add(Long.parseLong(id));
                } catch (NumberFormatException e) {
                    usage("Invalid mailbox id: " + id);
                }
            }
        }
        mSkipSizeCheck = CliUtil.hasOption(cl, LO_SKIP_SIZE_CHECK);
    }
    
    private void run()
    throws Exception {
        CliUtil.toolSetup();
        SoapProvisioning prov = SoapProvisioning.getAdminInstance();
        prov.soapZimbraAdminAuthenticate();
        XMLElement request = new XMLElement(AdminConstants.CHECK_BLOB_CONSISTENCY_REQUEST);
        for (short volumeId : mVolumeIds) {
            request.addElement(AdminConstants.E_VOLUME).addAttribute(AdminConstants.A_ID, volumeId);
        }
        for (long mboxId : mMailboxIds) {
            request.addElement(AdminConstants.E_MAILBOX).addAttribute(AdminConstants.A_ID, mboxId);
        }
        request.addAttribute(AdminConstants.A_CHECK_SIZE, !mSkipSizeCheck);
        
        Element response = prov.invoke(request);
        for (Element mboxEl : response.listElements(AdminConstants.E_MAILBOX)) {
            BlobConsistencyChecker.Results results = new BlobConsistencyChecker.Results(mboxEl);
            for (BlobInfo blob : results.missingBlobs) {
                System.out.format("Mailbox %d, item %d, rev %d, volume %d, %s: blob not found.\n",
                    results.mboxId, blob.itemId, blob.modContent, blob.volumeId, blob.path);
            }
            for (BlobInfo blob : results.incorrectSize) {
                System.out.format(
                    "Mailbox %d, item %d, rev %d, volume %d, %s: incorrect data size.  Expected %d, was %d.  File size is %d.\n",
                    results.mboxId, blob.itemId, blob.modContent, blob.volumeId, blob.path,
                    blob.dbSize, blob.fileDataSize,
                    blob.fileSize);
            }
            for (BlobInfo blob : results.unexpectedBlobs) {
                System.out.format(
                    "Mailbox %d, volume %d, %s: unexpected blob.  File size is %d.\n",
                    results.mboxId, blob.volumeId, blob.path, blob.fileSize);
            }
        }
    }

    public static void main(String[] args) {
        BlobConsistencyUtil app = new BlobConsistencyUtil();
        
        try {
            app.parseArgs(args);
        } catch (ParseException e) {
            app.usage(e.getMessage());
        }
        
        try {
            app.run();
        } catch (Exception e) {
            if (app.mVerbose) {
                e.printStackTrace(new PrintWriter(System.err, true));
            } else {
                String msg = e.getMessage();
                if (msg == null) {
                    msg = e.toString();
                }
                System.err.println(msg);
            }
        }
    }
    
}
