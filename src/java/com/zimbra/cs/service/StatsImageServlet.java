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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Dec 20, 2004
 * @author greg
 * */
package com.zimbra.cs.service;

import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.localconfig.LC;

import java.net.*;

public class StatsImageServlet extends ZimbraServlet {

    private static Log mLog = LogFactory.getLog(StatsImageServlet.class);

    private static final String IMG_NOT_AVAIL = "imagenotavailable.gif";

    private static final String IMG_FNF = "filenotfound.gif";
    private static final String IMG_CONN_FAILED = "connfailed.gif";
    private static final String IMG_RCVD_DATA_12M = "LmtpRcvdData.12m.gif";
    private static final String IMG_RCVD_DATA_3M = "LmtpRcvdData.3m.gif";    
    private static final String IMG_RCVD_DATA_D = "LmtpRcvdData.d.gif";
    private static final String IMG_RCVD_MSGS_3M = "LmtpRcvdMsgs.3m.gif";    
    private static final String IMG_RCVD_MSGS_12M = "LmtpRcvdMsgs.12m.gif";    
    private static final String IMG_RCVD_MSGS_D = "LmtpRcvdMsgs.d.gif"; 

    //parts of image names
    private static final String IMG_RCVD_DATA = "LmtpRcvdData.";
    private static final String IMG_RCVD_MSGS = "LmtpRcvdMsgs.";
    private static final String IMG_ZIMBRA = "zimbra.";
    private static final String IMG_DB = "db.";
    private static final String IMG_STORE = "store.";
    private static final String IMG_INDEX = "index.";
    private static final String IMG_LOG = "log.";
    private static final String IMG_REDOLOG = "redolog.";    
    
    private static final String IMG_12M = "12m.";
    private static final String IMG_3M = "3m.";    
    private static final String IMG_D = "d.";
    
    private static final String IMG_GIF = "gif";


    private static final String SYSTEMWIDE_PREFIX = "Systemwide_";

    public void init() throws ServletException {
        String name = getServletName();
        mLog.info("Servlet " + name + " starting up");
        super.init();
    }

