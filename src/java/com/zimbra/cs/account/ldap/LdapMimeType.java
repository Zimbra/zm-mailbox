/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Apr 14, 2005
 *
 */
package com.zimbra.cs.account.ldap;

import java.util.Set;
import java.util.TreeSet;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.MimeTypeInfo;

class LdapMimeType extends Entry implements LdapEntry, MimeTypeInfo {

    private String mDn;
    
    LdapMimeType(String dn, Attributes attrs, Provisioning prov) throws NamingException {
        super(LdapUtil.getAttrs(attrs), null, prov);
        mDn = dn;
    }
    
    public String getLabel() {
        return mDn;
    }

    public String[] getMimeTypes() {
        return super.getMultiAttr(Provisioning.A_zimbraMimeType);
    }

    public String getHandlerClass() {
        return super.getAttr(Provisioning.A_zimbraMimeHandlerClass, null);
    }

    public boolean isIndexingEnabled() {
        return super.getBooleanAttr(Provisioning.A_zimbraMimeIndexingEnabled, true);
    }

    public String getDescription() {
        return super.getAttr(Provisioning.A_description, "");
    }

    public Set<String> getFileExtensions() {
        String[] extensions = super.getMultiAttr(Provisioning.A_zimbraMimeFileExtension);
        Set<String> extSet = new TreeSet<String>();
        for (String ext : extensions) {
            if (ext != null) {
                extSet.add(ext.toLowerCase());
            }
        }
        return extSet;
    }

    public String getExtension() {
        return super.getAttr(Provisioning.A_zimbraMimeHandlerExtension, null);
    }

    public String getDN() {
        return mDn;
    }

    public int getPriority() {
        return super.getIntAttr(Provisioning.A_zimbraMimePriority, 0);
    }
}
