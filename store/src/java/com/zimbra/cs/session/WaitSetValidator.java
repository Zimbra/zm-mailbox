/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016, 2018 Synacor, Inc.
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
package com.zimbra.cs.session;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapTransport.DebugListener;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.zclient.ZClientException;

/**
 * Test utility for verifying the state of a running wait set
 */
public class WaitSetValidator implements DebugListener {

    private long mSendStart;
    private int mTimeout = -1;
    private int mRetryCount = -1;
    private SoapHttpTransport mTransport;
    private ZAuthToken mAuthToken;
    private long mAuthTokenLifetime;
    private long mAuthTokenExpiration;
    private DebugListener mDebugListener = null;
    
    void setVerbose(boolean value) {
        if (value) {
            mDebugListener = this;
        } else {
            mDebugListener = null;
        }
    }
    
    /**
     * @param uri URI of server we want to talk to
     */
    void soapSetURI(String uri) {
        if (mTransport != null) mTransport.shutdown();
        mTransport = new SoapHttpTransport(uri);
        if (mTimeout >= 0)
            mTransport.setTimeout(mTimeout);
        if (mRetryCount > 0)
            mTransport.setRetryCount(mRetryCount);
        if (mAuthToken != null)
            mTransport.setAuthToken(mAuthToken);
        if (mDebugListener != null)
            mTransport.setDebugListener(mDebugListener);
    }    
    
    private String serverName() {
        try {
            return new URI(mTransport.getURI()).getHost();
        } catch (URISyntaxException e) {
            return mTransport.getURI();
        }
    }
    private void checkTransport() throws ServiceException {
        if (mTransport == null)
            throw ServiceException.FAILURE("transport has not been initialized", null);
    }
    
    protected Element invoke(Element request) throws ServiceException {
        checkTransport();
        
        try {
            return mTransport.invoke(request);
        } catch (SoapFaultException e) {
            throw e; // for now, later, try to map to more specific exception
        } catch (IOException e) {
            throw ZClientException.IO_ERROR("invoke "+e.getMessage()+", server: "+serverName(), e);
        }
    }

    protected Element invokeOnTargetAccount(Element request, String targetId) throws ServiceException {
        checkTransport();
        
        String oldTarget = mTransport.getTargetAcctId();
        try {
            mTransport.setTargetAcctId(targetId);
            return mTransport.invoke(request);
        } catch (SoapFaultException e) {
            throw e; // for now, later, try to map to more specific exception
        } catch (IOException e) {
            throw ZClientException.IO_ERROR("invoke "+e.getMessage()+", server: "+serverName(), e);
        } finally {
            mTransport.setTargetAcctId(oldTarget);
        }
    }
    
    protected Element invokeQueryWaitSet(String id) throws ServiceException {
        XMLElement req = new XMLElement(AdminConstants.QUERY_WAIT_SET_REQUEST);
        req.addAttribute(MailConstants.A_WAITSET_ID, id);
        Element response = invoke(req);
        return response;
    }
    
    protected List<WaitSetSession> queryWaitSet(String id) throws ServiceException {
        Element qws = invokeQueryWaitSet(id);
        
        List<WaitSetSession> toRet = new ArrayList<WaitSetSession>();
        
        for (Iterator<Element> iter = qws.elementIterator("session"); iter.hasNext();) {
            Element selt = iter.next();
            WaitSetSession wss = new WaitSetSession();
            wss.accountId = selt.getAttribute("account");
            wss.token = selt.getAttribute("token", null);
            toRet.add(wss);
        }
        return toRet; 
    }
    
    protected boolean validateSessionStatus(WaitSetSession wss) throws ServiceException {
        XMLElement req = new com.zimbra.common.soap.Element.XMLElement(MailConstants.SYNC_REQUEST);
        req.addAttribute("token", wss.token);
        Element syncResponse = invokeOnTargetAccount(req, wss.accountId);
        String newToken = syncResponse.getAttribute("token");
        String md = syncResponse.getAttribute("md");
        
        long diff = Long.parseLong(newToken) - Long.parseLong(wss.token);
        if (!newToken.equals(wss.token) && (diff >10)) {
            System.out.println("ERROR: Account "+wss.accountId+" -- old token "+wss.token+" doesn't match new token: "+
                               newToken+" md="+md+" Difference="+diff);
            return false;
        } else {
            System.out.println("Account "+wss.accountId+" diff="+diff+" -- OK");
            return true;
        }
    }
    
