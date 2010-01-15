/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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

package com.zimbra.cs.client.soap;

import java.io.IOException;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.CliUtil;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.client.*;
import com.zimbra.common.soap.SoapFaultException;

public class TestAuth implements Runnable {

    private static Log mLog = LogFactory.getLog(TestAuth.class);

    private static void usage() {
        System.err.println("Usage: java " + TestAuth.class.getName() + " iterations threads loacct hiacct domain url");
        System.exit(1);
    }

    public static void main(String argv[]) {
        CliUtil.toolSetup();

        if (argv.length != 6) {
            usage();
        }

        int iterations = 0, threads = 0, loAcctNum = 0, hiAcctNum = 0;
        String domain = null, url = null;

        try {
            iterations = Integer.valueOf(argv[0]).intValue();
            threads = Integer.valueOf(argv[1]).intValue();
            loAcctNum = Integer.valueOf(argv[2]).intValue();
            hiAcctNum = Integer.valueOf(argv[3]).intValue();
            domain = argv[4];
            url = argv[5];
        } catch (NumberFormatException nfe) {
            usage();
        }

        PooledExecutor executor = new PooledExecutor(new BoundedLinkedQueue(), threads);
        executor.setMinimumPoolSize(threads);
        executor.waitWhenBlocked();

        for (int iter = 0; iter < iterations; iter++) {
            for (int acct = loAcctNum; acct <= hiAcctNum; acct++) {
                String user = "load" + acct + "@" + domain;
                try {
                    executor.execute(new TestAuth(iter, url, user, "test123"));
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        executor.shutdownAfterProcessingCurrentlyQueuedTasks();
    }

    private String mUrl;
    private String mAcct;
    private String mPasswd;
    private int mIteration;

    TestAuth(int iter, String url, String acct, String passwd) {
        mIteration = iter;
        mUrl = url;
        mAcct = acct;
        mPasswd = passwd;
    }

    public void run() {
        long start = System.currentTimeMillis();
        String tags = doAuth(mUrl, mAcct, mPasswd);
        long elapsed = System.currentTimeMillis() - start;
        System.err.println(mIteration + " " + Thread.currentThread().getName() + " " + mAcct + " " +
                tags);
    }

    private String doAuth(String url, String account, String password) {
        try {
            /* auth first */
            //System.out.println("========= AUTHENTICATE ===========");
            LmcAuthRequest auth = new LmcAuthRequest();
            auth.setUsername(account);
            auth.setPassword(password);
            LmcAuthResponse authResp = (LmcAuthResponse) auth.invoke(url);
            LmcSession session = authResp.getSession();

            /* get the tags */
            //System.out.println("======== GET TAGS =======");
            LmcGetTagRequest gtReq = new LmcGetTagRequest();
            gtReq.setSession(session);
            LmcGetTagResponse gtResp = (LmcGetTagResponse) gtReq.invoke(url);

            /* dump the tags */
            //System.out.println("==== DUMP TAGS ======");
            LmcTag tags[] = gtResp.getTags();
            StringBuffer sb = new StringBuffer();
            for (int t = 0; tags != null && t < tags.length; t++) {
                sb.append(tags[t]).append(" ");
            }
            return sb.toString();
        } catch (SoapFaultException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (LmcSoapClientException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return null;
    }
}
