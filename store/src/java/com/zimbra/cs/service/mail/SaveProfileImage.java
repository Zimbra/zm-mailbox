package com.zimbra.cs.service.mail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.doc.SaveDocument;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SaveProfileImageRequest;
import com.zimbra.soap.mail.message.SaveProfileImageResponse;

public class SaveProfileImage extends SaveDocument {

    public static final String IMAGE_FOLDER_PREFIX = "ProfileImageHolder_";
    public static final String IMAGE_ITEM_PREFIX = "ProfileImage_";
    public static final String IMAGE_CUSTOM_DATA_SECTION = "Profile";
    private static final String FAILURE_MESSAGE = "Failed to add account profile image";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        SaveProfileImageRequest req = zsc.elementToJaxb(request);
        String uploadId = req.getUploadId();
        String base64 = req.getImageB64Data().trim();
        InputStream in = null;
        String contentType = null;
        Document imgDocItem = null;
        String name = IMAGE_ITEM_PREFIX + mbox.getAccountId();
        String folderName = IMAGE_FOLDER_PREFIX + mbox.getAccountId();
        int folderId;
        try {
            if (!StringUtil.isNullOrEmpty(base64)) {
                byte[] decodedBytes = Base64.decodeBase64(base64);
                in = new ByteArrayInputStream(decodedBytes);
                contentType = MimeDetect.getMimeDetect()
                    .detect(new ByteArrayInputStream(decodedBytes));
            } else if (uploadId != null) {
                Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), uploadId,
                    zsc.getAuthToken());
                in = up.getInputStream();
                if (up.getSize() > 2097152l) {
                    throw ServiceException.FORBIDDEN("Uploaded image is larger than 2 MB");
                }
                contentType = up.getContentType();
            } else {
                throw ServiceException
                    .INVALID_REQUEST("Request is missing image content information", null);
            }
            if (contentType == null || !contentType.matches(MimeConstants.CT_IMAGE_WILD)) {
                throw MailServiceException
                    .INVALID_IMAGE("Uploaded image is not a valid image file");
            }
            try {
                Folder imgFolder = mbox.getFolderByName(octxt, Mailbox.ID_FOLDER_ROOT, folderName);
                folderId = imgFolder.getId();
            } catch (ServiceException exception) {
                if (MailServiceException.NO_SUCH_FOLDER.equals(exception.getCode())) {
                    byte hidden = Folder.FOLDER_IS_IMMUTABLE | Folder.FOLDER_DONT_TRACK_COUNTS;
                    Folder.FolderOptions fopt = new Folder.FolderOptions();
                    fopt.setAttributes(hidden).setDefaultView(MailItem.Type.DOCUMENT).setFlags(0)
                        .setColor(MailItem.DEFAULT_COLOR_RGB);
                    Folder profileFolder = mbox.createFolder(octxt, folderName,
                        Mailbox.ID_FOLDER_ROOT, fopt);
                    folderId = profileFolder.getId();
                } else {
                    throw ServiceException.FAILURE(FAILURE_MESSAGE, exception);
                }
            }
            Doc imageDoc = new Doc(in, new ContentType(contentType, "image/jpeg"), name, null,
                "Account Profile Image");
            try {
                TypedIdList ids = mbox.getItemIds(octxt, folderId);
                List<Integer> idList = ids.getAllIds();
                MailItem[] itemList = mbox.getItemById(octxt, idList, MailItem.Type.DOCUMENT);
                for (MailItem item : itemList) {
                    CustomMetadata customData = item.getCustomData(IMAGE_CUSTOM_DATA_SECTION);
                    if (customData != null && customData.containsKey("p")
                        && customData.get("p").equals("1")) {
                        mbox.delete(octxt, item.getId(), MailItem.Type.DOCUMENT);
                    }
                }
                imgDocItem = createDocument(imageDoc, zsc, octxt, mbox, null, in, folderId,
                    MailItem.Type.DOCUMENT, null,
                    new CustomMetadata(IMAGE_CUSTOM_DATA_SECTION, "d1:p1:1e"), false);
            } catch (ServiceException e) {
                throw ServiceException.FAILURE(FAILURE_MESSAGE, e);
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE(FAILURE_MESSAGE, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    ZimbraLog.mailbox.error("Exception in closing input stream for upload", e);
                }
            }
        }
        SaveProfileImageResponse response = new SaveProfileImageResponse();
        response.setItemId(imgDocItem.getId());
        return zsc.jaxbToElement(response);
    }
}
