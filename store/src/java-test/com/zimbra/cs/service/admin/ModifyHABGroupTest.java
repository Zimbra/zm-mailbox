package com.zimbra.cs.service.admin;

import org.junit.Test;

import junit.framework.Assert;

/**
 * @author zimbra
 *
 */
public class ModifyHABGroupTest {

    @Test
    public void testIsGroupMovedtoSameOu() {
        boolean result = ModifyHABGroup.isGroupMovedtoSameOu(
            "cn=Security,ou=zimbraHab123,dc=rdesai,dc=zdev,dc=local",
            "cn=MyOrg,ou=zimbraHab123,dc=rdesai,dc=zdev,dc=local");
        Assert.assertTrue("dn do not have same ou", result);
        
        result = ModifyHABGroup.isGroupMovedtoSameOu(
            "cn=Operations,cn=Security,ou=zimbraHab123,dc=rdesai,dc=zdev,dc=local",
            "cn=MyOrg,ou=zimbraHab123,dc=rdesai,dc=zdev,dc=local");
        Assert.assertTrue("dn do not have same ou", result);
        
        result = ModifyHABGroup.isGroupMovedtoSameOu(
            "cn=Operations,cn=Security,ou=zimbraHab1234,dc=rdesai,dc=zdev,dc=local",
            "cn=MyOrg,ou=zimbraHab123,dc=rdesai,dc=zdev,dc=local");
        Assert.assertFalse("dn have same ou", false);
        
        result = ModifyHABGroup.isGroupMovedtoSameOu(
            "cn=Operations,cn=Security,ou=zimbraHab1234,dc=rdesai,dc=zdev,dc=local",
            "cn=MyOrg,dc=rdesai,dc=zdev,dc=local");
        Assert.assertFalse("dn have same ou", false);
    }

}
