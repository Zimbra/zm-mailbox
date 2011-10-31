package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.octosync.PatchException;
import com.zimbra.cs.octosync.PatchInputStream;
import com.zimbra.cs.octosync.store.BlobStore;
import com.zimbra.cs.octosync.store.PatchStore;
import com.zimbra.cs.octosync.store.StoreManagerBasedTempBlobStore;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

/**
 * Formatter for octopus patches
 *
 * @author grzes
*/
public class OctopusPatchFormatter extends Formatter
{
    private static final Log log = LogFactory.getLog(OctopusPatchFormatter.class);

    private PatchStore patchStore;

    OctopusPatchFormatter()
    {
        long incomingPatchExpiration = LC.octopus_incoming_patch_max_age.intValue() * 60 * 1000;
        long storedPatchExpiration = LC.octopus_stored_patch_max_age.intValue() * 60 * 1000;

        BlobStore blobStore = new StoreManagerBasedTempBlobStore(StoreManager.getInstance(),
                incomingPatchExpiration, storedPatchExpiration);

        patchStore = new PatchStore(blobStore);
    }

    // Formatter API
    @Override
    public FormatType getType()
    {
        return FormatType.OPATCH;
    }

    // Formatter API
    @Override
    public boolean supportsSave()
    {
        return true;
    }

    // Formatter API
    @Override
    public void saveCallback(UserServletContext context,
            String contentType,
            Folder folder,
            String filename) throws IOException, ServiceException, UserServletException
    {
        log.info("Uploading patch for " + filename);

        if (filename == null) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "Missing filename");
        }

        MailItem item = null;
        Mailbox mbox = folder.getMailbox();

        try {
            item = mbox.getItemByPath(context.opContext, filename, folder.getId());

            if (!(item instanceof Document))
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                        "cannot overwrite existing object at that path");
        } catch (NoSuchItemException e) {
            log.debug("No document found at " + filename + "(folder id=" + folder.getId() +
                "; will create new one");
        }

        PatchInputStream pis = null;
        PatchStore.IncomingPatch ip = null;

        // @todo Temporary, this is likely to be supplied by the client
        // depending how we go about supporting resumable download
        // for now just get a random uuid
        String resumeId = UUID.randomUUID().toString();

        try {
            ip = patchStore.createIncomingPatch(context.targetAccount.getId(), resumeId);

            int defaultFileId = 0;
            int defaultVersion = 0;

            if (item != null) {
                defaultFileId = item.getId();
                defaultVersion = item.getVersion();
            }

            pis = PatchInputStream.create(
                    context.getRequestInputStream(),
                    // use targetMailbox here, patch will refer only to files in
                    // the current (target) user's view
                    context.targetMailbox,
                    context.opContext,
                    defaultFileId,
                    defaultVersion,
                    ip.getOutputStream(),
                    ip.getManifest());

            String creator = context.getAuthAccount() == null ? null : context.getAuthAccount().getName();

            if (contentType == null) {
                contentType = MimeDetect.getMimeDetect().detect(filename);
                if (contentType == null)
                    contentType = MimeConstants.CT_APPLICATION_OCTET_STREAM;
            }

            log.debug("Storing blob");
            Blob blob = StoreManager.getInstance().storeIncoming(pis, null);

            log.debug("Creating parsed document; filename=" + filename + ", contentType=" + contentType
                    + ", creator=" + creator);

            ParsedDocument pd = new ParsedDocument(
                    blob,
                    filename,
                    contentType,
                    System.currentTimeMillis(),
                    creator,
                    context.req.getHeader("X-Zimbra-Description"),
                    true);

            log.debug("Parsed document created " + filename);

            if (item == null) {
                log.debug("Creating new document " + filename);
                item = mbox.createDocument(context.opContext, folder.getId(), pd, MailItem.Type.DOCUMENT, 0);
            } else {
                log.debug("Creating new version of the document " + filename + ", current version: " +
                        item.getVersion());
                item = mbox.addDocumentRevision(context.opContext, item.getId(), pd);
            }

            patchStore.acceptPatch(ip, item.getId(), item.getVersion());

            NativeFormatter.sendZimbraHeaders(context.resp, item);

        } catch (PatchException e) {
            log.error("Patch upload failed: " + e);

            patchStore.rejectPatch(ip);

            throw new UserServletException(HttpServletResponse.SC_CONFLICT,
                    "patch cannot be applied, try uploading whole file", e);
        } finally {
            try {
                pis.close();
            } catch (Exception e) {
                log.error("Exception during PatchInputStream close, ignored: " + e);
            }
        }
    }

    // Formatter API
    @Override
    public void formatCallback(UserServletContext context) throws UserServletException,
            ServiceException, IOException, ServletException
    {
        throw new UserServletException(HttpServletResponse.SC_FORBIDDEN, "Not implemented yet");
    }
}
