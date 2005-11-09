/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.util.ZimbraLog;

public abstract class UploadScanner {

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
    
    private static List sRegisteredScanners = new LinkedList();
    
    public static void registerScanner(UploadScanner scanner) {
    	sRegisteredScanners.add(scanner);
    }
    
    public static void unregisterScanner(UploadScanner scanner) {
    	sRegisteredScanners.remove(scanner);
    }
    
    public static Result accept(Upload up, StringBuffer info) {
        
    	for (Iterator iter = sRegisteredScanners.iterator(); iter.hasNext();) {
    		UploadScanner scanner = (UploadScanner)iter.next();
    		if (!scanner.isEnabled()) {
    			continue;
    		}
    		
    		Result result;
    		InputStream is = null;
    		try {
    			is = up.getInputStream();
    		} catch (IOException ioe) {
    			ZimbraLog.misc.error("exception getting input stream for scanning", ioe);
    			info.append(" ").append(ioe);
    			return ERROR;
    		}
    		try {
    			result = scanner.accept(is, info); 
    		} finally {
    			try {
    				is.close();
    			} catch (IOException ioe) {
    				ZimbraLog.misc.warn("exception closing scanned input stream", ioe);
    			}
    		}
			if (result == REJECT || result == ERROR) {
				// Fail on the first scanner that says it was bad,
				// or first error we encounter. Is bailing on first error
				// too harsh, should we continue to try other scanners?
				return result;
			}
    	}
        return ACCEPT;
    }
    
    protected abstract Result accept(InputStream is, StringBuffer info);

    protected abstract Result accept(byte[] data, StringBuffer info);

    public abstract void setURL(String string) throws MalformedURLException;
    
    public abstract boolean isEnabled();
}
