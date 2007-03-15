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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.common.soap.AdminConstants;

public class DumpSessions extends AdminDocumentHandler {
    
    static enum GroupSessionsBy {
        ACCOUNT, TYPE;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Element response = lc.createElement(AdminConstants.DUMP_SESSIONS_RESPONSE);
        
        boolean includeAccounts = request.getAttributeBool("includeAccts", true);
        GroupSessionsBy groupBy = GroupSessionsBy.valueOf(request.getAttribute(MailConstants.A_GROUPBY, "ACCOUNT").toUpperCase());

        List<Session> sessions = SessionCache.getActiveSessions();
        
        HashMap<Session.Type, Integer> sessionByType = new HashMap<Session.Type, Integer>(Session.Type.values().length); 
        
        for (Session s : sessions) {
            Integer prev = sessionByType.get(s.getSessionType());
            if (prev == null)
                prev = new Integer(0);
            sessionByType.put(s.getSessionType(), prev+1);
        }
        
        response.addAttribute("total", sessions.size());
        for (Map.Entry<Session.Type, Integer> e : sessionByType.entrySet()) {
            response.addAttribute(e.getKey().name().toLowerCase(), e.getValue());
        }
        
        if (includeAccounts) {
            if (groupBy == GroupSessionsBy.ACCOUNT) {
                HashMap<String/*accountid*/, List<Session>> map = new HashMap<String, List<Session>>();
                
                for (Session s : sessions) {
                    List<Session> list = map.get(s.getAccountId());
                    if (list == null) {
                        list = new ArrayList<Session>();
                        map.put(s.getAccountId(), list);
                    }
                    list.add(s);
                }
                
                
                for (Map.Entry<String, List<Session>> entry : map.entrySet()) {
                    Element acctElt = response.addElement("acct");
                    acctElt.addAttribute(MailConstants.A_ID, entry.getKey());
                    for (Session s : entry.getValue()) {
                        encodeSession(acctElt, s, false);
                    }
                }
                
            } else {
                HashMap<Session.Type, HashMap<String/*accountid*/, List<Session>>> map = 
                    new HashMap<Session.Type, HashMap<String, List<Session>>>();

                for (Session s : sessions) {
                    HashMap<String, List<Session>> typeSet = map.get(s.getSessionType());
                    if (typeSet == null) {
                        typeSet = new HashMap<String, List<Session>>();
                        map.put(s.getSessionType(), typeSet);
                    }
                    
                    List<Session> list = typeSet.get(s.getAccountId());
                    if (list == null) {
                        list = new ArrayList<Session>();
                        typeSet.put(s.getAccountId(), list);
                    }
                    list.add(s);
                }
                
                for (Map.Entry<Session.Type, HashMap<String, List<Session>>> entry : map.entrySet()) {
                    Element te = response.addElement(entry.getKey().name().toLowerCase());
                    for (Map.Entry<String, List<Session>> acct : entry.getValue().entrySet()) {
                        for (Session s : acct.getValue()) {
                            encodeSession(te, s, true);
                        }
                    }
                }
                
            }
        }
        
        
        return response;
    }
    
    private static void encodeSession(Element parent, Session s, boolean includeAcct) {
        Element sElt = parent.addElement("s");
        if (includeAcct) {
            sElt.addAttribute("account", s.getAccountId());
        }
        sElt.addAttribute(MailConstants.A_TYPE, s.getSessionType().name().toLowerCase());
        sElt.addAttribute(MailConstants.A_ID, s.getSessionId());
        sElt.addAttribute("created", s.getCreationTime());
        sElt.addAttribute("last", s.getLastAccessTime());
        s.encodeState(sElt);
        
    }
}
