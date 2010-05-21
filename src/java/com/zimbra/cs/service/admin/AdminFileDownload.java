/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import com.zimbra.common.localconfig.LC;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.RemoteIP;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;

/**
 * Created by IntelliJ IDEA.
 * User: ccao
 * Date: Oct 1, 2008
 * Time: 12:17:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class AdminFileDownload  extends ZimbraServlet {

    private static final String ACTION_GETBP_RESULTS = "getBP" ;
    private static final String ACTION_GETSR = "getSR" ; //get search results
    private static final String ACTION_GETBP_FILE = "getBulkFile" ; //get search results
    private static final String fileFormat = "fileFormat";
    private static final String fileID = "fileID";
    
	public static final String FILE_FORMAT_MIGRATION_XML = "migrationxml";
	public static final String FILE_FORMAT_BULK_XML = "bulkxml";
	public static final String FILE_FORMAT_BULK_CSV = "csv";
	public static final String FILE_FORMAT_BULK_IMPORT_ERRORS = "errorscsv";
	public static final String FILE_FORMAT_BULK_IMPORT_REPORT = "reportcsv";

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		try {
			//check the auth token

            AuthToken authToken = getAdminAuthTokenFromCookie(req, resp);
            
            if (authToken == null)
			   return;
		    String action = req.getParameter("action") ;
            if (action != null && action.length() > 0) {
                ZimbraLog.webclient.debug("Receiving the file download request " + action) ;
            }else{
                return ;
            }
            String filename ;
            if (action.equalsIgnoreCase(ACTION_GETBP_RESULTS))  {
                String aid = req.getParameter("aid") ;
                filename = "bp_result.csv"  ;

                if (aid == null) {
                    ZimbraLog.webclient.error("Missing required parameter aid " ) ;
                    return ;
                } else {
                   /*
                    FileUploadServlet.Upload up = FileUploadServlet.fetchUpload(authToken.getAccountId(), aid, authToken);

                    if (up == null){
                      // throw ServiceException.FAILURE("Uploaded CSV file with id " + aid + " was not found.", null);
                    }else {
                        filename = "bpresult_" + up.getName() ;
                    }*/
                    ZimbraLog.webclient.debug ("Download the bulk provision status for uploaded file " + aid ) ;
                }

                resp.setHeader("Expires", "Tue, 24 Jan 2000 20:46:50 GMT");
