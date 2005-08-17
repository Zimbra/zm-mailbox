package com.liquidsys.coco.account.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import com.liquidsys.coco.account.DistributionList;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.ServiceException;

public class LdapDistributionList extends LdapNamedEntry implements DistributionList {
    private String mName;

    LdapDistributionList(String dn, Attributes attrs)
    {
        super(dn, attrs);
        mName = LdapUtil.dnToEmail(mDn);
    }

    public String getId() {
        return getAttr(Provisioning.A_liquidId);
    }

    public String getName() {
        return mName;
    }

    public void addMember(String member) throws ServiceException {
        member = member.toLowerCase();
        String[] parts = member.split("@");
        if (parts.length != 2) {
            throw ServiceException.INVALID_REQUEST("must be valid member email address: " + member, null);
        }
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            addAttr(ctxt, Provisioning.A_liquidMailForwardingAddress, member);
        } catch (NamingException ne) {
            throw ServiceException.FAILURE("add failed for member: " + member, ne);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    public void removeMember(String member) throws ServiceException {
        member = member.toLowerCase();
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            removeAttr(ctxt, Provisioning.A_liquidMailForwardingAddress, member);
        } catch (NamingException ne) {
            throw ServiceException.FAILURE("remove failed for member: " + member, ne);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    public String[] getAllMembers() throws ServiceException {
        return getMultiAttr(Provisioning.A_liquidMailForwardingAddress);
    }
}
