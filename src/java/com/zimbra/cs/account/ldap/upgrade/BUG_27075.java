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
package com.zimbra.cs.account.ldap.upgrade;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Version;
import com.zimbra.cs.account.AttributeCardinality;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeInfo;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.util.BuildInfo;

public class BUG_27075 extends UpgradeOp {

    private Version mSince;

    @Override
    boolean parseCommandLine(CommandLine cl) {
        String[] args = cl.getArgs();
        if (args == null || args.length != 1) {
            LdapUpgrade.usage(null, this, "missing required argument: since");
            return false;
        }

        try {
            mSince = new Version(args[0]);
        } catch (ServiceException e) {
            LdapUpgrade.usage(null, this, "invalid version: " + args[0]);
            return false;
        }
        return true;
    }

    @Override
    void usage(HelpFormatter helpFormatter) {
        printer.println();
        printer.println("args for bug " + bug + ":");
        printer.println("    {since}  (e.g. 5.0.12)");
        printer.println();
    }

    @Override
    void doUpgrade() throws ServiceException {
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            doGlobalConfig(zlc);
            doAllCos(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    /*
     * return values as: value1, value2, value3, ...
     */
    private String formatMultiValue(Collection<String> values) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String v : values) {
            if (!first)
                sb.append(", ");
            sb.append(v);
            first = false;
        }
        return sb.toString();
    }

    private boolean needsUpgrade(AttributeManager am, String attr, Version attrVersion) throws ServiceException  {
        String since = mSince.toString();

        if (attrVersion == null)
            return false;  // no version info, i.e. a 4.X attr, not need to upgrade

        if (!am.beforeVersion(attr, since) && !attrVersion.isFuture())
            return true;
        
        
        //
        // bug 38426
        //
        // 5.0.17_GA is after 6.0.0_BETA2
        //
        // We need to fixup:
        //    (1) 6.0.0_BETA1 -> 6.0.0_* upgrades
        //            - if the 6.0.0_BETA1 was freshly installed before a 5.0.17 attr was added
        //            - if the 6.0.0_BETA1 was upgraded from a 5.0.X before a 5.0.17 attr was added
        //    and
        //    (2) 6.0.0_BETA2 -> 6.0.0_* upgrades
        //            - if the 6.0.0_BETA2 was freshly installed before a 5.0.17 attr was added
        //            - this is fixed in 6.0.0_BETA3, if the system was upgraded to
        //              6.0.0_BETA2 from a 6.0.0_BETA1 described above, attrs added in 5.0.17
        //              that were missing in the 6.0.0_BETA1 are still missing in the 6.0.0_BETA2
        //
        if (attrVersion.compare("5.0.17") == 0) {
            boolean fromATroubledInstall = (mSince.compare("6.0.0_BETA1") == 0 || mSince.compare("6.0.0_BETA2") == 0);
            if (fromATroubledInstall) {
                return true;
            }
        }
        
        /*
         * bug 56667
         * 
         * zimbraFreebusyExchangeServerType was added in 6.0.11, *after* 7.0.0 and before 7.0.1
         * 
         * [from] 7.0.0 -> [to] higher version
         * upgrades will miss it.
         * 
         * [from] 7.0.1 and above -> [to] higher version
         * upgrades will be fine because:
         *   - if the [from] is a fresh install, it will have the default value set.
         *   - if the [from] is from an upgrade:
         *         - if from 7.0.0, fixed by this fix.
         *         - if from below 7.0.0:
         *               - if 6.0.11 and above, no problem
         *               - if below 6.0.11, taken care by the regular logic, no problem.    
         */
        if (Provisioning.A_zimbraFreebusyExchangeServerType.equalsIgnoreCase(attr)) {
            boolean fromATroubledInstall = (mSince.compare("7.0.0") == 0);
            if (fromATroubledInstall) {
                return true;
            }
        }
        
        /*
         * bug 58084
         * 
         * zimbraMailEmptyFolderBatchThreshold was added in 6.0.13, *after* 7.1.0 and before 7.1.1
         * 
         */
        if (Provisioning.A_zimbraMailEmptyFolderBatchThreshold.equalsIgnoreCase(attr)) {
            boolean fromATroubledInstall = (mSince.compare("7.0.0") >= 0 &&
                                            mSince.compare("7.1.1") < 0);
            if (fromATroubledInstall) {
                return true;
            }
        }
        
        return false;
    }

