/*
 * Created on Nov 17, 2004
 */
package com.liquidsys.coco.account.ldap;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.liquidsys.coco.account.GalContact;
import com.liquidsys.coco.service.ServiceException;

/**
 * @author schemers
 */
public class LdapGalContact implements GalContact {

    private Map mAttrs;
    private String mId;
    
    public LdapGalContact(String dn, Attributes attrs, String[] galList, Map galMap) {
        mId = dn;
        mAttrs = new HashMap();
        for (int i=0; i < galList.length; i++)
            addAttr(attrs, galList[i], (String)galMap.get(galList[i]));        
    }

    private void addAttr(Attributes attrs, String accountAttr, String contactAttr) {
        if (mAttrs.containsKey(accountAttr))
            return;
        try {
            // doesn't handle multi-value attrs
            String val = LdapUtil.getAttrString(attrs, accountAttr);
            if (val != null && !val.equals(""))
                mAttrs.put(contactAttr, val);            
        } catch (NamingException e) {
            // ignore
        }
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.account.GalContact#getId()
     */
    public String getId() {
        return mId;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.account.GalContact#getAttrs()
     */
    public Map getAttrs() throws ServiceException {
        return mAttrs;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("LdapGalContact: { ");
        sb.append("id="+mId);
        sb.append("}");
        return sb.toString();
    }
}