//                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                resp.setStatus(resp.SC_OK);
                resp.setContentType("application/x-download");
                resp.setHeader("Content-Disposition", "attachment; filename=" + filename );
                writeBulkProvisionResults (resp.getOutputStream(), aid);

                return ;
            } else if (action.equalsIgnoreCase(ACTION_GETSR) )  {


                //TODO: 2. need to consider the type of search results
                filename = "search_result.csv" ;
                String query = req.getParameter("q") ;
                String domain = req.getParameter("domain") ;
                String types = req.getParameter("types") ;
                
                resp.setHeader("Expires", "Tue, 24 Jan 2000 20:46:50 GMT");
//                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                resp.setStatus(resp.SC_OK);
                resp.setContentType("application/x-download");
                resp.setHeader("Content-Disposition", "attachment; filename=" + filename );
                writeSearchResults (resp.getOutputStream(), query, domain, types, authToken);
            } else if (action.equalsIgnoreCase(ACTION_GETBP_FILE) )  {
            	String pFileId = req.getParameter(fileID);
            	String pFileFormat = req.getParameter(fileFormat);
            	String bulkFileName = null;
            	String clientFileName = null;
            	if(pFileFormat.equalsIgnoreCase(FILE_FORMAT_BULK_CSV)) {
            		bulkFileName = String.format("%s%s_bulk_%s_%s.csv", LC.zimbra_tmp_directory.value(),File.separator,authToken.getAccountId(),pFileId);
            		clientFileName = "bulk_provision.csv";
            	} else if (pFileFormat.equalsIgnoreCase(FILE_FORMAT_BULK_XML)) {
            		bulkFileName = String.format("%s%s_bulk_%s_%s.xml", LC.zimbra_tmp_directory.value(),File.separator,authToken.getAccountId(),pFileId);
            		clientFileName = "bulk_provision.xml";
            	}  else if (pFileFormat.equalsIgnoreCase(FILE_FORMAT_MIGRATION_XML)) {
            		bulkFileName = String.format("%s%s_migration_%s_%s.xml", LC.zimbra_tmp_directory.value(),File.separator,authToken.getAccountId(),pFileId);
            		clientFileName = "bulk_provision.xml";
            	}  else if (pFileFormat.equalsIgnoreCase(FILE_FORMAT_BULK_IMPORT_ERRORS)) {
            		bulkFileName = String.format("%s%s_bulk_errors_%s_%s.csv", LC.zimbra_tmp_directory.value(),File.separator,authToken.getAccountId(),pFileId);
            		clientFileName = "failed_accounts.csv";
            	} else if (pFileFormat.equalsIgnoreCase(FILE_FORMAT_BULK_IMPORT_REPORT)) {
            		bulkFileName = String.format("%s%s_bulk_report_%s_%s.csv", LC.zimbra_tmp_directory.value(),File.separator,authToken.getAccountId(),pFileId);
            		clientFileName = "accounts_report.csv";
            	}
            	if(bulkFileName != null) {
            		InputStream is = null;            	
            		try {
            			is = new FileInputStream(bulkFileName);
            		} catch (FileNotFoundException ex) {
        	        	if(is != null)
        	        		is.close();
        	        	
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                        return;
                    }
            		resp.setHeader("Expires", "Tue, 24 Jan 2000 20:46:50 GMT");
            		resp.setStatus(resp.SC_OK);
            		resp.setContentType("application/x-download");
            		resp.setHeader("Content-Disposition", "attachment; filename=" + clientFileName );            		
            		try {
						ByteUtil.copy(is, true, resp.getOutputStream(), false);
					} catch (Exception e) {
						ZimbraLog.webclient.error(e) ;
					}
            		try {
						is.close();
						File file = new File(bulkFileName);
						file.delete();
					} catch (Exception e) {
						ZimbraLog.webclient.error(e) ;
					}
            	}
            }
		} catch (Exception e) {
			ZimbraLog.webclient.error(e) ;
        	return;
		}

	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

    private static void writeBulkProvisionResults (OutputStream out, String aid)
        throws IOException {
        InputStream in = null;
        StringBuffer sb = new StringBuffer();
        try {
//            Class c = Class.forName("com.zimbra.bp.ZimbraBulkProvisionExt") ;  //this will throw NoClassFoundException
            Class c = ExtensionUtil.findClass("com.zimbra.bp.BulkProvisionStatus") ;
            Class [] params = new Class [2] ;
            params[0] = Class.forName("java.io.OutputStream") ;
            params[1] = Class.forName("java.lang.String") ;
            Method m = c.getMethod("writeBpStatusOutputStream", params) ;
            Object [] paramValue = new Object [2] ;
            paramValue[0] = out ;
            paramValue[1] = aid ;
            m.invoke(c, paramValue) ;
        } catch (Exception e) {
             ZimbraLog.webclient.error(e) ;
        } finally {
          if (in != null) in.close(  );
        }
    }

    private static void writeSearchResults (
            OutputStream out, String query, String domain, String types, AuthToken authToken)
        throws IOException {
        InputStream in = null;
        StringBuffer sb = new StringBuffer();
        try {
            Class c = ExtensionUtil.findClass("com.zimbra.bp.SearchResults") ;
            Class [] params = new Class [5] ;
            params[0] = Class.forName("java.io.OutputStream") ;
            params[1] = Class.forName("java.lang.String") ;
            params[2] = Class.forName("java.lang.String") ;
            params[3] = Class.forName("java.lang.String") ;
            params[4] = Class.forName("com.zimbra.cs.account.AuthToken")  ;
            Method m = c.getMethod("writeSearchResultOutputStream", params) ;
            Object [] paramValue = new Object [5] ;
            paramValue[0] = out ;
            paramValue[1] = query ;
            paramValue[2] = domain ;
            paramValue[3] = types ;
            paramValue[4] = authToken ;
            m.invoke(c, paramValue) ;
        } catch (Exception e) {
             ZimbraLog.webclient.error(e) ;
        } finally {
          if (in != null) in.close(  );
        }
    } 
}
