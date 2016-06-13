/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ldap;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.Zimbra;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class LdapTODO {
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SDKTODO {
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SDKDONE {
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TODO {
    }
    
    /*
     * check all ref to DistributionList and TargetType.dl in accesscontrol package
     */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ACLTODO {
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TODOEXCEPTIONMAPPING {
        // verify if the exception mapping is indeed needed, and if so fix it.
        // test shows even in JNDI the exceptions cannot be thrown.
    }
    
    public static void TODO() {
        TODO(null);
    }
    
    public static void TODO(String message) {
        LdapException e = LdapException.TODO(); // get get the stack printed
        Zimbra.halt("TODO", e);
    }
    
    public static enum FailCode {
        /*
         * Turn JVM assertion on.  If this failure never happens, remove 
         * catching the LdapNameNotFoundException exception from all callsites.
         */
        NameNotFoundExceptionShouldNeverBeThrown,
        
        /*
         * Turn JVM assertion on.  If this failure never happens, remove 
         * catching the LdapInvalidNameException exception from all callsites.
         */
        LdapInvalidNameExceptionShouldNeverBeThrown,
        
    }
    /**
     * Should never happen.
     * 
     * Can be turned on/off by turning assertion in the JVM on/off
     */
    public static void FAIL(FailCode code) {
        ZimbraLog.ldap.error("assertion failure: " + code.name());
        assert(false);
    }
    
    
    /*
     * TODO: 
     * 
     * 1. search all occurrences of casting the LdapProvisioing.
     *    they should be cast to LdapProv.  Move methods to ldapProv
     *    when necessary.
     *    
     * 2. Go through Provisioning, make sure all methods are called 
     *    from one of the unit tests
     *       
     *    
     */
}
