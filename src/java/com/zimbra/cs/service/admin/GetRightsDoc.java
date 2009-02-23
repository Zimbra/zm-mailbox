/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

import org.dom4j.QName;

import com.zimbra.common.soap.Element;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SetUtil;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.DocumentDispatcher;

/**
 * @author schemers
 */
public class GetRightsDoc extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        
        Element response = lc.createElement(AdminConstants.GET_RIGHTS_DOC_RESPONSE);
        doGetRightsDoc(context, response);
        return response;
    }
    
    private void doGetRightsDoc(Map<String, Object> context, Element response) throws ServiceException {
        SoapEngine engine = (SoapEngine) context.get(SoapEngine.ZIMBRA_ENGINE);
        DocumentDispatcher dispatcher = engine.getDocumentDispatcher();
        
        Map<QName, DocumentHandler> handlers = dispatcher.getHandlers();
        
        Map<String, AdminDocumentHandler> handlersWithRightsDoc = new TreeMap<String, AdminDocumentHandler>();
        
        for (Map.Entry<QName, DocumentHandler> handler : handlers.entrySet()) {
            // String soapName = handler.getKey().getQualifiedName();
            DocumentHandler soapHandler = handler.getValue();
            
            if (soapHandler instanceof AdminDocumentHandler) {
                QName qname = handler.getKey();
                handlersWithRightsDoc.put(qname.getQualifiedName(), (AdminDocumentHandler)soapHandler);
            }
        }
        
        Set<AdminRight> usedRights = new HashSet<AdminRight>();
        
        List<AdminRight> relatedRights = new ArrayList<AdminRight>();
        List<String> notes = new ArrayList<String>();
        for (Map.Entry<String, AdminDocumentHandler> handler : handlersWithRightsDoc.entrySet()) {
            String soapName = handler.getKey();
            AdminDocumentHandler soapHandler = handler.getValue();
            
            relatedRights.clear();
            notes.clear();
            soapHandler.docRights(relatedRights, notes);
            
            Element eCommand = response.addElement(AdminConstants.E_CMD);
            eCommand.addAttribute(AdminConstants.A_NAME, soapName);
            Element eRights = eCommand.addElement(AdminConstants.E_RIGHTS);
            
            for (AdminRight adminRight : relatedRights) {
                Element eRight = eRights.addElement(AdminConstants.E_RIGHT);
                eRight.addAttribute(AdminConstants.A_NAME, adminRight.getName());
                
                usedRights.add(adminRight);
            }
            
            Element eNotes = eCommand.addElement(AdminConstants.E_DESC);
            for (String note : notes)
                eNotes.addElement(AdminConstants.E_NOTE).setText(note);
        }
        
        // see which ones are not being applied yet
        Set<AdminRight> allRights = new HashSet<AdminRight>();
        allRights.addAll(RightManager.getInstance().getAllAdminRights().values());
        Set<AdminRight> notUsed = SetUtil.subtract(allRights, usedRights);
        for (AdminRight nu : notUsed)
            response.addElement("notUsed").setText(nu.getName());
    }

    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }
    
    @Override
    protected void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(sDocRightNotesAllowAllAdmins);
    }
}
