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
package com.zimbra.cs.service.authenticator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extension;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.service.authenticator.ClientCertPrincipalMap.CertField;
import com.zimbra.cs.service.authenticator.ClientCertPrincipalMap.KnownCertField;
import com.zimbra.cs.service.authenticator.ClientCertPrincipalMap.SubjectCertField;

public class CertUtil {
    static final String LOG_PREFIX = ClientCertAuthenticator.LOG_PREFIX;
    
    static final String ATTR_EMAILADDRESS = "EMAILADDRESS";
    
    /* ObjectID for UPN in SubjectaltName for windows smart card logon */
    private static final String OID_UPN          = "1.3.6.1.4.1.311.20.2.3";
    
    /*
     * RFC 2253 2.3
     * 
     * If the AttributeType is in a published table of attribute types
     * associated with LDAP [4] (RFC 2252), then the type name string from that table
     * is used, otherwise it is encoded as the dotted-decimal encoding of
     * the AttributeType's OBJECT IDENTIFIER.
     * 
     * 
     */
    private static final Map<String, String> KNOWN_NON_RFC2252_ATTRS = new HashMap<String, String>();
    
    static {
        KNOWN_NON_RFC2252_ATTRS.put(ATTR_EMAILADDRESS, "1.2.840.113549.1.9.1");
    }

    X509Certificate cert;
    
    private CertUtil() {
    }
    
    CertUtil(X509Certificate cert) {
        this.cert = cert;
    }
    
    String getCertField(CertField certField) {
        if (certField instanceof KnownCertField) {
            return getKnownCertField((KnownCertField) certField);
        } else if (certField instanceof SubjectCertField) {
            return getSubjectCertField((SubjectCertField) certField);
        }
        return null;
    }
    
    private String getKnownCertField(KnownCertField certField) {
        String value = null;
        
        switch (certField.getField()) {
            case SUBJECT_DN:
                value = getSubjectDN();
                break;
            case SUBJECTALTNAME_OTHERNAME_UPN:
                value = getSubjectAltNameOtherNameUPN();
                break;
            case SUBJECTALTNAME_RFC822NAME:
                value = getSubjectAltNameRfc822Name();
                break;
        }
        return value;
    }
    
    private String getSubjectCertField(SubjectCertField certField) {
        String rdnAttrType = certField.getRDNAttrType();
        return getSubjectAttr(rdnAttrType, KNOWN_NON_RFC2252_ATTRS.get(rdnAttrType));
    }

    String getSubjectDN() {
        X500Principal subjectPrincipal = cert.getSubjectX500Principal();
        
        /*
        String CANONICAL = subjectPrincipal.getName(X500Principal.CANONICAL); 
        // 1.2.840.113549.1.9.1=#161075736572314070686f6562652e6d6270,cn=user one,ou=engineering,o=example company,l=saratoga,st=california,c=us
        
        String RFC1779   = subjectPrincipal.getName(X500Principal.RFC1779);
        // OID.1.2.840.113549.1.9.1=user1@phoebe.mbp, CN=user one, OU=Engineering, O=Example Company, L=Saratoga, ST=California, C=US
        
        String RFC2253   = subjectPrincipal.getName(X500Principal.RFC2253);
        // 1.2.840.113549.1.9.1=#161075736572314070686f6562652e6d6270,CN=user one,OU=Engineering,O=Example Company,L=Saratoga,ST=California,C=US
        */
        
        return subjectPrincipal.getName();
    }
    
    String getSubjectAltNameOtherNameUPN() {
        Collection<List<?>> generalNames = null;
        try {
            generalNames = cert.getSubjectAlternativeNames();
        } catch (CertificateParsingException e) {
            ZimbraLog.account.warn(LOG_PREFIX + "unable to get subject alternative names", e);
        }
    
        if (generalNames == null) {
            return null;
        }
        
        try {
            // Check that the certificate includes the SubjectAltName extension
            for (List<?> generalName : generalNames) {
                Integer tag = (Integer) generalName.get(0);
                if (GeneralName.otherName == tag.intValue()) {
                    // Value is encoded using ASN.1
                    ASN1InputStream decoder = new ASN1InputStream((byte[]) generalName.toArray()[1]);
                    DEREncodable encoded = decoder.readObject();
                    DERSequence derSeq = (DERSequence) encoded;
                    
                    DERObjectIdentifier typeId = DERObjectIdentifier.getInstance(derSeq.getObjectAt(0));
                    String oid = typeId.getId();
                    
                    String value = null;
                    ASN1TaggedObject otherNameValue = ASN1TaggedObject.getInstance(derSeq.getObjectAt(1));
                    if (OID_UPN.equals(oid)) {
                        ASN1TaggedObject upnValue = ASN1TaggedObject.getInstance(otherNameValue.getObject());
                        DERUTF8String str = DERUTF8String.getInstance(upnValue.getObject());
                        value= str.getString(); 
                        return value;
                    }
                }
            }
        } catch (IOException e) {
            ZimbraLog.account.warn(LOG_PREFIX + "unable to process ASN.1 data", e);
        }
        
        return null;
    }
    
