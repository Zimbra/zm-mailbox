/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
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
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.extension.ExtensionUtil;


public class MimeHandlerManager {

    private static Map<String,HandlerInfo> sHandlers = new ConcurrentHashMap<String,HandlerInfo>();
    private static Log sLog = LogFactory.getLog(MimeHandlerManager.class);
    
    static class HandlerInfo {
        MimeTypeInfo mMimeType;
        Class<MimeHandler> mClass;
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
     * for text MIME types or the unknown type handler for other types.
     * 
     * @param mimeType the MIME type or <tt>null</tt>
     * @param filename the filename or <tt>null</tt> 
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
        
        if (handlerInfo == null)
            handlerInfo = loadHandler(mimeType, extension);

        handler = handlerInfo.getInstance();
        if (handler != null && sLog.isDebugEnabled()) {
            sLog.debug("Returning MIME handler: %s", handler.getClass().getName());
        }
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
    
    private static synchronized HandlerInfo loadHandler(String mimeType, String extension) {
        sLog.debug("Loading MIME handler for type %s, extension '%s'", mimeType, extension);
        
        HandlerInfo handlerInfo = null;
        try {
            MimeTypeInfo mt = lookUpMimeTypeInfo(mimeType, extension);
            List<MimeTypeInfo> mimeTypeList;
            
            if (mt == null || mt.getHandlerClass() == null) {
                boolean isTextType = (mimeType != null && (mimeType.matches(Mime.CT_TEXT_WILD) ||
                    mimeType.equalsIgnoreCase(Mime.CT_MESSAGE_RFC822)));
                
                // All unhandled text types default to text/plain handler.
                if (isTextType) {
                    sLog.debug("Falling back to %s MIME Handler for type %s", Mime.CT_DEFAULT, mimeType);
                    mimeTypeList = Provisioning.getInstance().getMimeTypes(Mime.CT_DEFAULT);
                    if (mimeTypeList.size() > 0) {
                        mt = mimeTypeList.get(0);
                    } else {
                        sLog.warn("Unable to load MIME handler for %s", Mime.CT_DEFAULT);
                    }
                }
                if (mt == null || mt.getHandlerClass() == null) {
                    sLog.debug("Falling back to %s MIME Handler for type %s", MimeHandler.CATCH_ALL_TYPE, mimeType);
                    mimeTypeList = Provisioning.getInstance().getMimeTypes(MimeHandler.CATCH_ALL_TYPE);
                    if (mimeTypeList.size() > 0) {
                        mt = mimeTypeList.get(0);
                    } else {
                        sLog.warn("Unable to load MIME handler fo %s", MimeHandler.CATCH_ALL_TYPE);
                    }
                    assert(mt != null);
                }
            }

            if (mt != null && mt.getHandlerClass() != null) {
                String clazz = mt.getHandlerClass();
                assert(clazz != null);
                if (clazz.indexOf('.') == -1)
                    clazz = "com.zimbra.cs.mime.handler." + clazz;
                try {
                    handlerInfo = new HandlerInfo();
                    handlerInfo.mClass = ExtensionUtil.loadClass(mt.getExtension(), clazz);
                    handlerInfo.mMimeType = mt;
                    handlerInfo.mRealMimeType = mimeType;
                    sHandlers.put(getKey(mimeType, extension), handlerInfo);
                } catch (Exception e) {
                    sLog.warn("Unable to instantiate MIME handler", e);
                }
            }
        } catch (ServiceException e) {
            sLog.error("Unable to load MIME handler", e);
        } 
        return handlerInfo;
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
     * Looks up all <tt>MimeTypeInfo</tt>s that match either the given type or
     * extension and returns the one with the highest priority.
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
