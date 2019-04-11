/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Dec 20, 2004
 * @author Greg Solovyev
 * */
package com.zimbra.cs.service.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.servlet.ZimbraServlet;

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
    	
        String noDefaultImg = req.getParameter("nodef");
        boolean noDefault = false;
        if (noDefaultImg != null && !noDefaultImg.equals("") && noDefaultImg.equals("1")){
            noDefault = true;
        }
        String reqPath = req.getRequestURI(); 
        try { 
        	
        	
	        //check if this is the logger host, otherwise proxy the request to the logger host 
			String serviceHostname = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
			String logHost  = Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraLogHostname);
			if(!serviceHostname.equalsIgnoreCase(logHost)) {
				StringBuffer url = new StringBuffer("https");
				url.append("://").append(logHost).append(':').append(LC.zimbra_admin_service_port.value());
				url.append(reqPath);
				String queryStr = req.getQueryString();
				if(queryStr != null)
					url.append('?').append(queryStr);
				
				// create an HTTP client with the same cookies
		        BasicCookieStore cookieStore = new BasicCookieStore();
		        try {
		            
		            BasicClientCookie cookie = new BasicClientCookie(ZimbraCookie.COOKIE_ZM_ADMIN_AUTH_TOKEN, authToken.getEncoded());
                    cookie.setDomain(logHost);
                    cookie.setPath("/");
                    cookie.setSecure(false);
                    cookieStore.addCookie(cookie);
		        } catch (AuthTokenException ate) {
		            throw ServiceException.PROXY_ERROR(ate, url.toString());
		        }
		        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
		        clientBuilder.setDefaultCookieStore(cookieStore);
		        HttpGet get = new HttpGet(url.toString());
		        HttpClient client = clientBuilder.build();
		        HttpResponse httpResp = null;
		        try {
		            httpResp = HttpClientUtil.executeMethod(client, get);
		            int statusCode = httpResp.getStatusLine().getStatusCode();
		            if (statusCode != HttpStatus.SC_OK)
		                throw ServiceException.RESOURCE_UNREACHABLE(httpResp.getStatusLine().getReasonPhrase(), null);
		            
		            resp.setContentType("image/gif");
		            ByteUtil.copy(httpResp.getEntity().getContent(), true, resp.getOutputStream(), false);
		            return;
		        } catch (HttpException | IOException e) {
		            throw ServiceException.RESOURCE_UNREACHABLE(httpResp.getStatusLine().getReasonPhrase(), e);
		        }  finally {
                    EntityUtils.consumeQuietly(httpResp.getEntity());
                }
			}
        } catch (Exception ex) {
        	resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
        	return;
        }       
        try { 
	        
	        if(reqPath == null || reqPath.length()==0) {
	        	resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
	    		return;
	        } 
	        
	        if (mLog.isDebugEnabled())
	            mLog.debug("received request to:("+reqPath+")");

	        String reqParts[] = reqPath.split("/");
	
	        String reqFilename = reqParts[3];
	        imgName = LC.stats_img_folder.value() + File.separator + reqFilename;        	
	        try { 
	        	is = new FileInputStream(imgName);
	        } catch (FileNotFoundException ex) {//unlikely case - only if the server's files are broken
	        	if(is != null)
	        		is.close();
	        	if (!noDefault){
                    imgName = LC.stats_img_folder.value() + File.separator + IMG_NOT_AVAIL;
                    is = new FileInputStream(imgName);
                
	        	} else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }
	        }        
        } catch (Exception ex) {
        	if(is != null)
        		is.close();

        	resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "FNF image File not found");
        	return;
        }
    	resp.setContentType("image/gif");
    	ByteUtil.copy(is, true, resp.getOutputStream(), false);
    }
}