    static class WaitSetSession {
        public String accountId;
        public String token;
        
        
    }
    
    
    /**
     * used to authenticate via admin AuthRequest. can only be called after setting the URI with setURI.
     * 
     * @param name
     * @param password
     * @throws ServiceException
     * @throws IOException 
     */
    public void soapAdminAuthenticate(String name, String password) throws ServiceException {
       if (mTransport == null) throw ZClientException.CLIENT_ERROR("must call setURI before calling adminAuthenticate", null);
       XMLElement req = new XMLElement(AdminConstants.AUTH_REQUEST);
       req.addElement(AdminConstants.E_NAME).setText(name);
       req.addElement(AdminConstants.E_PASSWORD).setText(password);
       Element response = invoke(req);
       mAuthToken = new ZAuthToken(response.getElement(AdminConstants.E_AUTH_TOKEN), true);
       mAuthTokenLifetime = response.getAttributeLong(AdminConstants.E_LIFETIME);
       mAuthTokenExpiration = System.currentTimeMillis() + mAuthTokenLifetime;
       mTransport.setAuthToken(mAuthToken);
    }
    
    
    public void receiveSoapMessage(Element envelope) {
        long end = System.currentTimeMillis();        
        System.out.printf("======== SOAP RECEIVE =========\n");
        System.out.println(envelope.prettyPrint());
        System.out.printf("=============================== (%d msecs)\n", end-mSendStart);
        
    }

    public void sendSoapMessage(Element envelope) {
        mSendStart = System.currentTimeMillis();
        System.out.println("========== SOAP SEND ==========");
        System.out.println(envelope.prettyPrint());
        System.out.println("===============================");
    }

    void usage() {
        System.out.println("");
        System.out.println("testwaitset -i waitsetid [-u admin_user] [-p password] [-h host]");
        System.exit(1);
    }

    private static void printError(String text) {
        PrintStream ps = System.err;
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ps, "UTF-8"));
            writer.write(text+"\n");
            writer.flush();
        } catch (UnsupportedEncodingException e) {
            ps.println(text);
        } catch (IOException e) {
            ps.println(text);
        }
    }
    
    void run(String id, String host, String user, String pw) {
        try {
            soapSetURI(host);
            soapAdminAuthenticate(user, pw);
            List<WaitSetSession> wss = queryWaitSet(id);
            boolean hasFailures = false;
            for (WaitSetSession w : wss) {
                if (!validateSessionStatus(w)) 
                    hasFailures = true;
            }
            if (hasFailures) {
                Element qws = invokeQueryWaitSet(id);
                System.out.println(qws.prettyPrint());
            }
            
        } catch (ServiceException e) {
            e.printStackTrace();
            System.out.println("Caught exception: "+e);
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        CliUtil.toolSetup();
        
        WaitSetValidator t = new WaitSetValidator();
        
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        
        options.addOption("i", "id", true, "Wait Set ID");
        options.addOption("h", "host", true, "Hostname");
        options.addOption("u", "user", true, "Username");
        options.addOption("p", "pw", true, "Password");
        options.addOption("?", "help", false, "Help");
        options.addOption("v", "verbose", false, "Verbose");
        
        CommandLine cl = null;
        boolean err = false;
        
        String[] hosts;
        String[] ids;
        
        try {
            cl = parser.parse(options, args, true);
        } catch (ParseException pe) {
            printError("error: " + pe.getMessage());
            err = true;
        }
        
        if (err || cl.hasOption('?')) {
            t.usage();
        }
        
        String id=null, host=null, user=null, pw=null;
        
        if (!cl.hasOption('i'))
            t.usage();
        
        id = cl.getOptionValue('i');

        if (cl.hasOption('h')) 
            host = cl.getOptionValue('h');
        else
            host = "http://localhost:7071/service/admin";
        
        if (cl.hasOption('u'))
            user = cl.getOptionValue('u');
        else
            user = "admin";
        
        if (cl.hasOption('p')) 
            pw = cl.getOptionValue('p');
        else
            pw = "test123";
        
        if (cl.hasOption('v'))
            t.setVerbose(true);
        
        hosts = host.split(",");
        ids = id.split(",");
        
        if (hosts.length != ids.length) {
            System.err.println("If multiple hosts or ids are specified, the same number is required of each");
            System.exit(3);
        }
        
        for (int i = 0; i < hosts.length; i++) {
            if (i > 0)
                System.out.println("\n\n");
            System.out.println("Checking server "+hosts[i]+" waitsetId="+ids[i]);
            t.run(ids[i], hosts[i], user, pw); 
        }
    }

}