    String getSubjectAltNameRfc822Name() {
        Collection<List<?>> generalNames = null;
        try {
            generalNames = cert.getSubjectAlternativeNames();
        } catch (CertificateParsingException e) {
            ZimbraLog.account.warn(LOG_PREFIX + "unable to get subject alternative names", e);
        }
    
        if (generalNames == null) {
            return null;
        }
        
        for (List<?> generalName : generalNames) {
            Integer tag = (Integer) generalName.get(0);
            if (GeneralName.rfc822Name == tag.intValue()) {
                String value = (String) generalName.get(1);
                return value;
            }
        }
        
        return null;
    }
    
    private String getSubjectAttr(String needAttrName, String needAttrOid) {
        String subjectDN = getSubjectDN();
        
        try {
            LdapName dn = new LdapName(subjectDN);
            List<Rdn> rdns = dn.getRdns();
            
            for (Rdn rdn : rdns) {
                String type = rdn.getType();
                
                boolean isOid = type.contains(".");
                
                boolean matched = (isOid ? type.equals(needAttrOid) : type.equals(needAttrName));
                
                if (matched) {
                    Object value = rdn.getValue();
                    if (value == null) {
                        continue;
                    }
                    
                    if (isOid) {
                        byte[] bytes = (byte[]) value;
                        try {
                            ASN1InputStream decoder = new ASN1InputStream(bytes);
                            DEREncodable encoded = decoder.readObject();
                            DERIA5String str = DERIA5String.getInstance(encoded);
                            return str.getString();
                        } catch (IOException e) {
                            ZimbraLog.account.warn(LOG_PREFIX + "unable to decode " + type, e);
                        }
                        
                    } else {
                        return value.toString();
                    }
                }
            }
        } catch (InvalidNameException e) {
            ZimbraLog.account.warn(LOG_PREFIX + "Invalid subject dn value" + subjectDN, e);
        }
        
        return null;
    }
    

    
    /* 
     * ======================================================
     * Printing methods below for CLI - Not production code
     * ======================================================
     */
    
    private void loadCert(String certFilePath) throws Exception {
        InputStream inStream = new FileInputStream(certFilePath);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        cert = (X509Certificate)cf.generateCertificate(inStream);
        inStream.close();
    }
    
    private void dumpCert(String outputFlePath) throws Exception {
        outputCert(outputFlePath, true);
    }
    
    private void printCert(String outputFlePath) throws Exception {
        outputCert(outputFlePath, false);
    }
    
    private void outputCert(String outputFlePath, boolean dump) throws Exception {
        PrintStream outStream;
        
        if (outputFlePath != null) {
            outStream = new PrintStream(outputFlePath);
        } else {
            outStream = System.out;
        }
        
        try {
            if (dump) {
                outStream.println(cert.toString());
            } else {
                printCert(outStream);
            }
        } finally {
            outStream.flush();
            if (outputFlePath != null) {
                outStream.close();
            }
        }
    }
    
    private void printCert(PrintStream outStream) throws Exception {
        printVersion(outStream);
        printSerialNumber(outStream);
        printSigAlg(outStream);
        printIssuer(outStream);
        printValidity(outStream);
        printSubject(outStream);
        
        printX509V3Extension(outStream);
    }
    
    private void printX509V3Extension(PrintStream outStream) throws Exception {
        outStream.println();
        outStream.format("X509v3 extensions:\n");
        
        printSubjectAlternativeNames(outStream);
        outStream.println();
        
        printCRLDistributionPoints(outStream);
        outStream.println();
    }
    
    private void printVersion(PrintStream outStream) {
        int version = cert.getVersion();
        
        outStream.format("Version: %d (0x%x)\n", version, version);
    }
    
