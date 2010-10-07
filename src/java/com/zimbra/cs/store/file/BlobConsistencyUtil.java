/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.store.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.service.ServiceException;
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
    private static final String LO_UNEXPECTED_BLOB_LIST = "unexpected-blob-list";
    private static final String LO_MISSING_BLOB_DELETE_ITEM = "missing-blob-delete-item";
    private static final String LO_INCORRECT_REVISION_RENAME_FILE = "incorrect-revision-rename-file";
    private static final String LO_EXPORT_DIR = "export-dir";
    private static final String LO_NO_EXPORT = "no-export";
    
    private Options mOptions;
    private List<Integer> mMailboxIds;
    private List<Short> mVolumeIds = new ArrayList<Short>();
    private boolean mSkipSizeCheck = false;
    private boolean mVerbose = false;
    private String mUnexpectedBlobList;
    private PrintWriter mUnexpectedBlobWriter;
    private boolean mMissingBlobDeleteItem = false;
    private boolean mNoExport = false;
    private String mExportDir;
    private boolean mIncorrectRevisionRenameFile = false;
    
    private BlobConsistencyUtil() {
        mOptions = new Options();
        
        mOptions.addOption(new Option("h", LO_HELP, false, "Display this help message."));
        mOptions.addOption(new Option("v", LO_VERBOSE, false, "Display verbose output.  Display stack trace on error."));
        mOptions.addOption(new Option(null, LO_SKIP_SIZE_CHECK, false, "Skip blob size check."));
        
        Option o = new Option(null, LO_VOLUMES, true, "Specify which volumes to check.  If not specified, check all volumes.");
        o.setArgName("volume-ids");
        mOptions.addOption(o);
        
        o = new Option("m", LO_MAILBOXES, true, "Specify which mailboxes to check.  If not specified, check all mailboxes.");
        o.setArgName("mailbox-ids");
        mOptions.addOption(o);
        
        o = new Option(null, LO_UNEXPECTED_BLOB_LIST, true, "Write the paths of any unexpected blobs to a file.");
        o.setArgName("path");
        mOptions.addOption(o);
        
        mOptions.addOption(null, LO_MISSING_BLOB_DELETE_ITEM, false, "Delete any items that have a missing blob.");
        
        o = new Option(null, LO_EXPORT_DIR, true, "Target directory for database export files.");
        o.setArgName("path");
        mOptions.addOption(o);
        
        mOptions.addOption(null, LO_NO_EXPORT, false, "Delete items without exporting.");
        mOptions.addOption(new Option(null, LO_INCORRECT_REVISION_RENAME_FILE, false,
            "Rename the file on disk when the revision number doesn't match."));
    }
    
    private void usage(String errorMsg) {
        int exitStatus = 0;
        
        if (errorMsg != null) {
            System.err.println(errorMsg);
            exitStatus = 1;
        }
        HelpFormatter format = new HelpFormatter();
        format.printHelp(new PrintWriter(System.err, true), 80,
            "zmblobchk [options] start", null, mOptions, 2, 2,
            "\nThe \"start\" command is required, to avoid unintentionally running a blob check.  " +
            "Id values are separated by commas.");
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
        
        String volumeList = CliUtil.getOptionValue(cl, LO_VOLUMES);
        if (volumeList != null) {
            for (String id : volumeList.split(",")) {
                try {
                    mVolumeIds.add(Short.parseShort(id));
                } catch (NumberFormatException e) {
                    usage("Invalid volume id: " + id);
                }
            }
        }
        
        String mailboxList = CliUtil.getOptionValue(cl, LO_MAILBOXES);
        if (mailboxList != null) {
            mMailboxIds = new ArrayList<Integer>();
            for (String id : mailboxList.split(",")) {
                try {
                    mMailboxIds.add(Integer.parseInt(id));
                } catch (NumberFormatException e) {
                    usage("Invalid mailbox id: " + id);
                }
            }
        }

        mSkipSizeCheck = CliUtil.hasOption(cl, LO_SKIP_SIZE_CHECK);
        mVerbose = CliUtil.hasOption(cl, LO_VERBOSE);
        mUnexpectedBlobList = CliUtil.getOptionValue(cl, LO_UNEXPECTED_BLOB_LIST);
        mMissingBlobDeleteItem = CliUtil.hasOption(cl, LO_MISSING_BLOB_DELETE_ITEM);
        mExportDir = CliUtil.getOptionValue(cl, LO_EXPORT_DIR);
        
        if (mMissingBlobDeleteItem) {
            // --export-dir overrides --no-export
            if (mExportDir == null) {
                mNoExport = CliUtil.hasOption(cl, LO_NO_EXPORT);
                if (!mNoExport) {
                    usage("Please specify either " + LO_EXPORT_DIR + " or " + LO_NO_EXPORT + " when using " + LO_MISSING_BLOB_DELETE_ITEM);
                }
            }
        }
        
        mIncorrectRevisionRenameFile = CliUtil.hasOption(cl, LO_INCORRECT_REVISION_RENAME_FILE);
    }
    
    private void run()
    throws Exception {
        if (mUnexpectedBlobList != null) {
            mUnexpectedBlobWriter = new PrintWriter(new FileOutputStream(mUnexpectedBlobList), true);
        }
        
        CliUtil.toolSetup();
        SoapProvisioning prov = SoapProvisioning.getAdminInstance();
        prov.soapZimbraAdminAuthenticate();
        if (mMailboxIds == null) {
            mMailboxIds = getAllMailboxIds(prov);
        }
        for (int mboxId : mMailboxIds) {
            System.out.println("Checking mailbox " + mboxId + ".");
            checkMailbox(mboxId, prov);
        }
        
        if (mUnexpectedBlobWriter != null) {
            mUnexpectedBlobWriter.close();
        }
    }
    
    private List<Integer> getAllMailboxIds(SoapProvisioning prov)
    throws ServiceException {
        List<Integer> ids = new ArrayList<Integer>();
        XMLElement request = new XMLElement(AdminConstants.GET_ALL_MAILBOXES_REQUEST);
        Element response = prov.invoke(request);
        for (Element mboxEl : response.listElements(AdminConstants.E_MAILBOX)) {
            ids.add((int) mboxEl.getAttributeLong(AdminConstants.A_ID));
        }
        return ids;
    }
    
    private void checkMailbox(int mboxId, SoapProvisioning prov)
    throws ServiceException {
        XMLElement request = new XMLElement(AdminConstants.CHECK_BLOB_CONSISTENCY_REQUEST);
        for (short volumeId : mVolumeIds) {
            request.addElement(AdminConstants.E_VOLUME).addAttribute(AdminConstants.A_ID, volumeId);
        }
        request.addElement(AdminConstants.E_MAILBOX).addAttribute(AdminConstants.A_ID, mboxId);
        request.addAttribute(AdminConstants.A_CHECK_SIZE, !mSkipSizeCheck);
        
        Element response = prov.invoke(request);
        for (Element mboxEl : response.listElements(AdminConstants.E_MAILBOX)) {
            // Print results.
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
                if (mUnexpectedBlobWriter != null) {
                    mUnexpectedBlobWriter.println(blob.path);
                }
            }
            for (BlobInfo blob : results.incorrectModContent) {
                System.out.format(
                    "Mailbox %d, item %d, rev %d, volume %d, %s: file has incorrect revision.\n",
                    results.mboxId, blob.itemId, blob.modContent, blob.volumeId, blob.path);
            }
            
            // Fix inconsistencies.
            if (mMissingBlobDeleteItem && results.missingBlobs.size() > 0) {
                exportAndDelete(prov, results);
            }
            if (mIncorrectRevisionRenameFile) {
                for (BlobInfo blob : results.incorrectModContent) {
                    File file = new File(blob.path);
                    File dir = file.getParentFile();
                    if (dir != null) {
                        File newFile = new File(dir, FileBlobStore.getFilename((int) blob.itemId, blob.modContent));
                        System.out.format("Renaming %s to %s.\n", file.getAbsolutePath(), newFile.getAbsolutePath());
                        if (!file.renameTo(newFile)) {
                            System.err.format("Unable to rename %s to %s.\n", file.getAbsolutePath(), newFile.getAbsolutePath());
                        }
                    } else {
                        System.err.format("Could not determine parent directory of %s.\n", file.getAbsolutePath());
                    }
                }
            }
        }
    }
    
    private void exportAndDelete(SoapProvisioning prov, BlobConsistencyChecker.Results results)
    throws ServiceException {
        System.out.format("Deleting %d items from mailbox %d.\n", results.missingBlobs.size(), results.mboxId);
        
        Element request = new XMLElement(AdminConstants.EXPORT_AND_DELETE_ITEMS_REQUEST);
        request.addAttribute(AdminConstants.A_EXPORT_DIR, mExportDir);
        request.addAttribute(AdminConstants.A_EXPORT_FILENAME_PREFIX, "mbox" + results.mboxId + "_");
        Element mboxEl = request.addElement(AdminConstants.E_MAILBOX).addAttribute(AdminConstants.A_ID, results.mboxId);
        for (BlobInfo blob : results.missingBlobs) {
            mboxEl.addElement(AdminConstants.E_ITEM).addAttribute(AdminConstants.A_ID, blob.itemId);
        }
        prov.invoke(request);
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
            System.exit(1);
        }
    }
    
}
