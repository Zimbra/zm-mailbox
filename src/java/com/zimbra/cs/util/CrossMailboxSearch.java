/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
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
 * Created on Mar 28, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.index.HitIdGrouper;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.MultiQueryResults;
import com.zimbra.cs.index.ProxiedHit;
import com.zimbra.cs.index.ProxiedQueryResults;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.service.util.ParseMailboxID;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;
import com.zimbra.soap.SoapTransport;


/**
 * @author tim
 */
public class CrossMailboxSearch 
{
    
    /**
     * @param allowLocal TRUE if local tasks (ie directly run the search) are OK, FALSE if they aren't.  
     * Basically this is here so that I can use this class both in the Command-Line-Tool case (where I ALWAYS
     * want to connect to the remote server even if the hostname is the same) and also the processing-a-soap-request
     * case (where I want to run the local queries as local) 
     */
    public CrossMailboxSearch(boolean allowLocal) {
        mHashMap = new HashMap();
        mAllowLocalTask = allowLocal;
    }
    
    public ZimbraQueryResults getSearchResults(String encodedAuthToken, SearchParams params) throws ServiceException {
        ZimbraQueryResults results = null;
        
        Set tasks = getTaskSet();
        
        // results from every server we're searching:
        ZimbraQueryResults[] res = new ZimbraQueryResults[tasks.size()];
        int curResult = 0;
        
        try {
            for (Iterator iter = tasks.iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry)iter.next();
                String serverID = (String)entry.getKey();
                CrossMailboxSearch.ServerSearchTask task = (CrossMailboxSearch.ServerSearchTask)entry.getValue();
                
                // hackhackhack -- lets me test server-server requests
                String foo[] = serverID.split("/");
                res[curResult++] = task.getSearchResults(foo[0], encodedAuthToken, params); 
            }
            results = new HitIdGrouper(new MultiQueryResults(res, params.getSortBy()), params.getSortBy());
        } finally {
            if (results == null) {
                // results wasn't set, must've thrown an exception.  WE MUST CLEANUP ALL RESULTS!
                for (int i = 0; i < res.length; i++) {
                    if (res[i] != null) {
                        try {
                            res[i].doneWithSearchResults();
                        } catch(ServiceException e) {
                            // eat it and keep cleaning up!
                            if (mLog.isDebugEnabled()) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        
        return results;
    }
    
    
    /**
     * @param id
     */
    public void addMailboxToSearchList(ParseMailboxID id) throws ServiceException {
        
        if (mAllServersAllMailboxes) { return; }
        
        if (id.isAllServers()) {
            assert(id.isAllMailboxIds()); // required -- see ParseMailboxID 
            // okay, ignore whatever's been built and just return a big set of everything
            mHashMap = null;
            mAllServersAllMailboxes = true;
            return;
        }
        
        CrossMailboxSearch.ServerSearchTask server;
        
        // hackhackhack
        String hostname = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
        if (!id.isLocal()) {
            hostname = id.getServer();
        }
        
        server = (CrossMailboxSearch.ServerSearchTask)(mHashMap.get(hostname));
        if (server == null) {
            if (mAllowLocalTask && hack==0 && id.isLocal()) { // hackhackhack
                server = new CrossMailboxSearch.LocalServerSearchTask();
            } else {
                server = new CrossMailboxSearch.RemoteServerSearchTask();
            }
            if (mAllowLocalTask) {
                hostname = hostname+"/"+hack;  // hackhackhack 
            }
            mHashMap.put(hostname, server);
        }
        
        if (id.isAllMailboxIds()) {
            server.setAllMailboxes();
        } else {
            server.addMailbox(id);
        }
        
        if (mAllowLocalTask) {
            hack++; // comment/uncomment this line to force the NONFIRST mailbox in the request to go via loopback
        }
    }
    
    public static void main(String[] args) {
        Zimbra.toolSetup("WARN");

        CrossMailboxSearch.CLI cli = new CrossMailboxSearch.CLI(args);
        cli.run();
    }
    
    
    /**
     * @return An iterator over Map.Entries with all of the ServerSearchTasks to run
     * @throws ServiceException
     */
    private Set /* Map.Entry<String srv, ServerSearchTask task> */ getTaskSet() throws ServiceException {
        if (mHashMap == null) {
            HashMap toRet /* Map.Entry<ServerID, ServerSearchTask> */ = new HashMap();
            
            Provisioning prov = Provisioning.getInstance();
            
            List cos = prov.getAllServers();
            for (Iterator it=cos.iterator(); it.hasNext(); ) {
                Server s = (Server)(it.next());
                
                ServerSearchTask task = new RemoteServerSearchTask();
                task.setAllMailboxes();
                
                toRet.put(s.getName(), task);
            }
            
            return toRet.entrySet();
        } else {
            return mHashMap.entrySet();
        }
    }
    
    private static Log mLog = LogFactory.getLog(CrossMailboxSearch.class);
    
    private HashMap /* ServerID, ServerSearchTask */ mHashMap;
    private boolean mAllServersAllMailboxes = false;
    private boolean mAllowLocalTask = true;
    private int hack = 0; // hackhack
    
    
    private static abstract class ServerSearchTask {
        private boolean allMboxes = false;
        protected List /* ParseMailboxID */ mboxes = new ArrayList();
        
        public void setAllMailboxes() { allMboxes = true; }
        public boolean isAllMailboxes() { return allMboxes; }
        
        public void addMailbox(ParseMailboxID id) {
            if (!allMboxes) {
                mboxes.add(id);
            }
        }
        
        public abstract ZimbraQueryResults getSearchResults(String serverID, String encodedAuthToken, SearchParams params) throws ServiceException;
    }
    
    private static class LocalServerSearchTask extends ServerSearchTask {
        public ZimbraQueryResults getSearchResults(String serverID, String encodedAuthToken, SearchParams params) throws ServiceException
        {
            assert(serverID.equals(Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname)));
            ZimbraQueryResults[] toRet = null;
            boolean OK = false;
            try {
                if (isAllMailboxes()) {
                    int ids[] = Mailbox.getMailboxIds();
                    toRet = new ZimbraQueryResults[ids.length];
                    for (int i = 0; i < ids.length; i++) {
                        Mailbox mbx = Mailbox.getMailboxById(ids[i]);
                        // FIXME: really shouldn't use null as the OperationContext here...
                        toRet[i] = mbx.search(null, params.getQueryStr(), params.getTypes(), params.getSortBy(), 100);
                    }
                } else {
                    toRet = new ZimbraQueryResults[mboxes.size()];
                    int i = 0;
                    for (Iterator iter = mboxes.iterator(); iter.hasNext();) {
                        ParseMailboxID id = (ParseMailboxID)iter.next();
                        assert(id.isLocal());
                        // FIXME: really shouldn't use null as the OperationContext here...
                        toRet[i++] = id.getMailbox().search(null, params.getQueryStr(), params.getTypes(), params.getSortBy(), 100);
                    }
                }
                
//                ZimbraQueryResults multi = new HitIdGrouper(new MultiQueryResults(toRet, params.sortBy), params.sortBy);
                ZimbraQueryResults multi = new MultiQueryResults(toRet, params.getSortBy());

                OK = true; // for finally block
                
                return multi;
                
            } catch(IOException e) {
                throw ServiceException.FAILURE("IO Error", e);
            } catch(ParseException e) {
                throw ServiceException.FAILURE("Parse exception for query: "+params.getQueryStr(), e);
            } finally {
                // BE CAREFUL!  We need to make sure we always clean up the results we have -- even if we
                // fail out!
                if (!OK) {
                    // oops!  back out!
                    if (toRet != null) {
                        for (int i = 0; i < toRet.length; i++) {
                            if (toRet[i] != null) {
                                try {
                                    toRet[i].doneWithSearchResults();
                                } catch(ServiceException e) {
                                    // eat it and keep cleaning up!
                                    if (mLog.isDebugEnabled()) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                } // OK check
            } //finally
        } // function getSearchResults
    }
    
    private static class RemoteServerSearchTask extends ServerSearchTask {
        public ZimbraQueryResults getSearchResults(String serverID, String encodedAuthToken, SearchParams params) throws ServiceException
        {
            if (isAllMailboxes()) {
                return new ProxiedQueryResults(encodedAuthToken, serverID, params, ProxiedQueryResults.SEARCH_ALL_MAILBOXES);
            } else {
                List /* ParseMailboxID */ ids = new ArrayList();
                
                for (Iterator iter = mboxes.iterator(); iter.hasNext();) {
                    ParseMailboxID id = (ParseMailboxID)iter.next();
                    ids.add(id);
//                    assert(id.getServer().equals(serverID));
                }
                return new ProxiedQueryResults(encodedAuthToken, serverID, params, ids);
            }
        }
    }
    

    private static class CLI 
    {
        private String[] mArgs;
        
        public CLI(String[] args) {
            mArgs = args;
        }
        
        private void auth(String admin, String pwd, int port, String host) throws SoapFaultException, IOException, ServiceException {
            URL src = new URL("https", host, port, ZimbraServlet.ADMIN_SERVICE_URI);
            SoapTransport transport = new SoapHttpTransport(src.toExternalForm()); 
            
            // using XML for now
            Element authReq = new Element.XMLElement(AdminService.AUTH_REQUEST);
            
            authReq.addAttribute(AdminService.E_NAME, admin);
            authReq.addAttribute(AdminService.E_PASSWORD, pwd);
            
            Element authResp = transport.invokeWithoutSession(authReq);
            mAuthToken = authResp.getAttribute(AdminService.E_AUTH_TOKEN);
            String sessionId = authResp.getAttribute(ZimbraContext.E_SESSION_ID, null);
            transport.setAuthToken(mAuthToken);
            if (sessionId != null) {
                transport.setSessionId(sessionId);
            }
        }
        
        boolean isVerbose = false;
        
        
        public void run() {
            CommandLineParser clParser = new GnuParser();
            Options options = new Options();

            CrossMailboxSearch xmbs = new CrossMailboxSearch(false);
            
            try {
                setupCommandLineOptions(options);
                CommandLine cl = clParser.parse(options, mArgs);
                
                String query = cl.getOptionValue('q');

                String mailboxes[] = cl.getOptionValues('m');
                
                String user = cl.getOptionValue("user");
                String pw = cl.getOptionValue("pwd");
                String host = cl.getOptionValue("s");
                
                String outputDir = null;
                int offset = 0;
                int limit = 25;
                
                if (cl.hasOption("d")) {
                    outputDir = cl.getOptionValue("d");
                }
                if (cl.hasOption("o")) {
                    String ostr = cl.getOptionValue("o");
                    offset = Integer.parseInt(ostr);
                    if (offset < 0) {
                        offset = 0;
                    }
                }
                if (cl.hasOption("l")) {
                    String lstr = cl.getOptionValue("l");
                    limit = Integer.parseInt(lstr);
                    if (limit < 0) {
                        limit = 0;
                    }
                }
                
                int  port = Integer.parseInt(cl.getOptionValue("p"));
                
                if (cl.hasOption("v")) {
                    isVerbose = true;
                }

                if (isVerbose) {
                    System.out.println("Attempting to authenticate against "+host+":"+port+" u="+user+" p="+pw);
                }
                auth(user, pw, port, host);
                
                String sortStr = MailboxIndex.SORT_BY_DATE_DESCENDING;
                String typesStr = MailboxIndex.GROUP_BY_MESSAGE;
                
                // set search params
                SearchParams params = new SearchParams();
                params.setOffset(offset);
                params.setLimit(limit);
                params.setQueryStr(query);
                params.setTypesStr(typesStr);
                params.setSortByStr(sortStr);
                
                // setup mailbox list
                for (int i = 0; i < mailboxes.length; i++) {
                    // HACK to get around eclipse IDE fuckedupness!
                    if (mailboxes[i].equals("\\\\*")) {
                        mailboxes[i] = "*";
                    }
                    xmbs.addMailboxToSearchList(ParseMailboxID.parse(mailboxes[i]));
                }

                ZimbraQueryResults res = xmbs.getSearchResults(mAuthToken, params);
                
                File outputDirFile = null;
                if (outputDir != null) {
                    outputDirFile = new File(outputDir);
                }
                outputResults(res, offset, limit, outputDirFile);
                
                
//                if (cl.hasOption()) {
                    
//                }
                
            } catch (org.apache.commons.cli.ParseException e) {
                usage(e.toString(), options);
                return;
            } catch (SoapFaultException e) {
                System.err.println("Caught SoapFaultException: "+e);
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("Caught IOException: "+e);
                e.printStackTrace();
            } catch (ServiceException e) {
                System.err.println("Caught ServiceException: "+e);
                e.printStackTrace();
            }
            
        }
        
        /**
         * @param options
         */
        private void setupCommandLineOptions(Options options) 
        {
            Option q = new Option("q", "query", true, "Query String");
            q.setRequired(true);
            options.addOption(q);
            
            Option m = new Option("m", "mbox", true, "ID of Mailbox (see below for more info)");
            m.setRequired(true);
            m.setValueSeparator(',');
            options.addOption(m);
            
            Option s = new Option("s", "server", true, "ID ");
            s.setRequired(true);
            options.addOption(s);
            
            Option port = new Option("p", "port", true, "Mail server port number");
            port.setRequired(true);
            options.addOption(port);
            
            Option u = new Option("user", "username", true, "Admin username");
            u.setRequired(true);
            options.addOption(u);
            
            Option p = new Option("pwd", "password", true, "Admin password");
            p.setRequired(true);
            options.addOption(p);
            
            
            options.addOption("d", "dir", true, "Directory to write messages to.  If none is specified then only the headers are fetched.");

            options.addOption("o", "offset", true, "Offset in hit list to start at [default is 0]");
            options.addOption("l", "limit", true, "Limit number of results to [default is 25]");
            
            options.addOption("v", "verbose", false, "Print status messages while executing");
            
            
            
        }

        private void usage(String message, Options options) {
            System.err.println(message);
            HelpFormatter formatter = new HelpFormatter();
            System.out.println("java " + CrossMailboxSearch.class.getName() + " <options> ");
            formatter.printHelp("Available options:", options);
        }
        
        private int saveMessageBody(File destDir, int resultId, String server, String mailboxIdStr, int itemId) 
        throws ServiceException
        {
            String path = "/service/content/get"; 
            
            HttpState initialState = new HttpState();
            // forward auth token as a cookie
            Cookie cookie = new Cookie(
                    server,
                    ZimbraServlet.COOKIE_ZM_AUTH_TOKEN, 
                    mAuthToken, 
                    "/", -1, false);
            

            initialState.addCookie(cookie);
            initialState.setCookiePolicy(CookiePolicy.COMPATIBILITY);
            HttpClient client = new HttpClient();
            client.setState(initialState);
            
//            System.out.println("cookie sent: domain=" + cookie.getDomain() + " name=" + cookie.getName() +
//                    " value=" + cookie.getValue() + " path=" + cookie.getPath());
            
            // make the get
            Server svr = Provisioning.getInstance().getServerByName(server);
            String url = URLUtil.getMailURL(svr, path + "?id=" + mailboxIdStr + "/" + itemId);
            GetMethod get = new GetMethod(url);
            client.setConnectionTimeout(30000);
            int statusCode = -1;
            
            StringBuffer fileName = new StringBuffer();
            fileName.append(resultId);
            fileName.append(mailboxIdStr);
            fileName.append('-');
            fileName.append(itemId);

            // have to replace the /'s with something legal for filenames!
            String fn = fileName.toString().replace('/', '-');

            File outputFile = new File(destDir, fn);
            try {
                statusCode = client.executeMethod(get);
                
//                System.out.println("Code = "+statusCode);

                if (statusCode == 200) {
                    OutputStream out = new FileOutputStream(outputFile);
                    InputStream in = get.getResponseBodyAsStream();
                    byte[] buffer = new byte[10000];
                    int len ;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                    in.close();
                    out.close();
                } else {
                    System.err.println("HTTP Error ("+statusCode+")attempting to retrieve result "+resultId+" from /"+server+"/"+mailboxIdStr+"/"+itemId);
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                get.releaseConnection();
            }
            
            return statusCode; 
        }
        
        
        private String mAuthToken = null;
        
        private void outputResults(ZimbraQueryResults res, int offset, int limit, File outputDirectory) throws ServiceException 
        {
            for (ZimbraHit cur = res.skipToHit(offset); ((cur != null) && (offset<limit)); cur = res.getNext()) {
                //System.out.println(offset+": "+cur.toString());
                if (cur instanceof ProxiedHit) {
                    ProxiedHit ph = (ProxiedHit)cur;
                    
                    StringBuffer output = new StringBuffer();

                    output.append(offset);
                    output.append(") ");
                    
                    output.append(ph.getMailboxIdStr());
                    output.append('/');
                    output.append(ph.getItemId());
                    output.append(" - ");
                    Date date = new Date(cur.getDate());
                    output.append(date);
                    output.append(" - ");
                    output.append(cur.getName());
                    output.append("- ");
                    output.append(cur.getSubject());
                    output.append("- ");
                    output.append(ph.getFragment());
                    
                    System.out.println(output.toString());
                    
                    
                    if (outputDirectory != null) {
                        saveMessageBody(outputDirectory, offset, ph.getServer(), ph.getMailboxIdStr(), ph.getItemId());
                    }
                    
                    offset++;
                } else {
                    // could directly get the blob, but for now just disallow this case
                    assert(false);
                    throw ServiceException.PERM_DENIED("Error CrossMailboxSearch ran local search!");
                }
            }
        }
        

        
    }

    

}
