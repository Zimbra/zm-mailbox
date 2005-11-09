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
/*
 * Created on 2005. 1. 19.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.service.ServiceException;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ExecUtil {

    private static Log mLog = LogFactory.getLog(ExecUtil.class);

    public static class ProcessOutput {
        public int exitValue;
        public String stdout;
        public String stderr;
    }

    private static String inputStreamToString(InputStream is) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            int length = 0;
            int num;
            byte[] buffer = new byte[1024];
            while ((num = is.read(buffer)) != -1) {
                baos.write(buffer, 0, num);
                length += num;
            }
            if (baos.size() > 0)
                return new String(baos.toByteArray(), "UTF-8");
            else
                return null;
        } finally {
            is.close();
        }
    }

    public static int system(String cmdline) throws ServiceException {
        int rc = 0;
        try {
            Process p = Runtime.getRuntime().exec(cmdline);
            try {
                rc = p.waitFor();
            } catch (InterruptedException e) {
                mLog.warn("InterruptedException while waiting for command to finish");
            }
            if (rc != 0)
                mLog.warn("Command returned non-success exit value: " + rc);
        } catch (IOException e) {
            mLog.error("Unable to exec: " + cmdline, e);
            throw ServiceException.FAILURE("Unable to exec: " + cmdline, e);
        }
        return rc;
    }

    public static ProcessOutput exec(String cmdline) throws ServiceException {
        ProcessOutput proc = new ProcessOutput();
        try {
            Process p = Runtime.getRuntime().exec(cmdline);
            
            proc.stdout = inputStreamToString(p.getInputStream());
            proc.stderr = inputStreamToString(p.getErrorStream());
            try {
                proc.exitValue = p.waitFor();
            } catch (InterruptedException e) {
                mLog.warn("InterruptedException while waiting for command to finish");
            }
            if (proc.exitValue != 0)
                mLog.warn("Command returned non-success exit value: " + proc.exitValue);
        } catch (IOException e) {
            mLog.error("Unable to exec: " + cmdline, e);
            throw ServiceException.FAILURE("Unable to exec: " + cmdline, e);
        }
        return proc;
    }    
}
