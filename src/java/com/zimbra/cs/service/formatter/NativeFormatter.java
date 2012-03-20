/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 VMware, Inc.
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
package com.zimbra.cs.service.formatter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpException;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.ImageUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.html.BrowserDefang;
import com.zimbra.cs.html.DefangFactory;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.octosync.store.BlobStore;
import com.zimbra.cs.octosync.store.BlobStore.IncomingBlob;
import com.zimbra.cs.octosync.store.StoreManagerBasedTempBlobStore;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.http.ContentRange;

public final class NativeFormatter extends Formatter {

    private static final String CONVERSION_PATH = "/extension/convertd";
    public static final String ATTR_INPUTSTREAM = "inputstream";
    public static final String ATTR_MSGDIGEST  = "msgdigest";
    public static final String ATTR_FILENAME  = "filename";
    public static final String ATTR_CONTENTURL = "contenturl";
    public static final String ATTR_CONTENTTYPE = "contenttype";
    public static final String ATTR_CONTENTLENGTH = "contentlength";

    public static final String RESUME_ID_HEADER = "X-Zimbra-ResumeId";

    private static final Log log = LogFactory.getLog(NativeFormatter.class);

    private static final Set<String> SCRIPTABLE_CONTENT_TYPES = ImmutableSet.of(MimeConstants.CT_TEXT_HTML,
                                                                                MimeConstants.CT_APPLICATION_XHTML,
                                                                                MimeConstants.CT_TEXT_XML,
                                                                                MimeConstants.CT_IMAGE_SVG);
    /** Used for resumable upload of documents */
    private final StoreManagerBasedTempBlobStore blobStore;

    /** Constructor */
    public NativeFormatter()
    {
        final long incomingExpiration = LC.document_incoming_max_age.intValue() * 60 * 1000;

        blobStore = new StoreManagerBasedTempBlobStore(StoreManager.getInstance(),
                incomingExpiration,
                0 /* no expiration for "stored blobs", we don't use them here */);
    }

    @Override
    public FormatType getType() {
        return FormatType.HTML_CONVERTED;
    }

    @Override
    public Set<MailItem.Type> getDefaultSearchTypes() {
        // TODO: all?
        return EnumSet.of(MailItem.Type.MESSAGE);
    }

    @Override
    public void formatCallback(UserServletContext context) throws IOException, ServiceException, UserServletException, ServletException {
        try {
            sendZimbraHeaders(context.resp, context.target);
            HttpUtil.Browser browser = HttpUtil.guessBrowser(context.req);
            if (browser == HttpUtil.Browser.IE) {
                context.resp.addHeader("X-Content-Type-Options", "nosniff"); // turn off content detection..
            }
            if (context.target instanceof Message) {
                handleMessage(context, (Message) context.target);
            } else if (context.target instanceof CalendarItem) {
                // Don't return private appointments/tasks if the requester is not the mailbox owner.
                CalendarItem calItem = (CalendarItem) context.target;
                if (calItem.isPublic() || calItem.allowPrivateAccess(
                        context.getAuthAccount(), context.isUsingAdminPrivileges())) {
                    handleCalendarItem(context, calItem);
                } else {
                    context.resp.sendError(HttpServletResponse.SC_FORBIDDEN, "permission denied");
                }
            } else if (context.target instanceof Document) {
                handleDocument(context, (Document) context.target);
            } else if (context.target instanceof Contact) {
                handleContact(context, (Contact) context.target);
            } else {
                throw UserServletException.notImplemented("can only handle messages/appointments/tasks/documents");
            }
        } catch (MessagingException me) {
            throw ServiceException.FAILURE(me.getMessage(), me);
        }
    }

