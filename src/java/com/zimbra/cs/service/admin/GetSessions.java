/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.Session.Type;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GetSessions extends AdminDocumentHandler {

    private static final String SESSION_KEY = "GetSessionsCachedResult";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Server localServer = Provisioning.getInstance().getLocalServer();
        checkRight(zsc, context, localServer, Admin.R_getSessions);
        
        Element response = zsc.createElement(AdminConstants.GET_SESSIONS_RESPONSE);

        String typeStr = request.getAttribute(AdminConstants.A_TYPE);
        Type type;
        try {
            type = Type.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("invalid session type: "+typeStr, e);
        }

        SortBy sortBy = SortBy.fromString(request.getAttribute(AdminConstants.A_SORT_BY, SortBy.nameAsc.name()));
        long offset = request.getAttributeLong(AdminConstants.A_OFFSET, 0);
        long limit = request.getAttributeLong(AdminConstants.A_LIMIT, Integer.MAX_VALUE);
        boolean refresh = request.getAttributeBool(AdminConstants.A_REFRESH, false);

        AdminSession adminSession = (AdminSession) getSession(zsc, Session.Type.ADMIN);
        CachedResult result = getResult(adminSession, type, refresh, sortBy);
        List<SessionInfo> sessions = result.sessions;

        int i, limitMax = (int)(offset+limit);
        for (i=(int)offset; i < limitMax && i < sessions.size(); i++) {
            sessions.get(i).encodeSession(response);
        }

        response.addAttribute(AdminConstants.A_MORE, i < sessions.size());
        response.addAttribute(AdminConstants.A_TOTAL, sessions.size());

        return response;
    }

    enum SortBy {
        nameAsc, nameDesc, createdAsc, createdDesc, accessedAsc, accessedDesc;

        public static SortBy fromString(String s) throws ServiceException {
            try {
                return SortBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid sortBy: "+s+", valid values: "+ Arrays.asList(SortBy.values()), e);
            }
        }
    }

    static class SessionInfo {

        SessionInfo(Session s, String name) {
            mAccountName = name;
            mAccountId = s.getAuthenticatedAccountId();
            mSessionId = s.getSessionId();
            mCreated = s.getCreationTime();
            mAccessed = s.getLastAccessTime();
        }

        private String mAccountName;
        private String mAccountId;
        private String mSessionId;
        private long mCreated;
        private long mAccessed;

        public String getAccountName() { return mAccountName; }
        public String getAccountId() { return mAccountId; }
        public String getSessionId() { return mSessionId; }
        public long getCreated() { return mCreated; }
        public long getAccessed() { return mAccessed; }

        void encodeSession(Element parent) {
            Element sElt = parent.addElement("s");
            sElt.addAttribute(AdminConstants.A_ZIMBRA_ID, mAccountId);
            sElt.addAttribute(AdminConstants.A_NAME, mAccountName);
            sElt.addAttribute(AdminConstants.A_SESSION_ID, mSessionId);
            sElt.addAttribute(AdminConstants.A_CREATED_DATE, mCreated);
            sElt.addAttribute(AdminConstants.A_LAST_ACCESSED_DATE, mAccessed);
        }
    }

    static class CachedResult {
        List<SessionInfo> sessions;
        Type type;
        SortBy sortBy;
    }

    public CachedResult getResult(AdminSession adminSession, Type type, boolean refresh, final SortBy sortBy) {

        List<Session> sessions = SessionCache.getActiveSessions(type);
        CachedResult result = (adminSession == null || refresh) ? null : (CachedResult) adminSession.getData(SESSION_KEY);

        if (result != null && result.type == type && result.sortBy == sortBy)
            return result;
        
        Provisioning prov = Provisioning.getInstance();

        result = new CachedResult();
        result.type = type;
        result.sortBy = sortBy;
        result.sessions = new ArrayList<SessionInfo>(sessions.size());
        for (Session s : sessions) {
            result.sessions.add(new SessionInfo(s, getName(prov, s.getAuthenticatedAccountId())));
        }

        // SORT
        Comparator<SessionInfo> comparator = new Comparator<SessionInfo>() {
            public int compare(SessionInfo a, SessionInfo b) {
                long diff;
                switch(sortBy) {
                    case nameAsc: return a.getAccountName().compareToIgnoreCase(b.getAccountName());
                    case nameDesc: return -a.getAccountName().compareToIgnoreCase(b.getAccountName());
                    case accessedAsc:
                        diff = a.getAccessed() - b.getAccessed();
                        return diff == 0 ? 0 : diff > 0 ? 1 : -1;
                    case accessedDesc:
                        diff = a.getAccessed() - b.getAccessed();
                        return diff == 0 ? 0 : diff > 0 ? -1 : 1;
                    case createdAsc:
                        diff = a.getAccessed() - b.getAccessed();
                        return diff == 0 ? 0 : diff > 0 ? 1 : -1;
                    case createdDesc:
                        diff = a.getAccessed() - b.getAccessed();
                        return diff == 0 ? 0 : diff > 0 ? -1 : 1;
                    default:
                        return 0;
                }
            }
        };
        Collections.sort(result.sessions, comparator);

        if (adminSession != null)
        adminSession.setData(SESSION_KEY, result);
        
        return result;
    }

    private static String getName(Provisioning prov, String id) {
        try {
            Account acct = prov.get(AccountBy.id, id);
            return acct == null ? id : acct.getName();
        } catch (ServiceException e) {
            return id;
        }
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getSessions);
    }
}
