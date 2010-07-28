/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.mime.handler.UnknownTypeHandler;


public class MimeHandlerManager {

    private static Map<String, HandlerInfo> sHandlers = new ConcurrentHashMap<String,HandlerInfo>();
    private static Log sLog = LogFactory.getLog(MimeHandlerManager.class);

    private static class HandlerInfo {
        MimeTypeInfo mMimeType;
        Class<? extends MimeHandler> mClass;
        String mRealMimeType;

        public MimeHandler getInstance() throws MimeHandlerException {
            MimeHandler handler;
            try {
                handler = mClass.newInstance();
            } catch (InstantiationException e) {
                throw new MimeHandlerException(e);
            } catch (IllegalAccessException e) {
                throw new MimeHandlerException(e);
            }
            handler.setContentType(mRealMimeType);
            handler.mMimeTypeInfo = mMimeType;
            return handler;
        }
    }

    /**
     * Returns the <tt>MimeHandler</tt> for the given MIME type and filename
     * extension.  If multiple MIME handlers match, returns the one with the
     * highest priority.  If no match is found, returns either the text/plain
     * handler for text MIME types or the unknown type handler for other types.
     *
     * @param mimeType the MIME type or <tt>null</tt>
     * @param filename the filename or <tt>null</tt>
     * @throws MimeHandlerException if the handler could not be loaded
     */
    public static MimeHandler getMimeHandler(String mimeType, String filename)
    throws MimeHandlerException {
        sLog.debug("Getting MIME handler for type %s, filename '%s'", mimeType, filename);

        MimeHandler handler = null;
        if (!StringUtil.isNullOrEmpty(mimeType)) {
            mimeType = Mime.getContentType(mimeType);
        }
        String extension = FileUtil.getExtension(filename);
        HandlerInfo handlerInfo = sHandlers.get(getKey(mimeType, extension));

        if (handlerInfo == null) {
            handlerInfo = loadHandler(mimeType, extension);
            sHandlers.put(getKey(mimeType, extension), handlerInfo);
        }


        handler = handlerInfo.getInstance();
        sLog.debug("Returning MIME handler: %s", handler.getClass().getName());
        return handler;
    }

    /**
     * Returns the maximum number of characters that can be returned by
     * {@link MimeHandler#getContent}.
     * @see Provisioning#A_zimbraAttachmentsIndexedTextLimit
     */
    public static int getIndexedTextLimit() {
        int length = 1024 * 1024;
        try {
            Provisioning prov = Provisioning.getInstance();
            Server server = prov.getLocalServer();
            length = server.getIntAttr(Provisioning.A_zimbraAttachmentsIndexedTextLimit, length);
        } catch (ServiceException e) {
            sLog.warn("Unable to determine maximum indexed content length", e);
        }
        return length;
    }

