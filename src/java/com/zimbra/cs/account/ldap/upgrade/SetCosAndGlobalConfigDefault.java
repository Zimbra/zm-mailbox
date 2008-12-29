package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCardinality;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeInfo;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.BuildInfo.Version;

public class SetCosAndGlobalConfigDefault extends LdapUpgrade {

    private Version mSince;
    
    SetCosAndGlobalConfigDefault(boolean verbose) throws ServiceException {
        super(verbose);
    }
    
    @Override
    void parseCommandLine(CommandLine cl) throws ServiceException {
        String[] args = cl.getArgs();
        if (args == null || args.length != 1) {
            LdapUpgrade.usage(null, this, "missing required argument: since");
            throw ServiceException.INVALID_REQUEST("missing since", null);
        }
        
        mSince = new Version(args[0]);
    }
    
    @Override
    void usage(HelpFormatter helpFormatter) {
        System.out.println();
        System.out.println("args:");
        System.out.println("    {since}  (e.g. 5.0.12)");
        System.out.println();
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        ZimbraLdapContext zlc = new ZimbraLdapContext(true);
        try {
            doGlobalConfig(zlc);
            doAllCos(zlc);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }
    
    private void doEntry(ZimbraLdapContext zlc, Entry entry, String entryName, AttributeClass klass) throws ServiceException {
        
        System.out.println();
        System.out.println("------------------------------");
        System.out.println("Upgrading " + entryName + ": ");
        
        AttributeManager am = AttributeManager.getInstance();
        
        String since = mSince.toString();
        Set<String> attrs = am.getAttrsInClass(klass);
        Map<String, Object> attrValues = new HashMap<String, Object>();
        for (String attr : attrs) {
            AttributeInfo ai = am.getAttributeInfo(attr);
            BuildInfo.Version attrVersion = ai.getSince();
            
            if (!am.inVersion(attr, since) && !attrVersion.isFuture()) {
                if (mVerbose) {
                    System.out.println("");
                    System.out.println("Checking " + entryName + " attribute: " + attr + "(" + attrVersion + ")");
                }
                
                // already has a value, skip it
                if (entry.getAttr(attr) != null) {
                    if (mVerbose)
                        System.out.println("    already has value, skipping");
                    continue;
                }
                
                List<String> values = null;
                if (klass == AttributeClass.globalConfig)
                    values = ai.getGlobalConfigValues();
                else if (klass == AttributeClass.cos)
                    values = ai.getDefaultCosValues();
                else {
                    System.out.println("Internal error: invalid attribute class " + klass.name());
                    return;
                }
                        
                if (values == null || values.size() ==0) {
                    if (mVerbose)
                        System.out.println("    does not have a default value, skipping");
                    continue;
                }
                
                attrValues.clear();
                if (ai.getCardinality() != AttributeCardinality.multi) {
                    System.out.println("    setting " + entryName + " attribute " + attr + "(" + attrVersion + ")" + " to: " + values.get(0));
                    attrValues.put(attr, values.get(0));
                } else {
                    StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    for (String s : values) {
                        if (!first)
                            sb.append(", ");
                        sb.append(s);
                        first = false;
                    }
                    System.out.println("    setting " + entryName + " attribute " + attr + "(" + attrVersion + ")" + " to: " + sb);
                    attrValues.put(attr, values.toArray(new String[0]));
                }
                
                try {
                    LdapUpgrade.modifyAttrs(entry, zlc, attrValues);
                } catch (ServiceException e) {
                    // log the exception and continue
                    System.out.println("Caught ServiceException while modifying " + entryName + " attribute " + attr);
                    e.printStackTrace();
                } catch (NamingException e) {
                    // log the exception and continue
                    System.out.println("Caught NamingException while modifying " + entryName + " attribute " + attr);
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void doGlobalConfig(ZimbraLdapContext zlc) throws ServiceException {
        Config config = mProv.getConfig();
        doEntry(zlc, config, "global config", AttributeClass.globalConfig);
    }
    
    private void doAllCos(ZimbraLdapContext zlc) throws ServiceException {
        List<Cos> coses = mProv.getAllCos();
        
        for (Cos cos : coses)
            doEntry(zlc, cos, "cos " + cos.getName(), AttributeClass.cos);
    }
}
