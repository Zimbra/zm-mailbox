/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.ldap.LdapProv;

public class LdapObjectClassHierarchy {

    /*
     * key: an objectClass
     * value set: all superior objectClass's of the key objectClass
     */
    private static Map<String, Set<String>> sSuperOCs = new HashMap<String, Set<String>>();

    private static synchronized void addToSuperOCCache(Map<String, Set<String>> addition) {
        sSuperOCs.putAll(addition);
    }

    private static synchronized Set<String> getFromCache(String key) {
        return sSuperOCs.get(key);
    }

    /**
     * returns whether oc2 is a superior OC of oc1 based on cached data.
     * To get accurate result, must be called after a call to fetchAndCacheSuperiorOCsIfNecessary.
     *
     * @param oc1
     * @param oc2
     * @return true if oc2 is a super OC of oc1
     */
    private static boolean isSuperiorOC(String oc1, String oc2) {
        oc1 = oc1.toLowerCase();
        oc2 = oc2.toLowerCase();

        Set<String> supers = getFromCache(oc1);

        if (supers == null) {
            return false;
        }

        if (supers.contains(oc2)) {
            return true;
        }

        for (String superOC : supers) {
            if (isSuperiorOC(superOC, oc2)) {
                return true;
            }
        }

        return false;
    }

    /**
     * fetch and cache all superior objectClass's of each entry in the ocs array
     * @param prov
     * @param ocs
     */
    private static void fetchAndCacheSuperiorOCsIfNecessary(LdapProv prov, String[] ocs) {

        Map<String, Set<String>> ocsToLookFor = new HashMap<String, Set<String>>();

        for (String oc : ocs) {
            String ocLower = oc.toLowerCase();
            // skip zimbra OCs
            if (ocLower.startsWith("zimbra")) {
                continue;
            }

            // publish in cache if not in yet
            if (getFromCache(ocLower) == null) {
                ocsToLookFor.put(ocLower, new HashSet<String>());
            }
        }

        // query LDAP schema if needed
        if (ocsToLookFor.size() > 0) {
            prov.searchOCsForSuperClasses(ocsToLookFor);  // query LDAP
            addToSuperOCCache(ocsToLookFor);              // put in cache
        }
    }

    /**
     * get the most specific OC among oc1s and oc2
     *
     * @param oc1s
     * @param oc2
     * @return
     */
    public static String getMostSpecificOC(LdapProv prov, String[] oc1s, String oc2) {
        fetchAndCacheSuperiorOCsIfNecessary(prov, oc1s);

        /*
         * check cache
         */
        String mostSpecific = oc2;
        for (String oc : oc1s) {
            if (isSuperiorOC(oc, mostSpecific)) {
                mostSpecific = oc;
            }
        }

        return mostSpecific;
    }

    /**
     * returns whether oc2 is a superior OC of oc1
     *
     * returns true if oc2 is a superior OC of oc1
     * returns false if oc2 is not a superior OC of oc1, or if oc1 and oc2 are unrelated
     *
     * e.g. returns true if oc1 is inetOrgPerson and oc2 is organizationalPerson
     *      returns true if oc1 is organizationalPerson and oc2 is person
     *      returns false if oc1 is amavisAccount and oc2 is inetOrgPerson
     *
     * @param prov
     * @param oc1
     * @param oc2
     * @return
     */
    public static boolean isSuperiorOC(LdapProv prov, String oc1, String oc2) {
        fetchAndCacheSuperiorOCsIfNecessary(prov, StringUtil.toStringArray(oc1));
        return isSuperiorOC(oc1, oc2);
    }

}
