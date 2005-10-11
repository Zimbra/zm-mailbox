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

/*
 * Created on Dec 20, 2004
 * @author Greg Solovyev
 * */
package com.zimbra.cs.service;

import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.localconfig.LC;

public class StatsImageServlet extends ZimbraServlet {

    private static Log mLog = LogFactory.getLog(StatsImageServlet.class);

    private static final String IMG_NOT_AVAIL = "data_not_available.gif";

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
        
        String imgName = null;
        InputStream is = null;
        boolean imgAvailable = true;
        boolean localServer = false;
        boolean systemWide = false;
        
        String serverAddr = "";
    	
        try { 
                
	        //check if requested server IP is mine if yes, then find the picture, else ask another server for the picture
	        String reqPath = req.getRequestURI();
	        if(reqPath == null && reqPath.length()==0) {
	        	resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
	    		return;
	        } 
	        
	        if (mLog.isDebugEnabled())
	            mLog.debug("received request to:("+reqPath+")");
	

	        String reqParts[] = reqPath.split("/");
	
	        String reqFilename = reqParts[3];
	        imgName = LC.stats_img_folder.value() + File.separator + reqFilename;
        } catch (Exception ex) {
        	imgName = LC.stats_img_folder.value() + File.separator + IMG_NOT_AVAIL;       	
        }       
        try { 
	        try { 
	        	is = new FileInputStream(imgName);
	        } catch (FileNotFoundException ex) {//unlikely case - only if the server's files are broken
	        	if(is != null)
	        		is.close();
	        	
	        	imgName = LC.stats_img_folder.value() + File.separator + IMG_NOT_AVAIL;
	        	is = new FileInputStream(imgName);
	        }        
        } catch (Exception ex) {
        	if(is != null)
        		is.close();

        	resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "FNF image File not found");
        	return;
        }
    	resp.setContentType("image/gif");    	
    	ByteUtil.copy(is, resp.getOutputStream());
    	is.close();
    }

}
