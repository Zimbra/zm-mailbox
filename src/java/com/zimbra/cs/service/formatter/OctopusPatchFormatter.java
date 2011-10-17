package com.zimbra.cs.service.formatter;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

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
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;

/**
 * Formatter for octopus patches
 *
 * @author grzes
*/
public class OctopusPatchFormatter extends Formatter
{
    private static final Log log = LogFactory.getLog(OctopusPatchFormatter.class);

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
    @SuppressWarnings("unused")
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

        try {
            pis = PatchInputStream.create(
                    context.getRequestInputStream(),
                    // use targetMailbox here, patch will refer only to files in
                    // the current (target) user's view
                    context.targetMailbox,
                    context.opContext,
                    item.getId(),
                    item.getVersion());

            String creator = context.getAuthAccount() == null ? null : context.getAuthAccount().getName();

            if (contentType == null) {
                contentType = MimeDetect.getMimeDetect().detect(filename);
                if (contentType == null)
                    contentType = MimeConstants.CT_APPLICATION_OCTET_STREAM;
            }

            log.debug("Creating parsed document; filename=" + filename + ", contentType=" + contentType
                    + ", creator=" + creator);

            ParsedDocument pd = new ParsedDocument(
                    pis,
                    filename,
                    contentType,
                    System.currentTimeMillis(),
                    creator,
                    context.req.getHeader("X-Zimbra-Description"));

            log.debug("Parsed document created " + filename);

            if (item == null) {
                log.debug("Creating new document " + filename);
                item = mbox.createDocument(context.opContext, folder.getId(), pd, MailItem.Type.DOCUMENT, 0);
            } else {
                log.debug("Creating new version of the document " + filename + ", current version: " +
                        item.getVersion());
                item = mbox.addDocumentRevision(context.opContext, item.getId(), pd);
            }

            NativeFormatter.sendZimbraHeaders(context.resp, item);

        } catch (PatchException e) {
            log.error("Patch upload failed: " + e);
            throw new UserServletException(HttpServletResponse.SC_CONFLICT,
                    "patch cannot be applied, try uploading whole file", e);
        } finally {
            pis.close();
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
