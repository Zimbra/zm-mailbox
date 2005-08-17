package com.liquidsys.coco.client.soap;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 
import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.client.*;
import com.liquidsys.soap.SoapFaultException;
import com.liquidsys.coco.util.Liquid;

public class TestAuth implements Runnable {

    private static Log mLog = LogFactory.getLog(TestAuth.class);
    
    private static void usage() {
        System.err.println("Usage: java " + TestAuth.class.getName() + " iterations threads loacct hiacct domain url");
        System.exit(1);
    }

    public static void main(String argv[]) {
        Liquid.toolSetup();
        
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
