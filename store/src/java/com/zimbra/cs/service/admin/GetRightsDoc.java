/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Set;

import org.dom4j.QName;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SetUtil;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.AttrRight;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.soap.DocumentDispatcher;

public class GetRightsDoc extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        
        HashSet<String> specificPackages = null;
        for (Element ePackage : request.listElements(AdminConstants.E_PACKAGE)) {
            if (specificPackages == null)
                specificPackages = new HashSet<String>();
            specificPackages.add(ePackage.getAttribute(AdminConstants.A_NAME));
        }
        
        Element response = lc.createElement(AdminConstants.GET_RIGHTS_DOC_RESPONSE);
        doGetRightsDoc(context, specificPackages, response);
        return response;
    }
    
    private void doGetRightsDoc(Map<String, Object> context, HashSet<String> specificPackages,
            Element response) throws ServiceException {
        SoapEngine engine = (SoapEngine) context.get(SoapEngine.ZIMBRA_ENGINE);
        DocumentDispatcher dispatcher = engine.getDocumentDispatcher();
        
        Map<QName, DocumentHandler> handlers = dispatcher.getHandlers();
        
        Map<String, TreeMap<String, AdminRightCheckPoint>>
            handlersWithRightsDoc = new TreeMap<String, TreeMap<String, AdminRightCheckPoint>>();
        
        for (Map.Entry<QName, DocumentHandler> handler : handlers.entrySet()) {
            // String soapName = handler.getKey().getQualifiedName();
            DocumentHandler soapHandler = handler.getValue();
            
            if (soapHandler instanceof AdminRightCheckPoint) {
                QName qname = handler.getKey();
                String pkg = soapHandler.getClass().getPackage().getName();
                
                if (specificPackages != null && !specificPackages.contains(pkg))
                    continue;
                
                TreeMap<String, AdminRightCheckPoint> handlersInPkg = handlersWithRightsDoc.get(pkg);
                if (handlersInPkg == null) {
                    handlersInPkg = new TreeMap<String, AdminRightCheckPoint>();
                    handlersWithRightsDoc.put(pkg, handlersInPkg);
                }
                    
                handlersInPkg.put(qname.getQualifiedName(), (AdminRightCheckPoint)soapHandler);
            }
        }
        
        Set<AdminRight> usedRights = new HashSet<AdminRight>();
        
        List<AdminRight> relatedRights = new ArrayList<AdminRight>();
        List<String> notes = new ArrayList<String>();
        for (Map.Entry<String, TreeMap<String, AdminRightCheckPoint>> entry : handlersWithRightsDoc.entrySet()) {
            String pkg = entry.getKey();
            Map<String, AdminRightCheckPoint> handlersInPkg = entry.getValue();
            
            Element ePackage = response.addElement(AdminConstants.E_PACKAGE);
            ePackage.addAttribute(AdminConstants.A_NAME, pkg);
            
            for (Map.Entry<String, AdminRightCheckPoint> handler : handlersInPkg.entrySet()) {
                String soapName = handler.getKey();
                AdminRightCheckPoint soapHandler = handler.getValue();
                
                relatedRights.clear();
                notes.clear();
                soapHandler.docRights(relatedRights, notes);
                
                Element eCommand = ePackage.addElement(AdminConstants.E_CMD);
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
        }
        genNotUsed(usedRights, response);
        genDomainAdminRights(context, response);
    }
    
    private void genNotUsed(Set<AdminRight> usedRights, Element response) throws ServiceException {
        Set<AdminRight> allRights = new HashSet<AdminRight>();
        allRights.addAll(RightManager.getInstance().getAllAdminRights().values());
        Set<AdminRight> notUsed = SetUtil.subtract(allRights, usedRights);
        for (AdminRight nu : notUsed)
            response.addElement("notUsed").setText(nu.getName());
    }

    @ACLTODO //  handle dynamic group
    private void genDomainAdminRights(Map<String, Object> context, Element response) throws ServiceException {
        Element eDomainAdmin = response.addElement("domainAdmin-copypaste-to-zimbra-rights-domainadmin-xml-template");
        
        SoapEngine engine = (SoapEngine) context.get(SoapEngine.ZIMBRA_ENGINE);
        DocumentDispatcher dispatcher = engine.getDocumentDispatcher();
        
        Map<QName, DocumentHandler> handlers = dispatcher.getHandlers();
        
        // keys are sorted by targetType
        // values are sets sorted by attr name
        Map<TargetType, TreeSet<String>> rights = new TreeMap<TargetType, TreeSet<String>>();
        for (TargetType tt : TargetType.values())
            rights.put(tt, new TreeSet<String>());
        
        // add our domain admin attr rights, which are generated by RightManager
        rights.get(TargetType.account).add(Admin.R_setDomainAdminAccountAndCalendarResourceAttrs.getName());
        rights.get(TargetType.calresource).add(Admin.R_setDomainAdminAccountAndCalendarResourceAttrs.getName());
        rights.get(TargetType.calresource).add(Admin.R_setDomainAdminCalendarResourceAttrs.getName());
        rights.get(TargetType.dl).add(Admin.R_setDomainAdminDistributionListAttrs.getName());
        rights.get(TargetType.domain).add(Admin.R_setDomainAdminDomainAttrs.getName());
        
        for (Map.Entry<QName, DocumentHandler> handler : handlers.entrySet()) {
            DocumentHandler soapHandler = handler.getValue();
            
            // only works for AdminDocumentHandler
            if (soapHandler instanceof AdminRightCheckPoint &&
                soapHandler instanceof AdminDocumentHandler) {
                AdminDocumentHandler adminHandler = (AdminDocumentHandler)soapHandler;
                if (adminHandler.domainAuthSufficient(context)) {
                    List<AdminRight> relatedRights = new ArrayList<AdminRight>();
                    List<String> notes = new ArrayList<String>();
                    adminHandler.docRights(relatedRights, notes);
                    
                    for (AdminRight r : relatedRights) {
                        if (r.isPresetRight()) {
                            TargetType tt = r.getTargetType();
                            rights.get(tt).add(r.getName());
                        } else if (r.isAttrRight()) {
                            Set<TargetType> tts = ((AttrRight)r).getTargetTypes();
                            for (TargetType tt : tts)
                                rights.get(tt).add(r.getName());
                        }
                    }
                }
            }
        }

        for (Map.Entry<TargetType, TreeSet<String>> entry : rights.entrySet()) {
            TargetType tt = entry.getKey();
            
            if (entry.getValue().size() > 0) {
                Element eRight = eDomainAdmin.addElement("right").addAttribute("name", "domainAdmin"+tt.getPrettyName()+"Rights").addAttribute("type", "combo");
                eRight.addElement("desc").setText("domain admin " + tt.getCode()+ " right");
                Element eRights = eRight.addElement("rights");
                for (String r : entry.getValue()) {
                    eRights.addElement("r").addAttribute("n", r);
                }
            }
        }
    }

    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
    }
}
