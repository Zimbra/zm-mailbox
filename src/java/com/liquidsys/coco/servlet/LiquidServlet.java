/*
 * Created on 2005. 4. 5.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.account.AuthToken;
import com.liquidsys.coco.account.AuthTokenException;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.LiquidLog;
import com.liquidsys.soap.SoapProtocol;

/**
 * @author jhahm
 *
 * Superclass for all Liquid servlets.  Supports port filtering and
 * provides some utility methods to subclasses.
 */
public class LiquidServlet extends HttpServlet {

    private static Log mLog = LogFactory.getLog(LiquidServlet.class);

    public static final String USER_SERVICE_URI  = "/service/soap/";
    public static final String ADMIN_SERVICE_URI = "/service/admin/soap/";
    public static final String MBOX_MOVE_URI     = "/service/admin/mboximport";

    public static final String COOKIE_LS_AUTH_TOKEN       = "LS_AUTH_TOKEN";
    public static final String COOKIE_LS_ADMIN_AUTH_TOKEN = "LS_ADMIN_AUTH_TOKEN"; 

    private static final String PARAM_ALLOWED_PORTS  = "allowed.ports";

    private int[] mAllowedPorts;

    public void init() throws ServletException {
        String portsCSV = getInitParameter(PARAM_ALLOWED_PORTS);
        if (portsCSV != null) {
            // Split on zero-or-more spaces followed by comma followed by
            // zero-or-more spaces.
            String[] vals = portsCSV.split("\\s*,\\s*");
            if (vals == null || vals.length == 0)
                throw new ServletException("Must specify comma-separated list of port numbers for " +
                                           PARAM_ALLOWED_PORTS + " parameter");
            mAllowedPorts = new int[vals.length];
            for (int i = 0; i < vals.length; i++) {
                try {
                    mAllowedPorts[i] = Integer.parseInt(vals[i]);
                } catch (NumberFormatException e) {
                    throw new ServletException("Invalid port number \"" + vals[i] + "\" in " +
                                               PARAM_ALLOWED_PORTS + " parameter");
                }
                if (mAllowedPorts[i] < 1)
                    throw new ServletException("Invalid port number " + mAllowedPorts[i] + " in " +
                                               PARAM_ALLOWED_PORTS + " parameter; port number must be greater than zero");
            }
        }
    }

    /**
     * Filter the request based on incoming port.  If the allowed.ports
     * parameter is specified for the servlet, the incoming port must
     * match one of the listed ports.
     */
    protected void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        if (mAllowedPorts != null && mAllowedPorts.length > 0) {
            int incoming = request.getServerPort();
            boolean allowed = false;
            for (int i = 0; i < mAllowedPorts.length; i++) {
                if (mAllowedPorts[i] == incoming) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                SoapProtocol soapProto = SoapProtocol.Soap12;
                Element fault = SoapProtocol.Soap12.soapFault(
                        ServiceException.FAILURE("Request not allowed on port " + incoming, null));
                Element envelope = SoapProtocol.Soap12.soapEnvelope(fault);
                byte[] soapBytes = envelope.toUTF8();
                response.setContentType(soapProto.getContentType());
                response.setBufferSize(soapBytes.length + 2048);
                response.setContentLength(soapBytes.length);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getOutputStream().write(soapBytes);
                return;
            }
        }
        super.service(request, response);
    }

    /**
     * read until EOF is reached
     */
    protected byte[] readUntilEOF(InputStream input) throws IOException {
        final int SIZE = 2048;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(SIZE);
        byte[] buffer = new byte[SIZE];

        int n = 0;
        while ((n = input.read(buffer, 0, SIZE)) > 0)
            baos.write(buffer, 0, n);
        return baos.toByteArray();
    }

    protected void readFully(InputStream in, byte b[], int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0)
            throw new java.io.EOFException();
            n += count;
        }
    }

    protected static AuthToken getAuthTokenFromCookie(HttpServletRequest req,
                                                      HttpServletResponse resp)
    throws IOException {
        return getAuthTokenFromCookieImpl(req, resp, COOKIE_LS_AUTH_TOKEN, false);
    }

    protected static AuthToken getAuthTokenFromCookie(HttpServletRequest req,
                                                      HttpServletResponse resp,
                                                      boolean doNotSendHttpError)
    throws IOException {
        return getAuthTokenFromCookieImpl(req, resp, COOKIE_LS_AUTH_TOKEN, doNotSendHttpError);
    }

    protected static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req,
                                                           HttpServletResponse resp)
    throws IOException {
        return getAuthTokenFromCookieImpl(req, resp, COOKIE_LS_ADMIN_AUTH_TOKEN, false);
    }

    private static AuthToken getAuthTokenFromCookieImpl(HttpServletRequest req,
                                                        HttpServletResponse resp,
                                                        String cookieName,
                                                        boolean doNotSendHttpError)
    throws IOException {
        String authTokenStr = null;
        Cookie cookies[] =  req.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(cookieName)) {
                    authTokenStr = cookies[i].getValue();
                    break;
                }
            }
        }

        if (authTokenStr == null) {
            if (!doNotSendHttpError)
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "no authtoken cookie");
            return null;
        }

        try {
            AuthToken authToken = AuthToken.getAuthToken(authTokenStr);
            if (authToken.isExpired()) {
                if (!doNotSendHttpError)
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "authtoken expired");
                return null;
            }
            return authToken;
        } catch (AuthTokenException e) {
            if (!doNotSendHttpError)
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "unable to parse authtoken");
            return null;
        }
    }

    protected void proxyPost(HttpServletRequest req, HttpServletResponse resp,
                             String realHost, String realPort,
                             byte[] postBody)
    throws IOException {
        String port;
        if (realPort != null)
            port = ":" + realPort;
        else
            port = "";
        
        String uri = req.getScheme() + "://" + realHost + port + "/" + req.getRequestURI();
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(uri);

        method.setRequestHeader("Content-type", req.getContentType());
        ByteArrayInputStream bais = new ByteArrayInputStream(postBody);
        method.setRequestBody(bais);
        //method.setRequestContentLength(EntityEnclosingMethod.CONTENT_LENGTH_AUTO);
        method.setRequestContentLength(postBody.length);
        
        int statusCode = -1;
        int retryCount = 3;
        
        for (int attempt = 0; statusCode == -1 && attempt < retryCount; attempt++) {
            try {
                // execute the method.
                statusCode = client.executeMethod(method);
            } catch (HttpRecoverableException e) {
                System.err.println(
                        "A recoverable exception occurred, retrying." + 
                        e.getMessage());
            }
        }
        // Check that we didn't run out of retries.
        if (statusCode == -1)
            throw new IOException("retry limit reached");
        
        // Read the response body.
        byte[] responseBody = method.getResponseBody();
        
        // Release the connection.
        method.releaseConnection();

        if (LiquidLog.soap.isDebugEnabled())
            LiquidLog.soap.debug("response: \n" + new String(responseBody, "utf8"));
        
        // send response back to client
        resp.setContentType(req.getContentType());
        resp.setBufferSize(responseBody.length);
        resp.setContentLength(responseBody.length);
        resp.setStatus(statusCode);
        resp.getOutputStream().write(responseBody);
    }
}
