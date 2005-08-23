/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.scan;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.service.ServiceException;

public abstract class Scanner {

    public static final class Result {
        private String mDesc;
        
        private Result(String desc) {
            mDesc = desc;
        }
        
        public String toString() {
            return mDesc;
        }
    }
    
    public static final Result ACCEPT = new Result("ACCEPT");
    public static final Result REJECT = new Result("REJECT");
    public static final Result ERROR = new Result("ERROR");
    
    private static Log mLog = LogFactory.getLog(Scanner.class);

    private static ScanConfig mConfig;
    
    public static boolean isEnabled() {
        init();
        return mConfig.getEnabled();
    }
    
    private static Scanner sScanner;

    private static boolean mInitialized;
    
    private static synchronized boolean init() {
        if (!mInitialized) {
            try {
                mConfig = new ScanConfig();
                if (!mConfig.getEnabled()) {
                    mLog.info("attachment scan is disabled");
                    return mInitialized;
                }
                    
                String cname = mConfig.getClassName();
                if (cname == null) {
                    mLog.error("attachment scan class name is null");
                    return mInitialized;
                }
                
                sScanner = (Scanner)Class.forName(mConfig.getClassName()).newInstance();
                
                sScanner.setURL(mConfig.getURL());
                
                mInitialized = true;
                
                mLog.info("attachment scan enabled class=" + sScanner.getClass().getName() + " url=" + mConfig.getURL());
                
            } catch (InstantiationException e) {
                mLog.error("error creating scanner", e);
            } catch (IllegalAccessException e) {
                mLog.error("error creating scanner", e);
            } catch (ClassNotFoundException e) {
                mLog.error("error creating scanner", e);
            } catch (ServiceException e) {
                mLog.error("error creating scanner", e);
            } catch (MalformedURLException e) {
                mLog.error("error creating scanner", e);
            }
        }            
        return mInitialized;
    }
    
    public static Result accept(FileItem fi, StringBuffer info) {
        
        if (!init()) {
            return ERROR;
        }
        
        if (fi.isInMemory()) {
            return sScanner.accept(fi.get(), info);
        }
        
        InputStream is = null;
        try {
            is = fi.getInputStream();
        } catch (IOException ioe) {
            mLog.error("exception getting input stream for scanning", ioe);
            return ERROR;
        }
        
        Result result = REJECT;
        try {
            result = sScanner.accept(is, info); 
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
                mLog.warn("exception closing scanned input stream", ioe);
            }
        }
        return result;
    }
    
    protected abstract Result accept(InputStream is, StringBuffer info);

    protected abstract Result accept(byte[] data, StringBuffer info);

    public abstract void setURL(String string) throws MalformedURLException;
}
