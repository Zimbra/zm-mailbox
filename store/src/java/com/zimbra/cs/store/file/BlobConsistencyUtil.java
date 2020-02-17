/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.store.file.BlobConsistencyChecker.BlobInfo;
import com.zimbra.soap.admin.message.ExportAndDeleteItemsRequest;
import com.zimbra.soap.admin.type.ExportAndDeleteItemSpec;
import com.zimbra.soap.admin.type.ExportAndDeleteMailboxSpec;

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
    private static final String LO_OUTPUT_USED_BLOBS = "output-used-blobs";
    private static final String LO_USED_BLOB_LIST = "used-blob-list";

    private Options options;
    private List<Integer> mailboxIds;
    private List<Short> volumeIds = new ArrayList<Short>();
    private boolean skipSizeCheck = false;
    private boolean verbose = false;
    private String unexpectedBlobList;
    private PrintWriter unexpectedBlobWriter;
    private boolean missingBlobDeleteItem = false;
    private boolean noExport = false;
    private String exportDir;
    private boolean incorrectRevisionRenameFile = false;
    private boolean outputUsedBlobs = false;
    private String usedBlobList;
    private PrintWriter usedBlobWriter;

    private BlobConsistencyUtil() {
        options = new Options();

        options.addOption(new Option("h", LO_HELP, false, "Display this help message."));
        options.addOption(new Option("v", LO_VERBOSE, false, "Display verbose output.  Display stack trace on error."));
        options.addOption(new Option(null, LO_SKIP_SIZE_CHECK, false, "Skip blob size check."));
        options.addOption(new Option(null, LO_OUTPUT_USED_BLOBS, false, "Output listing of all blobs referenced by the mailbox(es)"));

        Option o = new Option(null, LO_VOLUMES, true, "Specify which volumes to check.  If not specified, check all volumes.");
        o.setArgName("volume-ids");
        options.addOption(o);

        o = new Option("m", LO_MAILBOXES, true, "Specify which mailboxes to check.  If not specified, check all mailboxes.");
        o.setArgName("mailbox-ids");
        options.addOption(o);

        o = new Option(null, LO_UNEXPECTED_BLOB_LIST, true, "Write the paths of any unexpected blobs to a file.");
        o.setArgName("path");
        options.addOption(o);

        o = new Option(null, LO_USED_BLOB_LIST, true, "Write the paths of all used blobs to a file.");
        o.setArgName("path");
        options.addOption(o);

        options.addOption(null, LO_MISSING_BLOB_DELETE_ITEM, false, "Delete any items that have a missing blob.");

        o = new Option(null, LO_EXPORT_DIR, true, "Target directory for database export files.");
        o.setArgName("path");
        options.addOption(o);

        options.addOption(null, LO_NO_EXPORT, false, "Delete items without exporting.");
        options.addOption(new Option(null, LO_INCORRECT_REVISION_RENAME_FILE, false,
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
            "zmblobchk [options] start", null, options, 2, 2,
            "\nThe \"start\" command is required, to avoid unintentionally running a blob check.  " +
            "Id values are separated by commas.");
        System.exit(exitStatus);
    }

    private void parseArgs(String[] args)
    throws ParseException {
        GnuParser parser = new GnuParser();
        CommandLine cl = parser.parse(options, args);

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
                    volumeIds.add(Short.parseShort(id));
                } catch (NumberFormatException e) {
                    usage("Invalid volume id: " + id);
                }
            }
        }

        String mailboxList = CliUtil.getOptionValue(cl, LO_MAILBOXES);
        if (mailboxList != null) {
            mailboxIds = new ArrayList<Integer>();
            for (String id : mailboxList.split(",")) {
                try {
                    mailboxIds.add(Integer.parseInt(id));
                } catch (NumberFormatException e) {
                    usage("Invalid mailbox id: " + id);
                }
            }
        }

        skipSizeCheck = CliUtil.hasOption(cl, LO_SKIP_SIZE_CHECK);
        verbose = CliUtil.hasOption(cl, LO_VERBOSE);
        unexpectedBlobList = CliUtil.getOptionValue(cl, LO_UNEXPECTED_BLOB_LIST);
        missingBlobDeleteItem = CliUtil.hasOption(cl, LO_MISSING_BLOB_DELETE_ITEM);
        exportDir = CliUtil.getOptionValue(cl, LO_EXPORT_DIR);
        outputUsedBlobs = CliUtil.hasOption(cl, LO_OUTPUT_USED_BLOBS);
        usedBlobList = CliUtil.getOptionValue(cl, LO_USED_BLOB_LIST);

        if (missingBlobDeleteItem) {
            // --export-dir overrides --no-export
            if (exportDir == null) {
                noExport = CliUtil.hasOption(cl, LO_NO_EXPORT);
                if (!noExport) {
                    usage("Please specify either " + LO_EXPORT_DIR + " or " + LO_NO_EXPORT + " when using " + LO_MISSING_BLOB_DELETE_ITEM);
                }
            }
        }

        incorrectRevisionRenameFile = CliUtil.hasOption(cl, LO_INCORRECT_REVISION_RENAME_FILE);
    }

    private void run()
    throws Exception {
        if (unexpectedBlobList != null) {
            unexpectedBlobWriter = new PrintWriter(new FileOutputStream(unexpectedBlobList), true);
        }

        if (usedBlobList != null) {
            usedBlobWriter = new PrintWriter(new FileOutputStream(usedBlobList), true);
        }

        CliUtil.toolSetup();
        SoapProvisioning prov = SoapProvisioning.getAdminInstance();
        prov.soapZimbraAdminAuthenticate();
        if (mailboxIds == null) {
            mailboxIds = getAllMailboxIds(prov);
        }
        try {
        	DbPool.startup();
        	for (int mboxId : mailboxIds) {
        		System.out.println("Checking mailbox " + mboxId + ".");
        		checkMailbox(mboxId, prov);
        	}
        }  finally{
        	DbPool.shutdown();
        }
        if (unexpectedBlobWriter != null) {
            unexpectedBlobWriter.close();
        }

        if (usedBlobWriter != null) {
            usedBlobWriter.close();
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

    private String locatorText(BlobInfo blob) {
        if (blob.external) {
            return String.format("locator %s", blob.path);
        } else {
            return String.format("volume %d, %s", blob.volumeId, blob.path);
        }
    }

    private void checkMailbox(int mboxId, SoapProvisioning prov)
    throws ServiceException {
        XMLElement request = new XMLElement(AdminConstants.CHECK_BLOB_CONSISTENCY_REQUEST);
        for (short volumeId : volumeIds) {
            request.addElement(AdminConstants.E_VOLUME).addAttribute(AdminConstants.A_ID, volumeId);
        }
        request.addElement(AdminConstants.E_MAILBOX).addAttribute(AdminConstants.A_ID, mboxId);
        request.addAttribute(AdminConstants.A_CHECK_SIZE, !skipSizeCheck);
        request.addAttribute(AdminConstants.A_REPORT_USED_BLOBS, outputUsedBlobs || usedBlobWriter != null);

        if (prov.isExpired()) {
            prov.soapZimbraAdminAuthenticate();
        }
        Element response = prov.invoke(request);
        for (Element mboxEl : response.listElements(AdminConstants.E_MAILBOX)) {
            // Print results.
            BlobConsistencyChecker.Results results = new BlobConsistencyChecker.Results(mboxEl);
            for (BlobInfo blob : results.missingBlobs.values()) {
                System.out.format("Mailbox %d, item %d, rev %d, %s: blob not found.\n",
                    results.mboxId, blob.itemId, blob.modContent, locatorText(blob));
            }
            for (BlobInfo blob : results.incorrectSize.values()) {
                System.out.format(
                    "Mailbox %d, item %d, rev %d, %s: incorrect data size.  Expected %d, was %d.  File size is %d.\n",
                    results.mboxId, blob.itemId, blob.modContent, locatorText(blob),
                    blob.dbSize, blob.fileDataSize,
                    blob.fileSize);
            }
            for (BlobInfo blob : results.unexpectedBlobs.values()) {
                System.out.format(
                    "Mailbox %d, %s: unexpected blob.  File size is %d.\n",
                    results.mboxId, locatorText(blob), blob.fileSize);
                if (unexpectedBlobWriter != null) {
                    unexpectedBlobWriter.println(blob.path);
                }
            }
            for (BlobInfo blob : results.incorrectModContent.values()) {
                System.out.format(
                                "Mailbox %d, item %d, rev %d, %s: file has incorrect revision.\n",
                                results.mboxId, blob.itemId, blob.modContent, locatorText(blob));
            }
            for (BlobInfo blob : results.usedBlobs.values()) {
                if (outputUsedBlobs) {
                    System.out.format(
                                    "Used blob: Mailbox %d, item %d, rev %d, %s.\n",
                                    results.mboxId, blob.itemId, blob.version, locatorText(blob));
                }
                if (usedBlobWriter != null) {
                    usedBlobWriter.println(blob.path);
                }
            }

            // Fix inconsistencies.
            if (missingBlobDeleteItem && results.missingBlobs.size() > 0) {
                exportAndDelete(prov, results);
            }
            if (incorrectRevisionRenameFile) {
                for (BlobInfo blob : results.incorrectModContent.values()) {
                    File file = new File(blob.path);
                    File dir = file.getParentFile();
                    if (dir != null) {
                        File newFile = new File(dir, FileBlobStore.getFilename(blob.itemId, blob.modContent));
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

        ExportAndDeleteMailboxSpec mailbox = new ExportAndDeleteMailboxSpec(results.mboxId);
        ExportAndDeleteItemsRequest jaxbRequest = new ExportAndDeleteItemsRequest(exportDir,
                "mbox" + results.mboxId + "_", mailbox);
        for (BlobInfo blob : results.missingBlobs.values()) {
            mailbox.addItem(ExportAndDeleteItemSpec.createForIdAndVersion(blob.itemId, blob.version));
        }
        prov.invokeJaxb(jaxbRequest);
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
            if (app.verbose) {
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
