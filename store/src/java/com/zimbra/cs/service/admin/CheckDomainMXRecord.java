/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.CheckDomainMXRecordRequest;
import com.zimbra.soap.admin.message.CheckDomainMXRecordResponse;

import  javax.naming.*;
import  javax.naming.directory.*;

import  java.util.Hashtable;

public class CheckDomainMXRecord extends AdminDocumentHandler {

    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        CheckDomainMXRecordRequest req = zsc.elementToJaxb(request);
        DomainBy domainBy = req.getDomain().getBy().toKeyDomainBy();
        String value = req.getDomain().getKey();

        Domain domain = prov.get(domainBy, value);

        checkDomainRight(zsc, domain, Admin.R_checkDomainMXRecord);

        String SMTPHost = domain.getAttr(Provisioning.A_zimbraDNSCheckHostname, true);
        String domainName = domain.getName();
        if(SMTPHost == null || SMTPHost.length()<1)
            SMTPHost = domain.getAttr(Provisioning.A_zimbraSmtpHostname, false);

        if(SMTPHost == null || SMTPHost.length()<1)
            SMTPHost = prov.getLocalServer().getAttr(Provisioning.A_zimbraSmtpHostname);

        if(SMTPHost == null || SMTPHost.length()<1)
            SMTPHost = prov.getConfig().getAttr(Provisioning.A_zimbraSmtpHostname);

        if(SMTPHost == null || SMTPHost.length()<1)
            SMTPHost = domain.getName();

        String SMTPHostMatch = String.format("^\\d+\\s%s\\.$", SMTPHost);
        ZimbraLog.soap.info("checking domain mx record");
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        String message = String.format("Domain is configured to use SMTP host: %s. None of the MX records match this name.", SMTPHost);
        // Element response = zsc.createElement(AdminConstants.CHECK_DOMAIN_MX_RECORD_RESPONSE);
        CheckDomainMXRecordResponse resp = new CheckDomainMXRecordResponse();
        boolean found = false;
        try {
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(domainName, new String[] {"MX"});
            if(attrs.size()<1) {
                throw ServiceException.FAILURE("NoMXRecordsForDomain", null);
            }
            for (NamingEnumeration<? extends Attribute> ne = attrs.getAll(); ne.hasMore(); ) {
                Attribute attr = (Attribute) ne.next();
                if (attr.size() == 1) {
                    ZimbraLog.soap.info("single attribute");
                    Object o = attr.get();
                    if (o instanceof String) {
                        String rec = o.toString();
                        ZimbraLog.soap.info("found MX record " + rec);
                        if(rec.matches(SMTPHostMatch)) {
                            found = true;
                            break;
                        }
                        resp.addEntry(rec);
                    } else {
                        String rec = new String((byte[])o);
                        ZimbraLog.soap.info("found MX attribute " + attr.getID() + " = "+ rec);
                        if(rec.matches(SMTPHostMatch)) {
                            found = true;
                            break;
                        }
                        resp.addEntry(rec);
                    }

                } else {
                    ZimbraLog.soap.info("multivalued attribute");
                    for (int i=0; i < attr.size(); i++) {
                        Object o = attr.get(i);
                        if (o instanceof String) {
                            String rec = o.toString();
                            ZimbraLog.soap.info("found MX record " + attr.getID() + "-" + Integer.toString(i) + " = " + rec);
                            if(rec.matches(SMTPHostMatch)) {
                                found = true;
                                break;
                            }
                            resp.addEntry(rec);
                        } else {
                            String rec = new String((byte[])o);
                            ZimbraLog.soap.info("found MX attribute " + attr.getID() + "-" + Integer.toString(i) + " = "+ rec);
                            if(rec.matches(SMTPHostMatch)) {
                                found = true;
                                break;
                            }
                            resp.addEntry(rec);
                            //message = String.format("%s %s", message,rec);
                        }
                    }

                }
            }
            if(found)
                resp.setCode("Ok");
            else {
                resp.setCode("Failed");
                resp.setMessage(message);
            }
        } catch (NameNotFoundException e) {
            throw ServiceException.FAILURE("NameNotFoundException", e);
        }
        catch (NamingException e) {
            throw ServiceException.FAILURE("Failed to verify domain's MX record. " + e.getMessage(), e);
        }
        return zsc.jaxbToElement(resp);
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_checkDomainMXRecord);
    }
}
