package com.zimbra.qa.unittest.ldap;

import java.io.PrintWriter;
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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPURL;
import com.unboundid.ldap.sdk.SearchResultEntry;

/*
 * bug 41959
 */


public class TestLdapReadTimeout {
    
    private static abstract class LdapReadTimeoutTester {
        String uri;
        String bindDN;
        String password;
        
        protected LdapReadTimeoutTester(String uri, String bindDN, String password) {
            this.uri = uri;
            this.bindDN = bindDN;
            this.password = password;
        }
        
        abstract void doTest(String dn, boolean createEntry) throws Exception;
    }

    private static class JNDITest extends LdapReadTimeoutTester {
        
        JNDITest(String uri, String bindDN, String password) {
            super(uri, bindDN, password);
        }
        
        private LdapContext connect() throws Exception {
            Hashtable<String, String> env = new Hashtable<String, String>(); 
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, uri);
            env.put(Context.REFERRAL, "follow");
            env.put("com.sun.jndi.ldap.connect.timeout", "30000");
            env.put("com.sun.jndi.ldap.read.timeout", "30000");
            env.put("com.sun.jndi.ldap.connect.pool", "false");
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, bindDN);
            env.put(Context.SECURITY_CREDENTIALS, password);
                
            LdapContext dirCtxt = null;
            long startTime = 0;
            try {
                startTime = System.currentTimeMillis();
                dirCtxt = new InitialLdapContext(env, null);
            } catch (Exception e) {
                SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("Connection attempt started at: " + fmt.format(new Date(startTime)));
                System.out.println("\nActual elapsed time for making connection= " + elapsed + "ms");
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
    
    private static class UnboundIDTest extends LdapReadTimeoutTester {
        String host;
        int port; 
        
        UnboundIDTest(String uri, String bindDN, String password) throws Exception {
            super(uri, bindDN, password);
            
            LDAPURL ldapUrl = new LDAPURL(uri);
            host = ldapUrl.getHost();
            port = ldapUrl.getPort();
        }
        
        LDAPConnection connect() throws Exception {
            LDAPConnectionOptions connOpts = new LDAPConnectionOptions();
            connOpts.setUseSynchronousMode(true);
            connOpts.setConnectTimeoutMillis(30000);
            connOpts.setResponseTimeoutMillis(30000);
            
            LDAPConnection conn = null;
            long startTime = 0;
            try {
                startTime = System.currentTimeMillis();
                conn = new LDAPConnection(connOpts, host, 389, 
                        bindDN, password);
                
            } catch (Exception e) {
                SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
                
                long elapsed = System.currentTimeMillis() - startTime;
                
                System.out.println("Connection attempt started at: " + fmt.format(new Date(startTime)));
                System.out.println("\nActual elapsed time for making connection = " + elapsed + "ms");
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
            System.out.println();
            e.printStackTrace();
        }
    }

    private static String O_HELP = "h";
    private static String O_SLEEP = "s";
    private static String O_URI = "H";
    private static String O_BINDDN = "D";
    private static String O_PASSWORD = "w";
    
    private static void usage(Options options) {
        System.out.println("\n");
        PrintWriter pw = new PrintWriter(System.out, true);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, formatter.getWidth(), 
                "zmjava " + TestLdapReadTimeout.class.getCanonicalName() + " [options]", 
                null, options, formatter.getLeftPadding(), formatter.getDescPadding(), null);
        System.out.println("\n");
        pw.flush();
    }
    /**
     * /Users/pshao/dev/workspace/sandbox/sandbox/bin>/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/java  LdapReadTimeout
     * /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/java LdapReadTimeout
     * 
     * zmjava com.zimbra.qa.unittest.TestLdapReadTimeout -s 5
     * zmjava com.zimbra.qa.unittest.TestLdapReadTimeout -H ldap://localhost:389 -D uid=zimbra,cn=admins,cn=zimbra -w zimbra -s 5
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(O_HELP, false, "print usage");
        options.addOption(O_SLEEP, true, 
                "upon hitting an Exception, minutes to wait until exiting the program.  " +  
                "If not specified, will exit immediately.");
        options.addOption(O_URI, true, "URI. e.g. ldap://localhost:389");
        options.addOption(O_BINDDN, true, "bind DN");
        options.addOption(O_PASSWORD, true, "password");
        
        CommandLine cl = null;
        try {
            CommandLineParser parser = new GnuParser();
            cl = parser.parse(options, args);
            if (cl == null) {
                throw new ParseException("");
            }
        } catch (ParseException e) {
            usage(options);
            e.printStackTrace();
            System.exit(1);
        }
        
        if (cl.hasOption(O_HELP)) {
            usage(options);
            System.exit(0);
        }
        
        String uri = null;
        String bindDN = null;
        String password = null;
        Integer minutesToWait = null;
        
        if (cl.hasOption(O_URI)) {
            uri = cl.getOptionValue(O_URI);
        } else {
            uri = "ldap://localhost:389";
        }
        
        if (cl.hasOption(O_BINDDN)) {
            bindDN = cl.getOptionValue(O_BINDDN);
        } else {
            bindDN =  "uid=zimbra,cn=admins,cn=zimbra";
        }
        
        if (cl.hasOption(O_PASSWORD)) {
            password = cl.getOptionValue(O_PASSWORD);
        } else {
            password =  "zimbra";
        }
        
        if (cl.hasOption(O_SLEEP)) {
            String wait = cl.getOptionValue(O_SLEEP);
            
            try {
                minutesToWait = Integer.valueOf(wait);
            } catch (NumberFormatException e) {
                usage(options);
                e.printStackTrace();
                System.exit(1);
            }
        }
        
        LdapReadTimeoutTester tester = null;
        
        try {
            // tester = new JNDITest(uri, bindDN, password);  // fails
            tester = new UnboundIDTest(uri, bindDN, password);    // works
            
            System.out.println("=============");
            System.out.println(tester.getClass().getCanonicalName());
            System.out.println("LDAP server URI: " + uri);
            System.out.println("bind DN: " + bindDN);
            System.out.println("=============");
            System.out.println();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        long startTime = System.currentTimeMillis();
        test(tester);
        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        System.out.println();
        System.out.println(tester.getClass().getCanonicalName());
        System.out.println("Started at: " + fmt.format(new Date(startTime)));
        System.out.println("Ended at: " + fmt.format(new Date(endTime)));
        System.out.println("Elapsed = " + (elapsed/1000) + " seconds");
        System.out.println();
        
        if (minutesToWait != null) {
            System.out.println("Sleeping for " + minutesToWait + " minutes before exiting...");
            Thread.sleep(minutesToWait * 60 * 1000);
        }
    }
}