    private void doEntry(ZLdapContext zlc, Entry entry, String entryName, AttributeClass klass) throws ServiceException {

        printer.println();
        printer.println("------------------------------");
        printer.println("Upgrading " + entryName + ": ");

        AttributeManager am = AttributeManager.getInstance();

        Set<String> attrs = am.getAttrsInClass(klass);
        Map<String, Object> attrValues = new HashMap<String, Object>();
        for (String attr : attrs) {
            AttributeInfo ai = am.getAttributeInfo(attr);
            if (ai == null)
                continue;

            Version attrVersion = ai.getSince();

            if (needsUpgrade(am, attr, attrVersion)) {
                if (verbose) {
                    printer.println("");
                    printer.println("Checking " + entryName + " attribute: " + attr + "(" + attrVersion + ")");
                }

                String curVal = entry.getAttr(attr);
                if (curVal != null) {
                    // already has a value, skip it
                    if (verbose) {
                        if (ai.getCardinality() == AttributeCardinality.multi)
                            curVal = formatMultiValue(entry.getMultiAttrSet(attr));
                        printer.println("    skipping - already has value: " + curVal);
                    }
                    continue;
                }

                /*
                 * use the upgrade values if set, otherwise use the default values
                 *
                 * Note, we support the case when we need to leave the value unset
                 * on upgrades, but set a value on new installs.  In AttributeManager,
                 * if <globalConfigValueUpgrade> or <defaultCOSValueUpgrade> element
                 * is present but does not have a value, AttributeInfo.getGlobalConfigValuesUpgrade()/
                 * getDefaultCosValuesUpgrade() will return an empty List.  If the upgrade
                 * element is not present, the two methods will return null.  We check
                 * null here and if it is null then use the same default value for new
                 * installs.
                 */
                List<String> values = null;
                if (klass == AttributeClass.globalConfig) {
                    values = ai.getGlobalConfigValuesUpgrade();
                    if (values == null)
                        values = ai.getGlobalConfigValues();
                } else if (klass == AttributeClass.cos) {
                    values = ai.getDefaultCosValuesUpgrade();
                    if (values == null)
                        values = ai.getDefaultCosValues();
                } else {
                    printer.println("Internal error: invalid attribute class " + klass.name());
                    return;
                }

                if (values == null || values.size() == 0) {
                    if (verbose) {
                        printer.println("    skipping - does not have a default value");
                    }
                    continue;
                }

                attrValues.clear();
                if (ai.getCardinality() != AttributeCardinality.multi) {
                    printer.println("    setting " + entryName + " attribute " + attr + "(" + attrVersion + ")" + " to: " + values.get(0));
                    attrValues.put(attr, values.get(0));
                } else {
                    printer.println("    setting " + entryName + " attribute " + attr + "(" + attrVersion + ")" + " to: " + formatMultiValue(values));
                    attrValues.put(attr, values.toArray(new String[0]));
                }

                try {
                    modifyAttrs(zlc, entry, attrValues);
                } catch (ServiceException e) {
                    // log the exception and continue
                    printer.println("Caught ServiceException while modifying " + entryName + " attribute " + attr);
                    printer.printStackTrace(e);
                }
            }
        }
    }

    private void doBug38425(Entry entry, String entryName) {
        String theAttr = Provisioning.A_zimbraPrefMailDefaultCharset;
        String sinceVer = mSince.toString();
        String thisVer = BuildInfo.VERSION;

        if (sinceVer.startsWith("6.0.0_BETA1") && thisVer.startsWith("6.0.0_BETA2")) {
            String curVal = entry.getAttr(theAttr);
            if ("UTF-8".equalsIgnoreCase(curVal)) {
                HashMap<String,Object> attrs = new HashMap<String,Object>();
                attrs.put(theAttr, "");
                try {
                    printer.println("Unsetting " + theAttr + " on " +  entryName);
                    prov.modifyAttrs(entry, attrs);
                } catch (ServiceException e) {
                    printer.println("Caught ServiceException while unsetting " + theAttr + " on " +  entryName);
                    printer.printStackTrace(e);
                }
            }
        }
    }

    private void doGlobalConfig(ZLdapContext zlc) throws ServiceException {
        Config config = prov.getConfig();
        doEntry(zlc, config, "global config", AttributeClass.globalConfig);
    }

    private void doAllCos(ZLdapContext zlc) throws ServiceException {
        List<Cos> coses = prov.getAllCos();

        for (Cos cos : coses) {
            String name = "cos " + cos.getName();
            doEntry(zlc, cos, name, AttributeClass.cos);
            doBug38425(cos, name);
        }
    }
}
