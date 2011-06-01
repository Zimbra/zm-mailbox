package com.zimbra.qa.unittest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.SearchResultEntry;

/*
 * bug 41959
 */


public class TestLdapReadTimeout {
    
    private static interface LdapReadTimeoutTester {
        void doTest(String dn, boolean createEntry) throws Exception;
    }

    private static class JNDITest implements LdapReadTimeoutTester {
        
        private LdapContext connect() throws Exception {
            Hashtable<String, String> env = new Hashtable<String, String>(); 
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://localhost:389");
            env.put(Context.REFERRAL, "follow");
            env.put("com.sun.jndi.ldap.connect.timeout", "30000");
            env.put("com.sun.jndi.ldap.read.timeout", "30000");
            env.put("com.sun.jndi.ldap.connect.pool", "false");
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, "uid=zimbra,cn=admins,cn=zimbra");
            env.put(Context.SECURITY_CREDENTIALS, "zimbra");
                
            LdapContext dirCtxt = null;
            long startTime = 0;
            try {
                startTime = System.currentTimeMillis();
                dirCtxt = new InitialLdapContext(env, null);
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("\nActual elapsed time for \"new InitialLdapContext\"= " + elapsed + "ms");
                throw e;
            }
    
            return dirCtxt;
        }
        
        private void createEntry(LdapContext dirCtxt, String dn) throws Exception {
            
            Attributes attrs = new BasicAttributes(true);
            attrs.put("objectClass", "person");
            attrs.put("sn", "my sn");
            
            Context newCtxt = null;
            try {
                Name cpName = new CompositeName().add(dn);
                newCtxt = dirCtxt.createSubcontext(cpName, attrs);
            } finally {
                if (newCtxt != null)
                    newCtxt.close();
            }
        }
        
        private void modifyEntry(LdapContext dirCtxt, String dn) throws Exception {
            Name cpName = new CompositeName().add(dn);
            
            BasicAttribute ba = new BasicAttribute("sn");
            ba.add("hello");
            
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, ba);
            dirCtxt.modifyAttributes(cpName, mods);
        }
        
        private void getEntry(LdapContext dirCtxt, String dn) throws Exception {
            Name cpName = new CompositeName().add(dn);
            dirCtxt.getAttributes(cpName);
        }
        
        @Override
        public void doTest(String dn, boolean createEntry) throws Exception {
            
            LdapContext dirCtxt = null;
            
            try {
                // connect
                dirCtxt = connect();
                
                // do stuff
                if (createEntry) {
                    createEntry(dirCtxt, dn);
                    System.out.println("Created entry: " + dn);
                } else {
                    // modifyEntry(dirCtxt, dn);
                    getEntry(dirCtxt, dn);
                }
                
            } finally {
                // close
                if (dirCtxt != null) {
                    dirCtxt.close();
                }
            }
        }
    }
    
    private static class UBIDTest implements LdapReadTimeoutTester {
        LDAPConnection connect() throws Exception {
            LDAPConnectionOptions connOpts = new LDAPConnectionOptions();
            connOpts.setUseSynchronousMode(true);
            connOpts.setConnectTimeoutMillis(30000);
            connOpts.setResponseTimeoutMillis(30000);
            
            LDAPConnection conn = null;
            long startTime = 0;
            try {
                startTime = System.currentTimeMillis();
                conn = new LDAPConnection(connOpts, "localhost", 389, 
                        "uid=zimbra,cn=admins,cn=zimbra", "zimbra");
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("\nActual elapsed time for \"creating conneciton\"= " + elapsed + "ms");
                throw e;
            }
            
            return conn;
        }
        
        private void createEntry(LDAPConnection conn, String dn) throws Exception {
            com.unboundid.ldap.sdk.Entry entry = new com.unboundid.ldap.sdk.Entry(dn);
            entry.addAttribute("objectClass", "person");
            entry.addAttribute("sn", "my sn");
            
            LDAPResult result = conn.add(entry);
            
        }
        
        private void getEntry(LDAPConnection conn, String dn) throws Exception {
            SearchResultEntry entry = conn.getEntry(dn);
        }

        @Override
        public void doTest(String dn, boolean createEntry) throws Exception {
            
            LDAPConnection conn = null;
            
            try {
                // connect
                conn = connect();
                
                // do stuff
                if (createEntry) {
                    createEntry(conn, dn);
                    System.out.println("Created entry: " + dn);
                } else {
                    // modifyEntry(dirCtxt, dn);
                    getEntry(conn, dn);
                }
            } finally {
                if (conn != null) {
                    conn.close();
                }
            }
        }
    }
    
    private static void printSysInfo() throws Exception {
        System.out.println("System property java.home="            + System.getProperty("java.home"));
        System.out.println("System property java.runtime.version=" + System.getProperty("java.runtime.version"));
        System.out.println("System property java.version="         + System.getProperty("java.version"));
        System.out.println("System property java.vm.info="         + System.getProperty("java.vm.info"));
        System.out.println("System property java.vm.name="         + System.getProperty("java.vm.name"));
        System.out.println("System property java.vm.version="      + System.getProperty("java.vm.version"));
        System.out.println("System property os.arch="              + System.getProperty("os.arch"));
        System.out.println("System property os.name="              + System.getProperty("os.name"));
        System.out.println("System property os.version="           + System.getProperty("os.version"));
        System.out.println("System property sun.arch.data.model="  + System.getProperty("sun.arch.data.model"));
        System.out.println("System property sun.cpu.endian="       + System.getProperty("sun.cpu.endian"));
        // System.out.println("System property sun.cpu.isalist="      + System.getProperty("sun.cpu.isalist"));
        // System.out.println("System property sun.os.patch.level="   + System.getProperty("sun.os.patch.level"));
        System.out.println();
    }
    
    private static void test(LdapReadTimeoutTester tester) {
        System.out.println("=============");
        System.out.println(tester.getClass().getCanonicalName());
        System.out.println("=============");
        System.out.println();
        
        Date date = new Date();
        SimpleDateFormat fmt =  new SimpleDateFormat("yyyyMMdd-HHmmss");
        String testId =  fmt.format(date);
        String dn = "cn=" + testId;
        
        int i = 1;
        try {
            printSysInfo();
            
            // create the entry
            tester.doTest(dn, true);
            
            while (true) {
                // do the test 
                tester.doTest(dn, false);
                
                // report some progress
                if (i % 100 == 0)
                    System.out.println("Tested " + i + " iterations");
                ++i;
            }
        } catch (Exception e) {
            System.out.println("Failed at iteration: " + i);
            e.printStackTrace();
        }
    }

    /**
     * /Users/pshao/dev/workspace/sandbox/sandbox/bin>/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/java  LdapReadTimeout
     * /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/java LdapReadTimeout
     * 
     * @param args
     */
    public static void main(String[] args) {
        // LdapReadTimeoutTester tester = new JNDITest();  // fails
        LdapReadTimeoutTester tester = new UBIDTest();     // works
        
        test(tester);
    }
}
