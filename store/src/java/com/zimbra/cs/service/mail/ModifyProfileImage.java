package com.zimbra.cs.service.mail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
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
import com.zimbra.cs.service.formatter.NativeFormatter;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.ModifyProfileImageRequest;

public class ModifyProfileImage extends SaveDocument {

    public static final String IMAGE_FOLDER_PREFIX = "ProfileImageHolder_";
    public static final String IMAGE_ITEM_PREFIX = "ProfileImage_";
    public static final String IMAGE_CUSTOM_DATA_SECTION = "Profile";
    private static final String FAILURE_MESSAGE = "Failed to add account profile image";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ModifyProfileImageRequest req = zsc.elementToJaxb(request);
        String uploadId = req.getUploadId();
        String base64 = req.getImageB64Data().trim();
        InputStream thumbnailIn = null;
        InputStream in = null;
        String contentType = null;
        String imageFileName = IMAGE_ITEM_PREFIX + mbox.getAccountId();
        String imageFolderName = IMAGE_FOLDER_PREFIX + mbox.getAccountId();
        long inputSize = 0l;
        int imageItemID = -1;
        boolean updateLDAPSuccess = false;
        boolean updateDBSuccess = false;
        boolean removeFromLDAPSuccess = false;
        boolean removeFromDBSuccess = false;
        String ldapBackup = null;
        boolean removeProfileImage = false;
        try {
            if (!StringUtil.isNullOrEmpty(base64)) {
                byte[] decodedBytes = Base64.decodeBase64(base64);
                inputSize = decodedBytes.length;
                thumbnailIn = new ByteArrayInputStream(decodedBytes);
                in = new ByteArrayInputStream(decodedBytes);
                contentType = MimeDetect.getMimeDetect()
                    .detect(new ByteArrayInputStream(decodedBytes));
            } else if (uploadId != null) {
                Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), uploadId,
                    zsc.getAuthToken());
                in = up.getInputStream();
                thumbnailIn = up.getInputStream();
                inputSize = up.getSize();
                contentType = up.getContentType();
            } else {
                // remove image
                removeProfileImage = true;
                ldapBackup = Provisioning.getInstance().A_thumbnailPhoto;
                removeFromLDAP(mbox, zsc);
                removeFromLDAPSuccess = true;
                removeFromDatabase(mbox, octxt, imageFolderName);
                removeFromDBSuccess = true;
            } 
            if(! removeProfileImage) {
            if (contentType == null || !contentType.matches(MimeConstants.CT_IMAGE_WILD)) {
                throw MailServiceException
                    .INVALID_IMAGE("Uploaded image is not a valid image file");
            }
            if (inputSize > DebugConfig.profileImageMaxSize) {
                throw MailServiceException.INVALID_IMAGE("Uploaded image is larger than 2 MB");
            }
            // Update LDAP with thumbnail image
            boolean imageResized = updateLDAP(inputSize, thumbnailIn, contentType, imageFileName,
                mbox, zsc, true);
            if (!imageResized) {
                ZimbraLog.mailbox.info("Image not resized. Updating original image to LDAP.");
                byte[] byteArray = IOUtils.toByteArray(in);
                InputStream originalIn = new ByteArrayInputStream(byteArray);
                // update LDAP with actual image
                updateLDAP(inputSize, originalIn, contentType, imageFileName, mbox, zsc, false);
            }
            updateLDAPSuccess = true;
            // Update Database with actual image
            imageItemID = updateDatabase(mbox, octxt, imageFolderName, contentType, in, imageFileName, zsc);
            updateDBSuccess = true;
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
            if (updateLDAPSuccess && !updateDBSuccess) {
                // Databse update failed; revert from LDAP
                HashMap<String, Object> prefs = new HashMap<String, Object>();
                prefs.put(Provisioning.A_thumbnailPhoto, null);
                Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true,
                    zsc.getAuthToken());
            } else if (removeFromLDAPSuccess && !removeFromDBSuccess) {
                // Databse removal failed; revert to LDAP
                HashMap<String, Object> prefs = new HashMap<String, Object>();
                prefs.put(Provisioning.A_thumbnailPhoto, ldapBackup);
                Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true,
                    zsc.getAuthToken());
            }
        }
        Element response = zsc.createElement(MailConstants.MODIFY_PROFILE_IMAGE_RESPONSE);
        if(imageItemID != -1) {
            response.addAttribute(MailConstants.A_ITEMID, imageItemID);
        }
        return response;
    }

    private static boolean updateLDAP(long inputSize, InputStream thumbnailIn, String contentType,
        String imageFileName, Mailbox mbox, ZimbraSoapContext zsc, boolean resize) throws IOException, ServiceException {
        String result = null;
        if (resize) {
            if (inputSize <= LC.max_image_size_to_resize.intValue()) {
                int thumbnailImageDimension = DebugConfig.profileThumbnailImageDimension;
                byte[] data = NativeFormatter.getResizedImageData(thumbnailIn, contentType,
                    imageFileName, thumbnailImageDimension, thumbnailImageDimension);
                if (data != null) {
                    String code = new String(
                        Arrays.copyOfRange(data, 0, NativeFormatter.RETURN_CODE_NO_RESIZE.length()),
                        "UTF-8");
                    if (NativeFormatter.RETURN_CODE_NO_RESIZE.equals(code)) {
                        return false;
                    }
                    result = ByteUtil.encodeLDAPBase64(data);
                } else {
                    ZimbraLog.mailbox.warn("Unable to resize profile image");
                    throw ServiceException.FAILURE("FAILURE_MESSAGE", null);
                }
            } else {
                ZimbraLog.mailbox.warn(
                    "Profile image is larger than maximum size allowed for resizing (max_image_size_to_resize): %d > %d",
                    inputSize, LC.max_image_size_to_resize.intValue());
                throw ServiceException.FAILURE("FAILURE_MESSAGE", null);
            }
        } else {
            byte[] byteArray = IOUtils.toByteArray(thumbnailIn);
            result = ByteUtil.encodeLDAPBase64(byteArray);
        }
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_thumbnailPhoto, result);
        Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true,
            zsc.getAuthToken());
        return true;
    }

    private int updateDatabase(Mailbox mbox, OperationContext octxt, String imageFolderName,
        String contentType, InputStream in, String imageFileName, ZimbraSoapContext zsc)
        throws ServiceException {
        int folderId;
        try {
            Folder imgFolder = mbox.getFolderByName(octxt, Mailbox.ID_FOLDER_ROOT, imageFolderName);
            folderId = imgFolder.getId();
        } catch (ServiceException exception) {
            if (MailServiceException.NO_SUCH_FOLDER.equals(exception.getCode())) {
                byte hidden = Folder.FOLDER_IS_IMMUTABLE | Folder.FOLDER_DONT_TRACK_COUNTS;
                Folder.FolderOptions fopt = new Folder.FolderOptions();
                fopt.setAttributes(hidden).setDefaultView(MailItem.Type.DOCUMENT).setFlags(0)
                    .setColor(MailItem.DEFAULT_COLOR_RGB);
                Folder profileFolder = mbox.createFolder(octxt, imageFolderName, Mailbox.ID_FOLDER_ROOT,
                    fopt);
                folderId = profileFolder.getId();
            } else {
                throw ServiceException.FAILURE(FAILURE_MESSAGE, exception);
            }
        }
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
        Doc imageDoc = new Doc(in, new ContentType(contentType, "image/jpeg"), imageFileName, null,
            "Account Profile Image");
        CustomMetadata customMetaData = new CustomMetadata(IMAGE_CUSTOM_DATA_SECTION);
        customMetaData.put("p", "1");
        Document imgDocItem = createDocument(imageDoc, zsc, octxt, mbox, null, in, folderId,
            MailItem.Type.DOCUMENT, null, customMetaData, false);
        return imgDocItem.getId();
    }
    
    private static void removeFromLDAP(Mailbox mbox, ZimbraSoapContext zsc) throws IOException, ServiceException {
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_thumbnailPhoto, null);
        Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true,
            zsc.getAuthToken());
    }
    
    private void removeFromDatabase(Mailbox mbox, OperationContext octxt, String imageFolderName)
        throws ServiceException {
        int folderId;
        try {
            Folder imgFolder = mbox.getFolderByName(octxt, Mailbox.ID_FOLDER_ROOT, imageFolderName);
            folderId = imgFolder.getId();
        } catch (ServiceException exception) {
            if (MailServiceException.NO_SUCH_FOLDER.equals(exception.getCode())) {
                return;
            } else {
                throw ServiceException.FAILURE(FAILURE_MESSAGE, exception);
            }
        }
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
    }
}