    /**
     * Returns the {@link HandlerInfo} that matches the given type or extension.
     * If an exact match is not found, returns the default handler. If a matched
     * handler class could not be instantiated, returns {@link UnknownTypeHandler}.
     *
     * @throws MimeHandlerException if the handler could not be loaded
     */
    private static synchronized HandlerInfo loadHandler(String mimeType,
            String extension) throws MimeHandlerException {
        sLog.debug("Loading MIME handler for type %s, extension '%s'",
                mimeType, extension);

        try {
            MimeTypeInfo mt = lookUpMimeTypeInfo(mimeType, extension);
            List<MimeTypeInfo> mimeTypeList;

            // Look up the MimeTypeInfo.
            if (mt == null || mt.getHandlerClass() == null) {
                boolean isTextType = (mimeType != null &&
                        (mimeType.matches(MimeConstants.CT_TEXT_WILD) ||
                                mimeType.equalsIgnoreCase(
                                        MimeConstants.CT_MESSAGE_RFC822)));

                // All unhandled text types default to text/plain handler.
                if (isTextType) {
                    sLog.debug("Falling back to %s MIME Handler for type %s",
                            MimeConstants.CT_DEFAULT, mimeType);
                    mimeTypeList = Provisioning.getInstance().getMimeTypes(
                            MimeConstants.CT_DEFAULT);
                    if (mimeTypeList.size() > 0) {
                        mt = mimeTypeList.get(0);
                    } else {
                        sLog.warn("Unable to load MIME handler for %s",
                                MimeConstants.CT_DEFAULT);
                    }
                }

                // If there was no match, load the catch-all handler.
                if (mt == null || mt.getHandlerClass() == null) {
                    sLog.debug("Falling back to %s MIME Handler for type %s",
                            MimeHandler.CATCH_ALL_TYPE, mimeType);
                    mimeTypeList = Provisioning.getInstance().getMimeTypes(
                            MimeHandler.CATCH_ALL_TYPE);
                    if (mimeTypeList.size() > 0) {
                        mt = mimeTypeList.get(0);
                    } else {
                        throw new MimeHandlerException(
                                "Unable to load MIME handler for type " +
                                MimeHandler.CATCH_ALL_TYPE);
                    }
                }
            }

            if (mt.getHandlerClass() == null) {
                String msg = String.format("%s not specified for MIME handler %s.",
                    Provisioning.A_zimbraMimeHandlerClass, mt.getDescription());
                throw new MimeHandlerException(msg);
            }

            // Load the class
            HandlerInfo handlerInfo = new HandlerInfo();
            String className = mt.getHandlerClass();
            if (className.indexOf('.') == -1) {
                className = "com.zimbra.cs.mime.handler." + className;
            }
            handlerInfo.mMimeType = mt;
            handlerInfo.mRealMimeType = mimeType;
            try {
                handlerInfo.mClass = ExtensionUtil.loadClass(mt.getExtension(),
                        className).asSubclass(MimeHandler.class);
            } catch (ClassNotFoundException e) {
                // miss configuration or the extension is disabled
                sLog.warn("MIME handler %s for %s (%s) not found",
                        className, extension, mimeType);
                // Fall back to UnknownTypeHandler
                handlerInfo.mClass = UnknownTypeHandler.class;
            }
            return handlerInfo;
        } catch (ServiceException e) {
            String msg = String.format(
                    "Unable to load MIME handler for type %s, extension %s.",
                    mimeType, extension);
            throw new MimeHandlerException(msg, e);
        }
    }

    private static String getKey(String mimeType, String ext) {
        if (mimeType == null) {
            mimeType = "";
        }
        if (ext == null) {
            ext = "";
        }
        return mimeType + "," + ext;
    }

    /**
     * Returns the highest-priority <tt>MimeTypeInfo</tt> that
     * matches either the given type or extension.
     * @return the <tt>MimeTypeInfo</tt> object or <tt>null</tt>
     */
    private static MimeTypeInfo lookUpMimeTypeInfo(String mimeType, String ext)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        // Look up by both type and extension
        List<MimeTypeInfo> mimeTypes = prov.getAllMimeTypes();
        if (mimeTypes.size() == 0) {
            return null;
        }

        MimeTypeInfo retVal = null;
        int maxPriority = Integer.MIN_VALUE;
        for (MimeTypeInfo mti : mimeTypes) {
            if (matches(mti, mimeType, ext) && mti.getPriority() > maxPriority) {
                retVal = mti;
                maxPriority = mti.getPriority();
            }
        }
        if (retVal != null && retVal.getHandlerClass() == null) {
            ZimbraLog.mailbox.warn("%s not defined for MIME handler %s",
                Provisioning.A_zimbraMimeHandlerClass, retVal.getDescription());
        }
        return retVal;
    }

    private static boolean matches(MimeTypeInfo mti, String mimeType, String ext) {
        if (mti == null) {
            return false;
        }
        if (mimeType == null) {
            mimeType = "";
        }
        if (ext == null) {
            ext = "";
        }
        mimeType = mimeType.toLowerCase();
        ext = ext.toLowerCase();
        if (mti.getFileExtensions().contains(ext)) {
            return true;
        }
        for (String patternString : mti.getMimeTypes()) {
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(mimeType);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

}
