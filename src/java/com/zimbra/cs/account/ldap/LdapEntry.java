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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 23, 2004
 *
 */
package com.zimbra.cs.account.ldap;

import java.util.Locale;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import com.zimbra.cs.account.AbstractEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 * 
 */
public class LdapEntry extends AbstractEntry {

    protected String mDn;

    LdapEntry(String dn, Attributes attrs) throws NamingException {
        super(LdapUtil.getAttrs(attrs));
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }

    void reload() throws ServiceException {
        try {
            refresh(null);
        } catch (NamingException e) {
            throw ServiceException
                    .FAILURE("unable to refresh entry: " + mDn, e);
        }
    }

    synchronized void refresh(DirContext initCtxt)
            throws NamingException, ServiceException {
        DirContext ctxt = initCtxt;
        try {
            if (ctxt == null)
                ctxt = LdapUtil.getDirContext();
            setAttrs(LdapUtil.getAttrs(ctxt.getAttributes(mDn)));
        } finally {
            if (initCtxt == null)
                LdapUtil.closeContext(ctxt);
        }
    }


    private Locale mLocale;

    public Locale getLocale() throws ServiceException {
        // Don't synchronize the entire method because Provisioning.getLocale
        // can recursively call LdapEntry.getLocale() on multiple entries.
        // If LdapEntry.getLocale() was synchronized, we might get into a
        // deadlock.
        synchronized (this) {
            if (mLocale != null)
                return mLocale;
        }
        Locale lc = Provisioning.getInstance().getLocale(this);
        synchronized (this) {
            mLocale = lc;
            return mLocale;
        }
    }

    public synchronized String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName()).append(": { dn=").append(mDn).append(
                " ");
        try {
            sb.append(getAttrs().toString());
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sb.append("}");
        return sb.toString();
    }
}