    private void handleMessage(UserServletContext context, Message msg) throws IOException, ServiceException, MessagingException, ServletException {
        if (context.hasBody()) {
            List<MPartInfo> parts = Mime.getParts(msg.getMimeMessage());
            MPartInfo body = Mime.getTextBody(parts, false);
            if (body != null) {
                handleMessagePart(context, body.getMimePart(), msg);
            } else {
                context.resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "body not found");
            }
        } else if (context.hasPart()) {
            MimePart mp = getMimePart(msg, context.getPart());
            handleMessagePart(context, mp, msg);
        } else {
            context.resp.setContentType(MimeConstants.CT_TEXT_PLAIN);
            long size = msg.getSize();
            if (size > 0)
                context.resp.setContentLength((int)size);
            InputStream is = msg.getContentStream();
            ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
        }
    }

    private void handleCalendarItem(UserServletContext context, CalendarItem calItem) throws IOException, ServiceException, MessagingException, ServletException {
        if (context.hasPart()) {
            MimePart mp;
            if (context.itemId.hasSubpart()) {
                MimeMessage mbp = calItem.getSubpartMessage(context.itemId.getSubpartId());
                mp = Mime.getMimePart(mbp, context.getPart());
            } else {
                mp = getMimePart(calItem, context.getPart());
            }
            handleMessagePart(context, mp, calItem);
        } else {
            context.resp.setContentType(MimeConstants.CT_TEXT_PLAIN);
            InputStream is = calItem.getRawMessage();
            if (is != null)
                ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
        }
    }

    private void handleContact(UserServletContext context, Contact con) throws IOException, ServiceException, MessagingException, ServletException {
        if (!con.hasAttachment()) {
            context.resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "body not found");
        } else if (context.hasPart()) {
            MimePart mp = Mime.getMimePart(con.getMimeMessage(false), context.getPart());
            handleMessagePart(context, mp, con);
        } else {
            context.resp.setContentType(MimeConstants.CT_TEXT_PLAIN);
            InputStream is = new ByteArrayInputStream(con.getContent());
            ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
        }
    }

    private static final String HTML_VIEW = "html";

    private void handleMessagePart(UserServletContext context, MimePart mp, MailItem item) throws IOException, MessagingException, ServletException {
        if (mp == null) {
            context.resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "part not found");
        } else {
            String contentType = mp.getContentType();
            String shortContentType = Mime.getContentType(mp);

            if (contentType == null) {
                contentType = MimeConstants.CT_TEXT_PLAIN;
            } else if (shortContentType.equalsIgnoreCase(MimeConstants.CT_APPLICATION_OCTET_STREAM)) {
                if ((contentType = MimeDetect.getMimeDetect().detect(Mime.getFilename(mp), mp.getInputStream())) == null)
                    contentType = MimeConstants.CT_APPLICATION_OCTET_STREAM;
                else
                    shortContentType = contentType;
            }
            // CR or LF in Content-Type causes Chrome to barf, unfortunately
            contentType = contentType.replace('\r', ' ').replace('\n', ' ');

            // IE displays garbage if the content-type header is too long
            HttpUtil.Browser browser = HttpUtil.guessBrowser(context.req);
            if (browser == HttpUtil.Browser.IE && contentType.length() > 80)
                contentType = shortContentType;

            boolean html = checkGlobalOverride(Provisioning.A_zimbraAttachmentsViewInHtmlOnly,
                    context.getAuthAccount()) || (context.hasView() && context.getView().equals(HTML_VIEW));
            InputStream in = null;
            try {
                if (!html || ExtensionUtil.getExtension("convertd") == null || contentType.startsWith(MimeConstants.CT_TEXT_HTML)) {
                    byte[] data = null;

                    // If this is an image that exceeds the max size, resize it.  Don't resize
                    // gigantic images because ImageIO reads image content into memory.
                    if (context.hasMaxWidth() && (Mime.getSize(mp) < LC.max_image_size_to_resize.intValue())) {
                        try {
                            data = getResizedImageData(mp, context.getMaxWidth());
                        } catch (Exception e) {
                            log.info("Unable to resize image.  Returning original content.", e);
                        }
                    }

                    // Return the data, or resized image if available.
                    long size;
                    if (data != null) {
                        in = new ByteArrayInputStream(data);
                        size = data.length;
                    } else {
                        in = mp.getInputStream();
                        String enc = mp.getEncoding();
                        if (enc != null) {
                            enc = enc.toLowerCase();
                        }
                        size = enc == null || enc.equals("7bit") || enc.equals("8bit") || enc.equals("binary") ? mp.getSize() : 0;
                    }
                    String defaultCharset = context.targetAccount.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, null);
                    sendbackOriginalDoc(in, contentType, defaultCharset, Mime.getFilename(mp), mp.getDescription(), size, context.req, context.resp);
                } else {
                    in = mp.getInputStream();
                    handleConversion(context, in, Mime.getFilename(mp), contentType, item.getDigest(), mp.getSize());
                }
            } finally {
                ByteUtil.closeStream(in);
            }
        }
    }

    /**
     * If the image stored in the {@code MimePart} exceeds the given width,
     * shrinks the image and returns the shrunk data.  If the
     * image width is smaller than {@code maxWidth} or resizing is not supported,
     * returns {@code null}.
     */
    private static byte[] getResizedImageData(MimePart mp, int maxWidth)
    throws IOException, MessagingException {
        ImageReader reader = null;
        ImageWriter writer = null;
        InputStream in = null;

        try {
            // Get ImageReader for stream content.
            reader = ImageUtil.getImageReader(Mime.getContentType(mp), mp.getFileName());
            if (reader == null) {
                log.debug("No ImageReader available.");
                return null;
            }

            // Read message content.
            in = mp.getInputStream();
            reader.setInput(new MemoryCacheImageInputStream(in));
            BufferedImage img = reader.read(0);
            if (img.getWidth() <= maxWidth) {
                log.debug("Image width %d is less than max %d.  Not resizing.", img.getWidth(), maxWidth);
                return null;
            }

            // Resize.
            writer = ImageIO.getImageWriter(reader);
            if (writer == null) {
                log.debug("No ImageWriter available.");
                return null;
            }
            int height = (int) ((double) maxWidth / (double) img.getWidth() * img.getHeight());
            BufferedImage small = ImageUtil.resize(img, maxWidth, height);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writer.setOutput(new MemoryCacheImageOutputStream(out));
            writer.write(small);
            return out.toByteArray();
        } finally {
            ByteUtil.closeStream(in);
            if (reader != null) {
                reader.dispose();
            }
            if (writer != null) {
                writer.dispose();
            }
        }


    }

    private void handleDocument(UserServletContext context, Document doc) throws IOException, ServiceException, ServletException {
        String v = context.params.get(UserServlet.QP_VERSION);
        int version = v != null ? Integer.parseInt(v) : -1;
        String contentType = doc.getContentType();

        doc = (version > 0 ? (Document)doc.getMailbox().getItemRevision(context.opContext, doc.getId(), doc.getType(), version) : doc);
        InputStream is = doc.getContentStream();
        // If the view is html and the convertd extension is deployed
        if (HTML_VIEW.equals(context.getView()) && ExtensionUtil.getExtension("convertd") != null && !(contentType != null && contentType.startsWith(MimeConstants.CT_TEXT_HTML))) {
            handleConversion(context, is, doc.getName(), doc.getContentType(), doc.getDigest(), doc.getSize());
        } else {
            String defaultCharset = context.targetAccount.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, null);
            boolean neuter = doc.getAccount().getBooleanAttr(Provisioning.A_zimbraNotebookSanitizeHtml, true);
            if (neuter)
                sendbackOriginalDoc(is, contentType, defaultCharset, doc.getName(), null, doc.getSize(), context.req, context.resp);
            else
                sendbackBinaryData(context.req, context.resp, is, contentType, null, doc.getName(), doc.getSize());
        }
    }

    private void handleConversion(UserServletContext ctxt, InputStream is, String filename, String ct, String digest, long length) throws IOException, ServletException {
        try {
            ctxt.req.setAttribute(ATTR_INPUTSTREAM, is);
            ctxt.req.setAttribute(ATTR_MSGDIGEST, digest);
            ctxt.req.setAttribute(ATTR_FILENAME, filename);
            ctxt.req.setAttribute(ATTR_CONTENTTYPE, ct);
            ctxt.req.setAttribute(ATTR_CONTENTURL, ctxt.req.getRequestURL().toString());
            ctxt.req.setAttribute(ATTR_CONTENTLENGTH, length);
            RequestDispatcher dispatcher = ctxt.req.getRequestDispatcher(CONVERSION_PATH);
            dispatcher.forward(ctxt.req, ctxt.resp);
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    public static MimePart getMimePart(CalendarItem calItem, String part) throws IOException, MessagingException, ServiceException {
        return Mime.getMimePart(calItem.getMimeMessage(), part);
    }

    public static MimePart getMimePart(Message msg, String part) throws IOException, MessagingException, ServiceException {
        return Mime.getMimePart(msg.getMimeMessage(), part);
    }

    public static void sendbackOriginalDoc(InputStream is, String contentType, String defaultCharset, String filename,
            String desc, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        sendbackOriginalDoc(is, contentType, defaultCharset, filename, desc, 0, req, resp);
    }

    private static void sendbackOriginalDoc(InputStream is, String contentType, String defaultCharset, String filename,
            String desc, long size, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String disp = req.getParameter(UserServlet.QP_DISP);
        disp = (disp == null || disp.toLowerCase().startsWith("i")) ? Part.INLINE : Part.ATTACHMENT;
        if (desc != null) {
            resp.addHeader("Content-Description", desc);
        }
        // defang when the html and svg attachment was requested with disposition inline
        if (disp.equals(Part.INLINE) && isScriptableContent(contentType)) {
            BrowserDefang defanger = DefangFactory.getDefanger(contentType);
            String content = defanger.defang(Mime.getTextReader(is, contentType, defaultCharset), false);
            resp.setContentType(contentType);
            String charset = Mime.getCharset(contentType);
            resp.setCharacterEncoding(Strings.isNullOrEmpty(charset) ? Charsets.UTF_8.name() : charset);
            if (!content.isEmpty()) {
                resp.setContentLength(content.length());
            }
            resp.getWriter().write(content);
        } else {
            // flash attachment may contain a malicious script hence..
            if (contentType.startsWith(MimeConstants.CT_APPLICATION_SHOCKWAVE_FLASH)) {
                disp = Part.ATTACHMENT;
            }
            resp.setContentType(contentType);
            sendbackBinaryData(req, resp, is, contentType, disp, filename, size);
        }
    }


    @Override
    public boolean supportsSave() {
        return true;
    }

    @Override
    public void saveCallback(UserServletContext context, String contentType, Folder folder, String filename)
            throws IOException, ServiceException, UserServletException
    {
        InputStream is = context.getRequestInputStream();

        try {
            ContentRange contentRange = null;

            try {
                contentRange = ContentRange.parse(context.req);
            } catch (HttpException e) {
                throw new UserServletException(e.getStatus(), "invalid Content-Range header");
            }

            final String resumeId = context.req.getHeader(RESUME_ID_HEADER);
            Mailbox mbox = folder.getMailbox();

            if (filename == null) {
                try {
                    if (contentRange != null && !contentRange.isComplete()) {
                        throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                        "incomplete upload of messages unsupported");
                    }

                    ParsedMessage pm = new ParsedMessage(context.getPostBody(), mbox.attachmentsIndexingEnabled());
                    DeliveryOptions dopt = new DeliveryOptions().setFolderId(folder).setNoICal(true);
                    mbox.addMessage(context.opContext, pm, dopt, null);
                    return;
                } catch (ServiceException e) {
                    throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "error parsing message");
                }
            }

            if (resumeId == null) {
                startNewUpload(context, is, contentType, contentRange, folder, filename);
            } else {
                BlobStore.IncomingBlob ib = blobStore.getIncoming(resumeId);

                if (ib == null) {
                    throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                            "unrecognized resume id: " + resumeId);
                }

                Object ibCtx = ib.getContext();

                // @todo seems like authenticated account can be null - should even allow
                // uploads in this case? exiting non-resumable code apparently allows that
                if (context.getAuthAccount() != null && (!(ibCtx instanceof String) ||
                        (String)ibCtx != context.getAuthAccount().getId())) {
                    // @todo Should we report "forbidden" here? this is a case of someone
                    // passing valid but stolen resume id (super rare)
                    throw new UserServletException(HttpServletResponse.SC_FORBIDDEN,
                        "invalid resume id");
                }

                resumeUpload(context, is, contentType, contentRange, resumeId, folder, filename, ib);
            }

        } finally {
            is.close();
        }
    }

    private void saveDocument(Blob blob, UserServletContext context, String contentType, Folder folder, String filename)
        throws IOException, ServiceException, UserServletException
    {
        Mailbox mbox = folder.getMailbox();
        MailItem item = null;

        String creator = context.getAuthAccount() == null ? null : context.getAuthAccount().getName();
        ParsedDocument pd = null;

        try {
            if (contentType == null) {
                contentType = MimeDetect.getMimeDetect().detect(filename);
                if (contentType == null)
                    contentType = MimeConstants.CT_APPLICATION_OCTET_STREAM;
            }

            pd = new ParsedDocument(blob, filename, contentType, System.currentTimeMillis(), creator,
                    context.req.getHeader("X-Zimbra-Description"), true);

            item = mbox.getItemByPath(context.opContext, filename, folder.getId());
            // XXX: should we just overwrite here instead?
            if (!(item instanceof Document))
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "cannot overwrite existing object at that path");

            item = mbox.addDocumentRevision(context.opContext, item.getId(), pd);
        } catch (NoSuchItemException nsie) {
            item = mbox.createDocument(context.opContext, folder.getId(), pd, MailItem.Type.DOCUMENT, 0);
        }

        sendZimbraHeaders(context.resp, item);
    }

    private static long getContentLength(HttpServletRequest req)
    {
        // note HttpServletRequest.getContentLength() returns int, that's
        // why we parse it ourselves
        final String contentLengthStr = req.getHeader("Content-Length");
        return contentLengthStr != null ? Long.parseLong(contentLengthStr) : -1;
    }

    private void startNewUpload(
            UserServletContext context,
            InputStream is,
            String contentType,
            ContentRange contentRange,
            Folder folder,
            String filename) throws IOException, ServiceException, UserServletException
    {
        if (contentRange == null) {
            // No content range so no resumable upload requested nor possible. Let's
            // just go strait to the blob store (note, we don't respond until uploaded is
            // completed, so we don't have opportunity to return the resume id to the
            // client before he completes upload. Well, in theory we do if client was
            // reading response headers asynchronously, but we're not supporting this
            // for now.

            Blob blob = StoreManager.getInstance().storeIncoming(is, null);
            saveDocument(blob, context, contentType, folder, filename);
            return;
        }

        // if actual range is given, it must start at 0
        if (contentRange.hasStartEnd() && contentRange.getStart() != 0) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                    "upload at non-zero offset without resume id");
        }

        // @todo not sure we should allow non-authenticated requests?
        String authAccountId = context.getAuthAccount() != null ? context.getAuthAccount().getId() : null;
        BlobStore.IncomingBlob ib = blobStore.createIncoming(authAccountId);
        final String resumeId = ib.getId();

        if (contentRange.hasInstanceLength()) {
            ib.setExpectedSize(contentRange.getInstanceLength());
        }

        OutputStream out = ib.getAppendingOutputStream();

        try {
            // please note, we may copy zero bytes here if client uses PUT to
            ByteUtil.copy(is, true, out, true);
        } catch (IOException e) {
            blobStore.deleteIncoming(ib);
            throw e;
        }

        // everything from the request has been written ok, now see if it's a complete upload

        if (ib.isComplete()) {
            Blob b = blobStore.extractIncoming(ib);
            saveDocument(b, context, contentType, folder, filename);
        } else {
            context.resp.addHeader(RESUME_ID_HEADER, resumeId);
        }
    }

    private void resumeUpload(
            UserServletContext context,
            InputStream is,
            String contentType,
            ContentRange contentRange,
            String resumeId,
            Folder folder,
            String filename,
            IncomingBlob ib) throws UserServletException, ServiceException, IOException
    {
        assert resumeId != null : "Resume id must be provided";

        if (contentRange == null) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                "cannot resume upload without Content-Range");
        }

        final long contentLength = getContentLength(context.req);

        if (contentLength == -1) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                "cannot resume upload without Content-Length");
        } else if (contentLength > 0) {
            if (!contentRange.hasStartEnd() || contentRange.getStart() != ib.getCurrentSize()) {

                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                    "upload must resume at the end offset (" + ib.getCurrentSize() + ")");
            }
        } else {
            if (contentRange.hasStartEnd()) {
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                        "Can't specify byte range with zero Content-Length");
            }
            // note it's okay if nothing or only instance length is supplied in Content-Range
            // without payload - it could be the final "closing" request -
        }

        if (contentRange.hasStartEnd() && contentLength != contentRange.getRangeLength()) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                "Content-Range and Content-Length mismatched");
        }

        if (contentRange.hasInstanceLength()) {
            final long instanceLength = contentRange.getInstanceLength();

            if (!ib.hasExpectedSize()) {

               if (ib.getCurrentSize() > instanceLength) {
                   throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                       "invalid instance length of " + instanceLength + "; current size: " +
                       ib.getCurrentSize());
               }

               ib.setExpectedSize(instanceLength);

            } else {
                if (ib.getExpectedSize() != instanceLength) {
                    throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                            "instance length cannot change, was: " + ib.getExpectedSize() +
                            ", in req: " + instanceLength);
                }
            }
        } else if (ib.hasExpectedSize()) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                    "instance length must be provided");
        }

        OutputStream out = ib.getAppendingOutputStream();

        // don't catch exceptions here, if something fails we do not
        // delete the incoming blob yet since client already has resume id
        // and can resume

        // also it's ok if input stream has no payload here; we'll just return
        // Content-Range below; this is for clients polling for current state of upload

        ByteUtil.copy(is, true, out, true, contentLength);

        if (ib.isComplete()) {
            Blob b = blobStore.extractIncoming(ib);
            saveDocument(b, context, contentType, folder, filename);
        } else {
            context.resp.addHeader(RESUME_ID_HEADER, resumeId);

            ContentRange range = getCurrentContentRange(ib);
            // Add Content-Range in response only if we have some data or if
            // expected length is known (instance length); it's weird to return */*
            // seems like not a valid combo to return (we accept it from clients, but
            // this is per the rule "Be stict with what you send and tolerant in what you receive")
            if (range != null) {
                context.resp.addHeader("Content-Range", range.toString());
            }

        }
    }

    private static ContentRange getCurrentContentRange(IncomingBlob ib) throws HttpException
    {
        ContentRange range = null;

        if (ib.getCurrentSize() == 0) {
            if (ib.hasExpectedSize()) {
                range = new ContentRange(ib.getExpectedSize());
            }
        } else {
            range = ib.hasExpectedSize() ?
                        new ContentRange(0, ib.getCurrentSize() - 1, ib.getExpectedSize()) :
                        new ContentRange(0, ib.getCurrentSize() - 1);
        }

        return range;
    }

    public static void sendZimbraHeaders(HttpServletResponse resp, MailItem item) {
        if (resp == null || item == null)
            return;
        resp.addHeader("X-Zimbra-ItemId", item.getId() + "");
        resp.addHeader("X-Zimbra-Version", item.getVersion() + "");
        resp.addHeader("X-Zimbra-Modified", item.getChangeDate() + "");
        resp.addHeader("X-Zimbra-Change", item.getModifiedSequence() + "");
        resp.addHeader("X-Zimbra-Revision", item.getSavedSequence() + "");
        resp.addHeader("X-Zimbra-ItemType", item.getType().toString());
        try {
            String val = item.getName();
            if (!StringUtil.isAsciiString(val)) {
                val = MimeUtility.encodeText(val, "utf-8", "B");
            }
            resp.addHeader("X-Zimbra-ItemName", val);
            val = item.getPath();
            if (!StringUtil.isAsciiString(val)) {
                val = MimeUtility.encodeText(val, "utf-8", "B");
            }
            resp.addHeader("X-Zimbra-ItemPath", val);
        } catch (UnsupportedEncodingException e1) {
        } catch (ServiceException e) {
        }
    }

    private static final int READ_AHEAD_BUFFER_SIZE = 256;
    private static final byte[][] SCRIPT_PATTERN = {
        { '<', 's', 'c', 'r', 'i', 'p', 't' },
        { '<', 'S', 'C', 'R', 'I', 'P', 'T' }
    };

    public static void sendbackBinaryData(HttpServletRequest req,
                                          HttpServletResponse resp,
                                          InputStream in,
                                          String contentType,
                                          String disposition,
                                          String filename,
                                          long size) throws IOException {
        resp.setContentType(contentType);
        if (disposition == null) {
            String disp = req.getParameter(UserServlet.QP_DISP);
            disposition = (disp == null || disp.toLowerCase().startsWith("i") ) ? Part.INLINE : Part.ATTACHMENT;
        }
        PushbackInputStream pis = new PushbackInputStream(in, READ_AHEAD_BUFFER_SIZE);
        boolean isSafe = false;
        HttpUtil.Browser browser = HttpUtil.guessBrowser(req);
        if (browser != HttpUtil.Browser.IE) {
            isSafe = true;
        } else if (disposition.equals(Part.ATTACHMENT)) {
            isSafe = true;
            if (isScriptableContent(contentType)) {
                resp.addHeader("X-Download-Options", "noopen"); // ask it to save the file
            }
        }

        if (!isSafe) {
            byte[] buf = new byte[READ_AHEAD_BUFFER_SIZE];
            int bytesRead = pis.read(buf, 0, READ_AHEAD_BUFFER_SIZE);
            boolean hasScript;
            for (int i = 0; i < bytesRead; i++) {
                if (buf[i] == SCRIPT_PATTERN[0][0] || buf[i] == SCRIPT_PATTERN[1][0]) {
                    hasScript = true;
                    for (int pos = 1; pos < 7 && (i + pos) < bytesRead; pos++) {
                        if (buf[i+pos] != SCRIPT_PATTERN[0][pos] &&
                                buf[i+pos] != SCRIPT_PATTERN[1][pos]) {
                            hasScript = false;
                            break;
                        }
                    }
                    if (hasScript) {
                        resp.addHeader("Cache-Control", "no-transform");
                        disposition = Part.ATTACHMENT;
                        break;
                    }
                }
            }
            if (bytesRead > 0)
                pis.unread(buf, 0, bytesRead);
        }
        String cd = disposition + "; filename=" + HttpUtil.encodeFilename(req, filename == null ? "unknown" : filename);
        resp.addHeader("Content-Disposition", cd);
        if (size > 0)
            resp.setContentLength((int)size);
        ByteUtil.copy(pis, true, resp.getOutputStream(), false);
    }
    /**
     * Determines whether or not the contentType passed might contain script or other unsavory tags.
     * @param contentType The content type to check
     * @return true if there's a possiblilty that <script> is valid, false if not
     */
    private static boolean isScriptableContent(String contentType) {
        // Make sure we don't have to worry about a null content type;
        if (Strings.isNullOrEmpty(contentType)) {
            return false;
        }
        contentType = Mime.getContentType(contentType).toLowerCase();
        // only set no-open for 'script type content'
        return SCRIPTABLE_CONTENT_TYPES.contains(contentType);

    }
}
