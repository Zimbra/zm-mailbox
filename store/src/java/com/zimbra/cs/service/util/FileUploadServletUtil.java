package com.zimbra.cs.service.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.soap.ZimbraSoapContext;

public class FileUploadServletUtil {

    public static String getImageBase64(ZimbraSoapContext zsc, String attachId)
        throws ServiceException {
        Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), attachId,
            zsc.getAuthToken());
        String contentType;
        try {
            contentType = MimeDetect.getMimeDetect().detect(up.getInputStream());
        } catch (IOException ioe) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(ioe);
        }
        if (contentType == null || !contentType.matches(MimeConstants.CT_IMAGE_WILD)) {
            throw MailServiceException.INVALID_IMAGE("Uploaded image is not a valid image file");
        }
        if (up.getSize() > 3145728l) {
            throw ServiceException.OPERATION_DENIED("Uploaded image is larger than 3MB");
        }
        InputStream in = null;
        String result = null;
        try {
            in = up.getInputStream();
            byte[] imageBytes = IOUtils.toByteArray(in);
            result = ByteUtil.encodeLDAPBase64(imageBytes);
        } catch (IOException e) {
            ZimbraLog.account.error(
                "Exception in adding user account profile image with aid=%s for account %s",
                attachId, zsc.getRequestedAccountId());
            throw ServiceException.INVALID_REQUEST("Exception in adding account profile image", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    ZimbraLog.account.error("Exception in closing inputstream for upload", e);
                }
            }
        }
        return result;
    }
}
