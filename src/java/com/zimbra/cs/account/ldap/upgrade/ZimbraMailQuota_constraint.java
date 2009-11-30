package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;

public class ZimbraMailQuota_constraint extends LdapUpgrade {

    ZimbraMailQuota_constraint() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        ZimbraLdapContext zlc = new ZimbraLdapContext(true);
        try {
            doAllCos(zlc);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }

    }
    
    private void setZimbraMailQuotaConstraint(ZimbraLdapContext zlc, Cos cos) {
        long quotaOnCos = cos.getLongAttr(Provisioning.A_zimbraMailQuota, 0);
        
        // no quota limitation
        if (quotaOnCos == 0) {
            System.out.println("Skip setting constraint for " + Provisioning.A_zimbraMailQuota + " on cos " + cos.getName() + ", there is no quota limit on this cos");
            return;
        }
        
        Set<String> constraints = cos.getMultiAttrSet(Provisioning.A_zimbraConstraint);
        
        for (String constraint : constraints) {
            if (constraint.startsWith(Provisioning.A_zimbraMailQuota)) {
                System.out.println("Skip setting constraint for " + Provisioning.A_zimbraMailQuota + " on cos " + cos.getName() + ", it is currently set to " + constraint);
                return;
            }
        }
        
        // there is currently no constraint for zimbraMailQuota, add it
        String value = Provisioning.A_zimbraMailQuota + ":max=" + quotaOnCos;
        constraints.add(value);
        
        Map<String, Object> newValues = new HashMap<String, Object>();
        newValues.put(Provisioning.A_zimbraConstraint, constraints.toArray(new String[constraints.size()]));
        
        try {
            System.out.println("Modifying " + Provisioning.A_zimbraConstraint + " on cos " + cos.getName() + ", adding value " + value);
            LdapUpgrade.modifyAttrs(cos, zlc, newValues);
        } catch (ServiceException e) {
            // log the exception and continue
            System.out.println("Caught ServiceException while modifying " + Provisioning.A_zimbraConstraint + " attribute ");
            e.printStackTrace();
        } catch (NamingException e) {
            // log the exception and continue
            System.out.println("Caught NamingException while modifying " + Provisioning.A_zimbraConstraint + " attribute ");
            e.printStackTrace();
        }
    }
    
    private void doAllCos(ZimbraLdapContext zlc) throws ServiceException {
        List<Cos> coses = mProv.getAllCos();
        
        for (Cos cos : coses) {
            setZimbraMailQuotaConstraint(zlc, cos);
        }
    }

}
