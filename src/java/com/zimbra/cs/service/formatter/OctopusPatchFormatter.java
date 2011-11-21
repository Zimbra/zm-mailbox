package com.zimbra.cs.service.formatter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;
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
import com.zimbra.cs.octosync.store.PatchStore.StoredPatch;
import com.zimbra.cs.octosync.store.StoreManagerBasedTempBlobStore;
import com.zimbra.cs.service.UserServlet;
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

        try {
            ip = patchStore.createIncomingPatch(context.targetAccount.getId());

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
        if (!(context.target instanceof Document)) {
            throw UserServletException.notImplemented("can only handle documents");
        }

        Document doc = (Document)context.target;

        String v = context.params.get(UserServlet.QP_VERSION);
        int version = v != null ? Integer.parseInt(v) : -1;

        if (log.isDebugEnabled()) {
            log.debug("Request received for patch for " + doc.getName() + ", id: " + doc.getId() +
                    (version == -1 ? ", latest version" : (", version: " + version)));
        }

        NativeFormatter.sendZimbraHeaders(context.resp, context.target);

        HttpUtil.Browser browser = HttpUtil.guessBrowser(context.req);
        if (browser == HttpUtil.Browser.IE) {
            context.resp.addHeader("X-Content-Type-Options", "nosniff"); // turn off content detection..
        }

        if (version > 0) {
            doc = (Document)doc.getMailbox().getItemRevision(context.opContext, doc.getId(), doc.getType(), version);
        } else {
            version = doc.getVersion();

            if (log.isDebugEnabled()) {
                log.debug("Latest version of " + doc.getName() + " is " + version);
            }
        }

        StoredPatch sp = patchStore.lookupPatch(context.targetAccount.getId(), doc.getId(), version);

        if (sp != null) {
            sendPatch(context, doc, version, sp);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Patch not available for " + doc.getName() + ", id: " + doc.getId() +
                        ", version: " + version + "; will return the entire file");
            }
            sendFullFile(context, doc, version);
        }
    }

    private void sendPatch(UserServletContext context, Document doc, int version, StoredPatch sp) throws IOException, ServiceException
    {
        InputStream patchIs = null;
        InputStream manifestIs = null;

        String manifestParam = context.params.get(UserServlet.QP_MANIFEST);
        // send manifest by default
        final boolean sendManifest = (manifestParam != null) ? (Integer.parseInt(manifestParam) > 0) : true;

        try {
            patchIs = sp.getInputStream();
            if (sendManifest) {
                manifestIs = sp.getManifestInputStream();
            }
        } catch (FileNotFoundException e) {
            log.warn("Cannot access patch for " + doc.getName() + ", id: " + doc.getId() + ", version: " + version +
                    "; will return the entire file (failure: " + e + ")");

            sendFullFile(context, doc, version);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Sending patch for " + doc.getName() + ", id: " + doc.getId() + ", version: " + version);
        }

        // tell the client we are indeed sending a patch
        context.resp.addIntHeader("X-Octopus-Patch", 1);

        InputStream is = null;

        if (sendManifest) {
            context.resp.setContentLength((int)(sp.getPatchSize() + sp.getManifestSize()));
            assert manifestIs != null;
            is = new SequenceInputStream(manifestIs, patchIs);
        } else {
            context.resp.setContentLength((int)sp.getPatchSize());
            is = patchIs;
        }

        ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
    }

    private void sendFullFile(UserServletContext context, Document doc, int version) throws ServiceException, IOException
    {
        // tell the client we are not sending a patch
        context.resp.addIntHeader("X-Octopus-Patch", 0);

        // @todo casting long to int is potentially a problem here
        // for files over 2 GB
        // Sun/Oracle apparently refuses to fix it
        // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4187336
        // I guess we could generate this header manually
        context.resp.setContentLength((int)doc.getSize());

        InputStream is = doc.getContentStream();
        ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
    }

}
