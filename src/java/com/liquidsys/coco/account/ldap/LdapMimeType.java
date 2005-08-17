/*
 * Created on Apr 14, 2005
 *
 */
package com.liquidsys.coco.account.ldap;

import javax.naming.directory.Attributes;

import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.mime.MimeTypeInfo;

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
     * @see com.liquidsys.coco.mime.MimeTypeInfo#getMimeType()
     */
    public String getType() {
        return super.getAttr(Provisioning.A_liquidMimeType);
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeTypeInfo#getHandlerClass()
     */
    public String getHandlerClass() {
        return super.getAttr(Provisioning.A_liquidMimeHandlerClass, null);
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeTypeInfo#isIndexingEnabled()
     */
    public boolean isIndexingEnabled() {
        return super.getBooleanAttr(Provisioning.A_liquidMimeIndexingEnabled, true);
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeTypeInfo#getDescription()
     */
    public String getDescription() {
        return super.getAttr(Provisioning.A_description, "");
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeTypeInfo#getFileExtensions()
     */
    public String[] getFileExtensions() {
        return super.getMultiAttr(Provisioning.A_liquidMimeFileExtension);
    }

}
