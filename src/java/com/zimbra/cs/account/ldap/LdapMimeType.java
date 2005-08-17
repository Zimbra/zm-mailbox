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
        return super.getAttr(Provisioning.A_liquidMimeType);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeTypeInfo#getHandlerClass()
     */
    public String getHandlerClass() {
        return super.getAttr(Provisioning.A_liquidMimeHandlerClass, null);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeTypeInfo#isIndexingEnabled()
     */
    public boolean isIndexingEnabled() {
        return super.getBooleanAttr(Provisioning.A_liquidMimeIndexingEnabled, true);
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
        return super.getMultiAttr(Provisioning.A_liquidMimeFileExtension);
    }

}