    private void printSerialNumber(PrintStream outStream) {
        BigInteger serialNumber = cert.getSerialNumber();
        
        outStream.format("Serial Number: %s (0x%x)\n", serialNumber.toString(), serialNumber);
    }
    
    private void printSigAlg(PrintStream outStream) throws Exception {
        String sigAlgName = cert.getSigAlgName();
        String sigAlgOID = cert.getSigAlgOID();
        
        outStream.format("Signature Algorithm: %s (%s)\n", sigAlgName, sigAlgOID);
        
        /*
        byte[] sigAlgParams = cert.getSigAlgParams();
        
        AlgorithmParameters algParams = AlgorithmParameters.getInstance(sigAlgName);
        algParams.init(sigAlgParams);
        outStream.format("Signature Algorithm Params: %s", algParams.toString());
        */
    }
    
    private void printIssuer(PrintStream outStream) {
        X500Principal issuerPrincipal = cert.getIssuerX500Principal();
        outStream.format("Issuer: %s\n", issuerPrincipal.getName());
    }
    
    private void printValidity(PrintStream outStream) {
        Date notBefore = cert.getNotBefore();
        Date notAfter = cert.getNotAfter();

        outStream.format("Validity\n");
        outStream.format("    Not Before: %s\n", notBefore.toGMTString());
        outStream.format("    Not After : %s\n", notAfter.toGMTString());
    }
    
    private void printSubject(PrintStream outStream) {
        X500Principal subjectPrincipal = cert.getSubjectX500Principal();
        outStream.format("Subject: %s\n", subjectPrincipal.getName());
    }
    
    private void printSubjectAlternativeNames(PrintStream outStream) throws Exception {
        
        final String UPN_DISPLAY = "Principal Name";
        final String RFC822NAME_DISPLAY = "RFC822 Name";
        final String DNSNAME_DISPLAY = "DNS Name";
        
        outStream.format("X509v3 Subject Alternative Name: \n");
        
        try {
            Collection<List<?>> generalNames = cert.getSubjectAlternativeNames();
            // Check that the certificate includes the SubjectAltName extension
            if (generalNames == null) {
                return;
            }

            /*
               OtherName ::= SEQUENCE {
                  type-id    OBJECT IDENTIFIER,
                  value      [0] EXPLICIT ANY DEFINED BY type-id }
             */
            
            for (List<?> generalName : generalNames) {
                Integer tag = (Integer) generalName.get(0);
                if (GeneralName.otherName == tag.intValue()) {
                    // Value is encoded using ASN.1
                    ASN1InputStream decoder = new ASN1InputStream((byte[]) generalName.toArray()[1]);
                    DEREncodable encoded = decoder.readObject();
                    DERSequence derSeq = (DERSequence) encoded;
                    
                    DERObjectIdentifier typeId = DERObjectIdentifier.getInstance(derSeq.getObjectAt(0));
                    String oid = typeId.getId();
                    
                    String value = null;
                    ASN1TaggedObject otherNameValue = ASN1TaggedObject.getInstance(derSeq.getObjectAt(1));
                    if (OID_UPN.equals(oid)) {
                        ASN1TaggedObject upnValue = ASN1TaggedObject.getInstance(otherNameValue.getObject());
                        DERUTF8String str = DERUTF8String.getInstance(upnValue.getObject());
                        value= str.getString(); 
                    }
                    
                    outStream.format("    [%d] %s(%s) = %s\n", tag, oid, UPN_DISPLAY, value);
                } else if (GeneralName.rfc822Name == tag.intValue()) {
                    String value = (String) generalName.get(1);
                    outStream.format("    [%d] %s = %s\n", tag, RFC822NAME_DISPLAY, value);
                } else if (GeneralName.dNSName == tag.intValue()) {
                    String value = (String) generalName.get(1);
                    outStream.format("    [%d] %s = %s\n", tag, DNSNAME_DISPLAY, value);
                } else {
                    outStream.format("    [%d] - not yet supported\n", tag);
                }
                
            }
        }
        catch (CertificateParsingException e) {
            e.printStackTrace();
        }
    }
    
