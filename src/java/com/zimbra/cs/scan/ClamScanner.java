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

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.extension.ZimbraExtension;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.tcpserver.TcpServerInputStream;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Zimbra;

public class ClamScanner extends UploadScanner implements ZimbraExtension {
    
    private static final String DEFAULT_URL = "clam://localhost:3310/";
    
    private static Log mLog = LogFactory.getLog(ClamScanner.class);

    private boolean mInitialized;

    private String mClamdHost;
    
    private int mClamdPort;

    private static final String CLAM_URL_REGEX = "[Cc][Ll][Aa][Mm]://([A-Za-z0-9]+):([0-9]+).*"; 
    
    private static final Pattern CLAM_URL_PATTERN = Pattern.compile(CLAM_URL_REGEX);

    public ClamScanner() {
    }

    public synchronized void init() {
        if (mInitialized) {
        	return;
        }

        try {
        	mConfig = new ClamScannerConfig();
        	if (!mConfig.getEnabled()) {
        		mLog.info("attachment scan is disabled");
        		mInitialized = true;
        	}
        	setURL(mConfig.getURL());
        	mInitialized = true;
        	mLog.info("attachment scan enabled url=" + mConfig.getURL());
        } catch (ServiceException e) {
        	mLog.error("error creating scanner", e);
        } catch (MalformedURLException e) {
        	mLog.error("error creating scanner", e);
        }
    }            

	public void destroy() {
		mInitialized = false;
	}

    public void setURL(String urlArg) throws MalformedURLException {
        if (urlArg == null) {
            urlArg = DEFAULT_URL;
        }
        
        Matcher m = CLAM_URL_PATTERN.matcher(urlArg);
        if (!m.matches()) {
            throw new MalformedURLException("invalid clam url: " + urlArg);
        }
        
        mClamdHost = m.group(1);
        mClamdPort = Integer.valueOf(m.group(2)).intValue();
        if (mClamdPort <= 0) {
            throw new MalformedURLException("invalid clamd port: " + mClamdPort);
        }
       
        mInitialized = true;
    }
    
    private ClamScannerConfig mConfig;

    protected Result accept(byte[] array, StringBuffer info) {
        if (!mInitialized) {
            return ERROR;
        }

        try {
            return accept0(array, null, info);
        } catch (Exception e) {
            mLog.error("exception communicating with clamd", e);
            return ERROR;
        }
    }

    protected Result accept(InputStream is, StringBuffer info) {
        if (!mInitialized) {
            return ERROR;
        }

        try {
            return accept0(null, is, info);
        } catch (Exception e) {
            mLog.error("exception communicating with clamd", e);
            return ERROR;
        }
    }

    private static final byte[] lineSeparator = { '\r', '\n' };

    private Result accept0(byte[] data, InputStream is, StringBuffer info) throws UnknownHostException, IOException {
        Socket commandSocket = null;
        Socket dataSocket = null;
        
        try {
            if (mLog.isDebugEnabled()) { mLog.debug("connecting to " + mClamdHost + ":" + mClamdPort); }
            commandSocket = new Socket(mClamdHost, mClamdPort);
        
            BufferedOutputStream out = new BufferedOutputStream(commandSocket.getOutputStream());
            TcpServerInputStream in = new TcpServerInputStream(commandSocket.getInputStream());

            if (mLog.isDebugEnabled()) { mLog.debug("writing STREAM command"); }
            out.write("STREAM".getBytes("iso-8859-1"));
            out.write(lineSeparator);
            out.flush();
        
            if (mLog.isDebugEnabled()) { mLog.debug("reading PORT"); }
            // REMIND - should have timeout's on this...
            String portLine = in.readLine();
            if (portLine == null) {
                throw new ProtocolException("EOF from clamd when looking for PORT repsonse");
            }
            if (!portLine.startsWith("PORT ")) {
                throw new ProtocolException("Got '" + portLine + "' from clamd, was expecting PORT <n>");
            }
            int port = 0;
            try {
                port = Integer.valueOf(portLine.substring("PORT ".length())).intValue();
            } catch (NumberFormatException nfe) {
                throw new ProtocolException("No port number in: " + portLine); 
            }

            if (mLog.isDebugEnabled()) { mLog.debug("stream connect to " + mClamdHost + ":" + port); }
            dataSocket = new Socket(mClamdHost, port);
            if (data != null) {
                dataSocket.getOutputStream().write(data);
                if (mLog.isDebugEnabled()) { mLog.debug("wrote " + data.length + " bytes"); }
            } else {
                int count = ByteUtil.copy(is, dataSocket.getOutputStream());
                if (mLog.isDebugEnabled()) { mLog.debug("copied " + count + " bytes"); }
            }
            dataSocket.close();
            
            if (mLog.isDebugEnabled()) { mLog.debug("reading result"); }
            String answer = in.readLine();
            if (answer == null) {
                throw new ProtocolException("EOF from clamd when looking for result");
            }
            info.setLength(0);
            if (answer.startsWith("stream: ")) {
                answer = answer.substring("stream: ".length());
            }
            info.append(answer);
            if (answer.equals("OK")) {
                return ACCEPT;
            } else {
                return REJECT;
            }
        } finally {
            if (dataSocket != null && !dataSocket.isClosed()) {
                mLog.warn("deffered close of stream connection");
                dataSocket.close();
            }
            if (commandSocket != null) {
                commandSocket.close();
            }
        }
    }
    
	public boolean isEnabled() {
		return mInitialized && mConfig.getEnabled();
	}

    public static void main(String[] args) throws IOException {
        Zimbra.toolSetup();
        StringBuffer info = new StringBuffer();

        ClamScanner scanner = new ClamScanner();
        scanner.setURL(args[0]);
        
        int count = Integer.getInteger("clam.test.count", 1).intValue();
        
        for (int iter = 0; iter < count; iter++) {
            for (int i = 1; i < args.length; i++) {
                info.setLength(0);
                InputStream is = new FileInputStream(args[i]);
                Result result = scanner.accept(new FileInputStream(args[i]), info);
                is.close();
                System.out.println("result=" + result + " file=" + args[i] + " info=" + info.toString()); 
            }
            
        }

        if (System.getProperty("clam.test.sleep") != null) {
            System.out.println("Sleeping...");
            System.out.flush();
            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
}
