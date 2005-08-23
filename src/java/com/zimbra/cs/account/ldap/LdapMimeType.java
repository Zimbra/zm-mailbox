/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
 * Created on Apr 14, 2005
 *
 */
package com.zimbra.cs.account.ldap;

import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.MimeTypeInfo;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LdapMimeType extends LdapEntry implements MimeTypeInfo {

    /**
     * @param dn
     * @param attrs
     */
    LdapMimeType(String dn, Attributes attrs) {
        super(dn, attrs);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeTypeInfo#getMimeType()
     */
    public String getType() {
        return super.getAttr(Provisioning.A_zimbraMimeType);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeTypeInfo#getHandlerClass()
     */
    public String getHandlerClass() {
        return super.getAttr(Provisioning.A_zimbraMimeHandlerClass, null);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeTypeInfo#isIndexingEnabled()
     */
    public boolean isIndexingEnabled() {
        return super.getBooleanAttr(Provisioning.A_zimbraMimeIndexingEnabled, true);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeTypeInfo#getDescription()
     */
    public String getDescription() {
        return super.getAttr(Provisioning.A_description, "");
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeTypeInfo#getFileExtensions()
     */
    public String[] getFileExtensions() {
        return super.getMultiAttr(Provisioning.A_zimbraMimeFileExtension);
    }

}