    public void destroy() {
        String name = getServletName();
        mLog.info("Servlet " + name + " shutting down");
        super.destroy();
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
    {
        AuthToken authToken = getAdminAuthTokenFromCookie(req, resp);
        if (authToken == null) 
            return;
        
        String imgName = IMG_NOT_AVAIL;
        InputStream is = null;
        boolean imgAvailable = true;
        boolean localServer = false;
        boolean systemWide = false;
        
        String serverAddr = "";

                
        //check if requested server IP is mine if yes, then find the picture, else ask another server for the picture
        String reqPath = req.getRequestURI();
        if(reqPath == null && reqPath.length()==0) {
        	resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    		return;
        } 
        
        if (mLog.isDebugEnabled())
            mLog.debug("received request to:("+reqPath+")");

        if(mLog.isDebugEnabled())
        	mLog.debug("my address is: (" + InetAddress.getLocalHost().getHostAddress()+ ")");
        
        String reqParts[] = reqPath.split("/");
    	if(reqParts.length != 7) {
        	resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    		return;
    	}
        String reqHostname = reqParts[3];
        int indexColon = reqHostname.indexOf(':');
        if (indexColon != -1)
            reqHostname = reqHostname.substring(0, indexColon);

        InetAddress localhost = InetAddress.getLocalHost();
        if (reqHostname.equalsIgnoreCase(LC.zimbra_server_hostname.value()) ||
            reqHostname.equalsIgnoreCase(localhost.getCanonicalHostName()) ||
            reqHostname.equalsIgnoreCase(localhost.getHostName()) ||
        	reqHostname.equalsIgnoreCase("localhost") ||
            reqHostname.equals("127.0.0.1")) {
        	localServer = true;
        } else if (reqHostname.equalsIgnoreCase("$y$temw1de")){
        	localServer = true;
        	systemWide = true;
        }
        else {
        	localServer = false;
        	serverAddr = reqHostname;
        }
        
        if(localServer) {
        	imgName = LC.stats_img_folder.value() + File.separator;
        	
        	if(systemWide)
        		imgName = imgName.concat(SYSTEMWIDE_PREFIX);
        	
        	//get the image from the disk
        	if(reqParts[4].equalsIgnoreCase("rcvddata")) {
        		imgName = imgName.concat(IMG_RCVD_DATA);        		
        	} else if (reqParts[4].equalsIgnoreCase("rcvdmsgs")) {
        		imgName = imgName.concat(IMG_RCVD_MSGS);        		
        	} else if (reqParts[4].equalsIgnoreCase("zimbra")) {
        		imgName = imgName.concat(IMG_ZIMBRA);        		
        	} else if (reqParts[4].equalsIgnoreCase("db")) {
        		imgName = imgName.concat(IMG_DB);        		
        	} else if (reqParts[4].equalsIgnoreCase("store")) {
        		imgName = imgName.concat(IMG_STORE);        		
        	} else if (reqParts[4].equalsIgnoreCase("index")) {
        		imgName = imgName.concat(IMG_INDEX);        		
        	} else if (reqParts[4].equalsIgnoreCase("log")) {
        		imgName = imgName.concat(IMG_LOG);        		
        	} else if (reqParts[4].equalsIgnoreCase("redolog")) {
        		imgName = imgName.concat(IMG_REDOLOG);        		
        	}

        	if(reqParts[5].equalsIgnoreCase("m")) {
        		if(reqParts[6].equalsIgnoreCase("12")) {
        			imgName = imgName.concat(IMG_12M);
        		} else if (reqParts[6].equalsIgnoreCase("3")) {
        			imgName = imgName.concat(IMG_3M);        				
        		}
        	} else if(reqParts[5].equalsIgnoreCase("d")) {
    			imgName = imgName.concat(IMG_D);        			
        	}
        	imgName = imgName.concat(IMG_GIF);
        	
        	try { 
            	is = new FileInputStream(imgName);
            } catch (FileNotFoundException ex) {
            	imgName = LC.stats_img_folder.value() + File.separator + IMG_FNF;
            }
            try { 
            	is = new FileInputStream(imgName);
            } catch (FileNotFoundException ex) {//unlikely case - only if the server's files are broken
            	resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "FNF image File not found");
            	return;
            }
        } else {
        	String serverURL = null;
        	if (req.isSecure()) {
        		serverURL = "https://".concat(serverAddr).concat(":").concat(Integer.toString(req.getServerPort())).concat(reqPath);
        	} else {
        		serverURL = "http://".concat(serverAddr).concat(":").concat(Integer.toString(req.getServerPort())).concat(reqPath);
        	}
        	try {	
                if (mLog.isDebugEnabled())
                    mLog.debug("connecting to:("+serverURL+")");
        		
        		//open connection to another machine
        		URL u = new URL(serverURL);
        		URLConnection uc =  u.openConnection();
                String authTokenStr = null;
                try {
                	authTokenStr = authToken.getEncoded();
                } catch (AuthTokenException e) {
                	// this will not happen because we already have
                    // a valid auth token object
                }
        		uc.setRequestProperty("Cookie", COOKIE_LS_ADMIN_AUTH_TOKEN.concat("=").concat(authTokenStr));
        		is = uc.getInputStream();
        	} catch (IOException ex) {
            	imgName = LC.stats_img_folder.value() + File.separator+ IMG_CONN_FAILED;
            	try {	
            		is = new FileInputStream(imgName);
            	} catch (FileNotFoundException fnfex) { //unlikely case - only if the server's files are broken
                	resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "FNF image File not found");
                	return;
            	}
        	}
        }
        if(is==null) {
        	resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot open input stream");
        	return;
        }
    	resp.setContentType("image/gif");    	
    	ByteUtil.copy(is, resp.getOutputStream());
    }

}
