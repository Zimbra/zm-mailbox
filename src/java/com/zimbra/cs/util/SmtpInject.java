package com.zimbra.cs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Properties;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple command line SMTP client for testing purposes.
 */
public class SmtpInject {

    private static Log mLog = LogFactory.getLog(SmtpInject.class);

    private static Options mOptions = new Options();
    
    static {
        mOptions.addOption("h", "help",      false, "show help text");
        mOptions.addOption("f", "file",      true,  "rfc822/MIME formatted text file");
        mOptions.addOption("a", "address",   true,  "smtp server (default localhost)");
        mOptions.addOption("s", "sender",    true,  "envelope sender (mail from)");
        Option ropt = new Option("r", "recipient", true, "envelope recipients (rcpt to)");
        ropt.setArgs(Option.UNLIMITED_VALUES);
        mOptions.addOption(ropt);
        mOptions.addOption("T", "trace",     false, "trace server/client traffic");
        mOptions.addOption("t", "tls",       false, "use TLS");
        mOptions.addOption("A", "auth",      false, "use SMTP auth");
        mOptions.addOption("u", "username",  true,  "username for SMTP auth");
        mOptions.addOption("p", "password",  true,  "password for SMTP auth");
        mOptions.addOption("v", "verbose",   false, "show provided options");
    }

    private static void usage(String errmsg) {
        if (errmsg != null) { 
            mLog.error(errmsg);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("SmtpInject [options]", mOptions);
        System.exit((errmsg == null) ? 0 : 1);
    }

    private static CommandLine parseArgs(String args[]) {
        StringBuffer gotCL = new StringBuffer("cmdline: ");
        for (int i = 0; i < args.length; i++) {
            gotCL.append("'").append(args[i]).append("' ");
        }
        //mLog.info(gotCL);
        
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }
        return cl;
    }

    public static void main(String[] args) {
        Zimbra.toolSetup();
        CommandLine cl = parseArgs(args);

        if (cl.hasOption("h")) {
            usage(null);
        }

        String file = null;
        if (!cl.hasOption("f")) {
            usage("no file specified");
        } else {
            file = cl.getOptionValue("f"); 
        }
        byte[] data = null;
        try {
            data = ByteUtil.getContent(new File(file));
        } catch (IOException ioe) {
            usage(ioe.getMessage());
        }
        
        String host = null;
        if (!cl.hasOption("a")) {
            usage("no smtp server specified");
        } else {
            host = cl.getOptionValue("a");
        }
       
        String sender = null;
        if (!cl.hasOption("s")) {
            usage("no sender specified");
        } else {
            sender = cl.getOptionValue("s");
        }
        
        String recipient = null;
        if (!cl.hasOption("r")) {
            usage("no recipient specified");
        } else {
            recipient = cl.getOptionValue("r");
        }

        boolean trace = false;
        if (cl.hasOption("T")) {
            trace = true;
        } 

        boolean tls = false;
        if (cl.hasOption("t")) {
            tls = true;
        } 

        boolean auth = false;
        String user = null;
        String password = null;
        if (cl.hasOption("A")) {
            auth = true;
            if (!cl.hasOption("u")) {
                usage("auth enabled, no user specified");
            } else {
                user = cl.getOptionValue("u");
            }
            if (!cl.hasOption("p")) {
                usage("auth enabled, no password specified");
            } else {
                password = cl.getOptionValue("p");
            }
        }
        
        if (cl.hasOption("v")) {
            mLog.info("SMTP server: " + host);
            mLog.info("Sender: " + sender);
            mLog.info("Recipient: " + recipient);
            mLog.info("File: " + file);
            mLog.info("TLS: " + tls);
            mLog.info("Auth: " + auth);
            if (auth) {
                mLog.info("User: " + user);
                char[] dummyPassword = new char[password.length()];
                Arrays.fill(dummyPassword, '*');
                mLog.info("Password: " + new String(dummyPassword)); 
            }
        }
        
        Properties props = System.getProperties();

        props.put("mail.smtp.host", host);
        
        if (auth) {
            props.put("mail.smtp.auth", "true");
        } else {
            props.put("mail.smtp.auth", "false");
        }
        
        if (tls) {
            props.put("mail.smtp.starttls.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "false");
        }
        
        // Disable certificate checking so we can test against
        // self-signed certificates
        java.security.Security.setProperty("ssl.SocketFactory.provider", SmtpInject.DummySSLSocketFactory.class.getName());

        Session session = Session.getInstance(props, null);
        session.setDebug(trace);

        try {
            // create a message
            MimeMessage msg = new MimeMessage(session, new FileInputStream(file));
            InternetAddress[] address = { new InternetAddress(recipient) };
            msg.setFrom(new InternetAddress(sender));
            
            // attach the file to the message
            Transport transport = session.getTransport("smtp");
            transport.connect(null, user, password);
            transport.sendMessage(msg, address);
            
        } catch (MessagingException mex) {
            mex.printStackTrace();
            Exception ex = null;
            if ((ex = mex.getNextException()) != null) {
                ex.printStackTrace();
            }
            System.exit(1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }
    }

    public static class DummySSLSocketFactory extends SSLSocketFactory {
        private SSLSocketFactory factory;
        
        public DummySSLSocketFactory() {
            try {
                SSLContext sslcontext = SSLContext.getInstance("TLS");
                sslcontext.init(null,
                                new TrustManager[] { new SmtpInject.DummyTrustManager()},
                                null);
                factory = (SSLSocketFactory)sslcontext.getSocketFactory();
            } catch(Exception ex) {
                // ignore
            }
        }
        
        public static SocketFactory getDefault() {
            return new DummySSLSocketFactory();
        }
        
        public Socket createSocket(Socket socket, String s, int i, boolean flag) throws IOException {
            return factory.createSocket(socket, s, i, flag);
        }
        
        public Socket createSocket(InetAddress inaddr, int i,
                                   InetAddress inaddr1, int j) throws IOException {
            return factory.createSocket(inaddr, i, inaddr1, j);
        }
        
        public Socket createSocket(InetAddress inaddr, int i) throws IOException {
            return factory.createSocket(inaddr, i);
        }

        public Socket createSocket(String s, int i, InetAddress inaddr, int j) throws IOException {
            return factory.createSocket(s, i, inaddr, j);
        }

        public Socket createSocket(String s, int i) throws IOException {
            return factory.createSocket(s, i);
        }

        public String[] getDefaultCipherSuites() {
            return factory.getDefaultCipherSuites();
        }

        public String[] getSupportedCipherSuites() {
            return factory.getSupportedCipherSuites();
        }
    }

    /**
     * DummyTrustManager - NOT SECURE
     */
    public static class DummyTrustManager implements X509TrustManager {
        
        public void checkClientTrusted(X509Certificate[] cert, String authType) {
            // everything is trusted
        }
        
        public void checkServerTrusted(X509Certificate[] cert, String authType) {
            // everything is trusted
        }
        
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