    private void printCRLDistributionPoints(PrintStream outStream) throws Exception {
        
        outStream.format("X509v3 CRL Distribution Points: \n");
            
        String extOid = X509Extension.cRLDistributionPoints.getId(); // 2.5.29.31
        byte[] extVal = cert.getExtensionValue(extOid);
        if (extVal == null) {
            return;
        }
        
        /* http://download.oracle.com/javase/6/docs/api/java/security/cert/X509Extension.html#getExtensionValue(java.lang.String)
         * 
           The ASN.1 definition for this is:

             Extensions  ::=  SEQUENCE SIZE (1..MAX) OF Extension
            
             Extension  ::=  SEQUENCE  {
                 extnId        OBJECT IDENTIFIER,
                 critical      BOOLEAN DEFAULT FALSE,
                 extnValue     OCTET STRING
                               -- contains a DER encoding of a value
                               -- of the type registered for use with
                               -- the extnId object identifier value
             }
         */
        
        byte[] extnValue = DEROctetString.getInstance(ASN1Object.fromByteArray(extVal)).getOctets();
        
        CRLDistPoint crlDistPoint = CRLDistPoint.getInstance(ASN1Object.fromByteArray(extnValue));
        DistributionPoint[] distPoints = crlDistPoint.getDistributionPoints();
        
        for (DistributionPoint distPoint : distPoints) {
            DistributionPointName distPointName = distPoint.getDistributionPoint();
            int type = distPointName.getType();
            
            if (DistributionPointName.FULL_NAME == type) {
                outStream.format("Full Name: \n");
                GeneralNames generalNames = GeneralNames.getInstance(distPointName.getName()); 
                GeneralName[] names = generalNames.getNames();
                for (GeneralName generalname : names) {
                    int tag = generalname.getTagNo();
                    if (GeneralName.uniformResourceIdentifier == tag) {
                        DEREncodable name = generalname.getName();
                        DERIA5String str = DERIA5String.getInstance(name);
                        String value = str.getString();
                        outStream.format("    %s\n", value);
                    } else {
                        outStream.format("tag %d not yet implemented", tag);
                    }
                }
            } else {
                outStream.format("type %d not yet implemented", type);
            }
        }
    }
    
    private static int EXIT_CODE_GOOD = 0;
    private static int EXIT_CODE_BAD = 0;
    
    private static String O_CERT = "c";
    private static String O_DUMP = "d";
    private static String O_GET  = "g";
    private static String O_HELP = "h";
    private static String O_PRINT = "p";
    
    private static void usage(Options options, String msg) {
        System.out.println("\n");
        System.out.println(msg);
        usage(options);
    }
    
    private static void usage(Options options) {
        System.out.println("\n");
        PrintWriter pw = new PrintWriter(System.out, true);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, formatter.getWidth(), 
                "zmjava " + CertUtil.class.getCanonicalName() + " [options]", 
                null, options, formatter.getLeftPadding(), formatter.getDescPadding(), null);
        System.out.println("\n");
        pw.flush();
    }
    
    /*
     *  zmjava com.zimbra.cs.service.authenticator.CertUtil [options]
     */
    public static void main(String[] args) {
        
        Options options = new Options();
        options.addOption(O_CERT, true, "file path of the certificate");
        options.addOption(O_DUMP, false, "dump the certificate (print toString() value of the certificate)");
        options.addOption(O_GET, true, "get a field in the certificate, valid fields:" + 
                KnownCertField.names() + "|" + SubjectCertField.names());
        options.addOption(O_HELP, false, "print usage");
        options.addOption(O_PRINT, false, "print the certificate(print each parsed certificate fields)");
        
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
            System.exit(EXIT_CODE_BAD);
        }
        
        if (cl.hasOption(O_HELP)) {
            usage(options);
            System.exit(EXIT_CODE_GOOD);
        }
        
        String certFilePath = null;
        
        if (cl.hasOption(O_CERT)) {
            certFilePath = cl.getOptionValue(O_CERT);
        } else {
            usage(options, "missing cert path");
            System.exit(EXIT_CODE_BAD);
        }
        
        try {
            CertUtil certUtil = new CertUtil();
            certUtil.loadCert(certFilePath);
            
            if (cl.hasOption(O_DUMP)) {
                certUtil.dumpCert((String)null);
            } else if (cl.hasOption(O_PRINT)) {
                certUtil.printCert((String)null);
            } else if (cl.hasOption(O_GET)) {
                String field = cl.getOptionValue(O_GET);
                CertField certField = ClientCertPrincipalMap.parseCertField(field);
                String value = certUtil.getCertField(certField);
                System.out.println(field + ": " + value);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(EXIT_CODE_BAD);
        }
    }
}
