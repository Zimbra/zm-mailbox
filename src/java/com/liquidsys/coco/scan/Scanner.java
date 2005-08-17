package com.liquidsys.coco.scan;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.service.ServiceException;

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
