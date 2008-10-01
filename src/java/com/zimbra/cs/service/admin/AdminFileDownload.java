package com.zimbra.cs.service.admin;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.servlet.ZimbraServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * Created by IntelliJ IDEA.
 * User: ccao
 * Date: Oct 1, 2008
 * Time: 12:17:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class AdminFileDownload  extends ZimbraServlet {

    private static final String ACTION_GETBP = "getBP" ;

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

            if (action.equalsIgnoreCase(ACTION_GETBP))  {
                String aid = req.getParameter("aid") ;
                String filename = "bp_result.csv"  ;

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
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                resp.setStatus(resp.SC_OK);
                resp.setContentType("application/x-download");
                resp.setHeader("Content-Disposition", "attachment; filename=" + filename );
                writeBulkProvisionResults (resp.getOutputStream(), aid);

                return ;
            }


            //ByteUtil.copy(new ByteArrayInputStream(rr.getMStdout()), true, resp.getOutputStream(), false);
		} catch (Exception e) {
			e.printStackTrace();
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
}
