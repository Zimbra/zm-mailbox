package com.zimbra.cs.service.admin;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.mail.Part;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.mime.MimeCompoundHeader.ContentDisposition;
import com.zimbra.cs.rmgmt.RemoteCommands;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.rmgmt.RemoteResult;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.ServerBy;

public class CollectConfigFiles extends ZimbraServlet {
	private static final String P_HOST = "host";
	private static final String DOWNLOAD_CONTENT_TYPE = "application/x-compressed";
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		try {
			//check the auth token
			AuthToken authToken = getAdminAuthTokenFromCookie(req, resp);
			if (authToken == null) 
			   return;		
			//take the host name
			Provisioning prov = Provisioning.getInstance();
			String hostName = req.getParameter(P_HOST);
			Server server = prov.get(ServerBy.name, hostName);
			if (server == null) {
				throw ServiceException.INVALID_REQUEST("server with name " + hostName + " could not be found", null);
			}		
			//call RemoteManager
			RemoteManager rmgr = RemoteManager.getRemoteManager(server);
			RemoteResult rr = rmgr.execute(RemoteCommands.COLLECT_CONFIG_FILES);
			//stream the data
			resp.setContentType(DOWNLOAD_CONTENT_TYPE);
            ContentDisposition cd = new ContentDisposition(Part.INLINE).setParameter("filename", hostName+".conf.tgz");
            resp.addHeader("Content-Disposition", cd.toString());			
			ByteUtil.copy(new ByteArrayInputStream(rr.getMStdout()), true, resp.getOutputStream(), false);
		} catch (ServiceException e) {
			returnError(resp, e);
        	return;
		}

	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

}
