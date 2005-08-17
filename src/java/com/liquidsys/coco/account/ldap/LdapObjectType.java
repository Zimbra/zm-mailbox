/*
 * Created on Apr 14, 2005
 *
 */
package com.liquidsys.coco.account.ldap;

import javax.naming.directory.Attributes;

import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.object.ObjectType;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LdapObjectType extends LdapEntry implements ObjectType {

    /**
     * @param dn
     * @param attrs
     */
    LdapObjectType(String dn, Attributes attrs) {
        super(dn, attrs);
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.object.ObjectType#getType()
     */
    public String getType() {
        return super.getAttr(Provisioning.A_liquidObjectType);
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.object.ObjectType#getDescription()
     */
    public String getDescription() {
        return super.getAttr(Provisioning.A_description, "");
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.object.ObjectType#isIndexingEnabled()
     */
    public boolean isIndexingEnabled() {
        return super.getBooleanAttr(Provisioning.A_liquidObjectIndexingEnabled, true);
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.object.ObjectType#isStoreMatched()
     */
    public boolean isStoreMatched() {
        return super.getBooleanAttr(Provisioning.A_liquidObjectStoreMatched, false);
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.object.ObjectType#getHandlerClass()
     */
    public String getHandlerClass() {
        return super.getAttr(Provisioning.A_liquidObjectHandlerClass, null);
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.object.ObjectType#getHandlerConfig()
     */
    public String getHandlerConfig() {
        return super.getAttr(Provisioning.A_liquidObjectHandlerConfig, null);
    }

}
