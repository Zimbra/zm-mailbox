/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.util.SoapCLI;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;

public class VolumeUtil extends SoapCLI {

    protected static final String O_A = "a";
    protected static final String O_D = "d";
    protected static final String O_L = "l";
    protected static final String O_E = "e";
    protected static final String O_DC = "dc";
    protected static final String O_SC = "sc";
    protected static final String O_TS = "ts";
    protected static final String O_ID = "id";
    protected static final String O_T = "t";
    protected static final String O_N = "n";
    protected static final String O_P = "p";
//    protected static final String O_FB = "fb";
//    protected static final String O_FGB = "fgb";
//    protected static final String O_MB = "mb";
//    protected static final String O_MGB = "mgb";
    protected static final String O_C = "c";
    protected static final String O_CT = "ct";
    
    protected VolumeUtil() throws ServiceException {
        super();
        setupCommandLineOptions();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Zimbra.toolSetup();
        VolumeUtil util = null;
        try {
            util = new VolumeUtil();
            CommandLine cl = util.getCommandLine(args);
            if (cl == null)
                return;
            String id = cl.getOptionValue(O_ID);
            String type = cl.getOptionValue(O_T);
            String name = cl.getOptionValue(O_N);
            String path = cl.getOptionValue(O_P);
//            String fileBits = cl.getOptionValue(O_FB);
//            String fileGroupBits = cl.getOptionValue(O_FGB);
//            String mailboxBits = cl.getOptionValue(O_MB);
//            String mailboxGroupBits = cl.getOptionValue(O_MGB);
            String compress = cl.getOptionValue(O_C);
            String compressThreshold = cl.getOptionValue(O_CT);
            
            if (cl.hasOption(O_A)) {
                if (id != null)
                    throw new IllegalArgumentException("id cannot be specified when adding a volume");
//                util.addVolume(name, type, path, fileBits, fileGroupBits, mailboxBits, mailboxGroupBits, compress, compressThreshold);
                util.addVolume(name, type, path, null, null, null, null, compress, compressThreshold);
            } else if (cl.hasOption(O_D)) {
                if (id == null)
                    throw new ParseException("volume id is missing");
                util.deleteVolume(id);
            } else if (cl.hasOption(O_L)) {
                util.listVolumes(id, true);
            } else if (cl.hasOption(O_E)) {
                if (id == null)
                    throw new ParseException("volume id is missing");
//                util.editVolume(id, name, type, path, fileBits, fileGroupBits, mailboxBits, mailboxGroupBits, compress, compressThreshold);
                util.editVolume(id, name, type, path, null, null, null, null, compress, compressThreshold);
            } else if (cl.hasOption(O_DC)) {
                util.displayCurrentVolumes();
            } else if (cl.hasOption(O_SC)) {
                if (id == null)
                    throw new ParseException("volume id is missing");
                short shortId = Short.parseShort(id);
                if (shortId < 0)
                    throw new IllegalArgumentException("id cannot be less than 0");
                util.setCurrentVolume(shortId);
            } else if (cl.hasOption(O_TS)) {
            	util.unsetCurrentSecondaryMessageVolume();
            } else {
                throw new ParseException("No action (-a,-d,-l,-e,-dc,-sc,-ts) is specified");
            }
            System.exit(0);
        } catch (ParseException e) {
            util.usage(e);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
        }
        System.exit(1);
    }

    private void setCurrentVolume(short id) throws SoapFaultException, IOException, ServiceException {
    	Integer idInt = new Integer(id);
    	Map vols = getVolumes(idInt.toString(), true);
    	Map vol = (Map) vols.get(idInt);
    	if (vol == null) {
    		System.err.println("Volume " + id + " does not exist");
    		System.exit(1);
    	}
    	short type = (short) ((Integer) vol.get(AdminService.A_VOLUME_TYPE)).intValue();
    	Element req = new Element.XMLElement(AdminService.SET_CURRENT_VOLUME_REQUEST);
        req.addAttribute(AdminService.A_VOLUME_TYPE, type);
        req.addAttribute(AdminService.A_ID, id);
        auth();
        getTransport().invokeWithoutSession(req);
        System.out.println("Volume " + id + " is now the current " +
        		           VolumeUtil.getTypeName(type) + " volume.");
    }

    private void unsetCurrentSecondaryMessageVolume()
    throws SoapFaultException, IOException, ServiceException {
        Element req = new Element.XMLElement(AdminService.SET_CURRENT_VOLUME_REQUEST);
        req.addAttribute(AdminService.A_VOLUME_TYPE, Volume.TYPE_MESSAGE_SECONDARY);
        req.addAttribute(AdminService.A_ID, Volume.ID_NONE);
        auth();
        getTransport().invokeWithoutSession(req);
        System.out.println("Turned off the current secondary message volume.");
    }

    private void displayCurrentVolumes()
    throws SoapFaultException, IOException, ServiceException {
    	Map vols = getVolumes(null, true);
        Element req = new Element.XMLElement(AdminService.GET_CURRENT_VOLUMES_REQUEST);
        Element resp = getTransport().invokeWithoutSession(req);
        for (Iterator it = resp.elementIterator(AdminService.E_VOLUME); it.hasNext(); ) {
            Element volElem = (Element) it.next();
            Integer key = new Integer(volElem.getAttribute(AdminService.A_ID));
            Map vol = (Map) vols.get(key);
            listVolume(vol);
        }
    }

    private Map getVolumes(String id, boolean auth)
    throws SoapFaultException, IOException, ServiceException {
    	Map vols = new LinkedHashMap();
        if (auth)
            auth();
        Element req = null;
        if (id == null) {
            req = new Element.XMLElement(AdminService.GET_ALL_VOLUMES_REQUEST);
        } else {
            req = new Element.XMLElement(AdminService.GET_VOLUME_REQUEST);
            req.addAttribute(AdminService.A_ID, id);
        }
        Element resp = getTransport().invokeWithoutSession(req);
        for (Iterator it = resp.elementIterator(AdminService.E_VOLUME); it.hasNext(); ) {
            Element volElem = (Element) it.next();
            String vid = volElem.getAttribute(AdminService.A_ID);
            String name = volElem.getAttribute(AdminService.A_VOLUME_NAME);
            short type = (short) volElem.getAttributeLong(AdminService.A_VOLUME_TYPE);
            String path = volElem.getAttribute(AdminService.A_VOLUME_ROOTPATH);
//            String fbits = volElem.getAttribute(AdminService.A_VOLUME_FBITS);
//            String fgbits = volElem.getAttribute(AdminService.A_VOLUME_FGBITS);
//            String mbits = volElem.getAttribute(AdminService.A_VOLUME_MBITS);
//            String mgbits = volElem.getAttribute(AdminService.A_VOLUME_MGBITS);
            boolean compressed = volElem.getAttributeBool(AdminService.A_VOLUME_COMPRESS_BLOBS);
            String threshold = volElem.getAttribute(AdminService.A_VOLUME_COMPRESSION_THRESHOLD);

            Map vol = new HashMap();
            Integer key = new Integer(vid);
            vol.put(AdminService.A_ID, key);
            vol.put(AdminService.A_VOLUME_NAME, name);
            vol.put(AdminService.A_VOLUME_TYPE, new Integer(type));
            vol.put(AdminService.A_VOLUME_ROOTPATH, path);
//            vol.put(AdminService.A_VOLUME_FBITS, new Integer(fbits));
//            vol.put(AdminService.A_VOLUME_FGBITS, new Integer(fgbits));
//            vol.put(AdminService.A_VOLUME_MBITS, new Integer(mbits));
//            vol.put(AdminService.A_VOLUME_MGBITS, new Integer(mgbits));
            vol.put(AdminService.A_VOLUME_COMPRESS_BLOBS, new Boolean(compressed));
            vol.put(AdminService.A_VOLUME_COMPRESSION_THRESHOLD, new Integer(threshold));
            vols.put(key, vol);
        }

        return vols;
    }

    private void listVolume(Map vol) {
		short vid = (short) ((Integer) vol.get(AdminService.A_ID)).intValue();
        short type = (short) ((Integer) vol.get(AdminService.A_VOLUME_TYPE)).intValue();
        boolean compressed = ((Boolean) vol.get(AdminService.A_VOLUME_COMPRESS_BLOBS)).booleanValue();
        int threshold = ((Integer) vol.get(AdminService.A_VOLUME_COMPRESSION_THRESHOLD)).intValue();
//        int fbits = ((Integer) vol.get(AdminService.A_VOLUME_FBITS)).intValue();
//        int fgbits = ((Integer) vol.get(AdminService.A_VOLUME_FGBITS)).intValue();
//        int mbits = ((Integer) vol.get(AdminService.A_VOLUME_MBITS)).intValue();
//        int mgbits = ((Integer) vol.get(AdminService.A_VOLUME_MGBITS)).intValue();

        System.out.println("   Volume id: " + vid);
        System.out.println("        name: " + vol.get(AdminService.A_VOLUME_NAME));
        System.out.println("        type: " + VolumeUtil.getTypeName(type));
        System.out.println("        path: " + vol.get(AdminService.A_VOLUME_ROOTPATH));
        System.out.print("  compressed: " + compressed);
        if (compressed)
            System.out.println("\t         threshold: " + threshold + " bytes");
        else
            System.out.println();
//        System.out.println("   file bits: " + fbits +      "\t   file group bits: " + fgbits);
//        System.out.println("mailbox bits: " + mbits +      "\tmailbox group bits: " + mgbits);
        System.out.println();
    }

    private void listVolumes(String id, boolean auth)
    throws SoapFaultException, IOException, ServiceException {
    	Map vols = getVolumes(id, auth);
    	for (Iterator iter = vols.keySet().iterator(); iter.hasNext(); ) {
    		Integer key = (Integer) iter.next();
    		Map vol = (Map) vols.get(key);
    		listVolume(vol);
    	}
    }

    private void deleteVolume(String id) throws SoapFaultException, IOException, ServiceException {
        Element req = new Element.XMLElement(AdminService.DELETE_VOLUME_REQUEST);
        req.addAttribute(AdminService.A_ID, id);
        auth();
        getTransport().invokeWithoutSession(req);
        System.out.println("Deleted volume " + id);
    }

    private void editVolume(String id, String name, String type, String path,
            String fileBits, String fileGroupBits, String mailboxBits,
            String mailboxGroupBits, String compress, String compressThreshold)
            throws SoapFaultException, IOException, ServiceException {
        
        Element req = new Element.XMLElement(AdminService.MODIFY_VOLUME_REQUEST);
        req.addAttribute(AdminService.A_ID, id);
        Element vol = req.addElement(AdminService.E_VOLUME);
        addAttributes(vol, name, type, path, fileBits, fileGroupBits,
                mailboxBits, mailboxGroupBits, compress, compressThreshold);
        auth();
        getTransport().invokeWithoutSession(req);
        System.out.println("Edited volume " + id);
    }

    private void addVolume(String name, String type, String path,
            String fileBits, String fileGroupBits, String mailboxBits,
            String mailboxGroupBits, String compress, String compressThreshold)
            throws SoapFaultException, IOException, ServiceException {

        if (name == null || type == null || path == null)
            throw new IllegalArgumentException("at least one of the required parameters (name, type, path) is missing");
        Element req = new Element.XMLElement(AdminService.CREATE_VOLUME_REQUEST);
        Element vol = req.addElement(AdminService.E_VOLUME);
//        if (fileBits == null)
//            fileBits = "12";
//        if (fileGroupBits == null)
//            fileGroupBits = "8";
//        if (mailboxBits == null)
//            mailboxBits = "12";
//        if (mailboxGroupBits == null)
//            mailboxGroupBits = "8";
        if (compress == null)
            compress = "false";
        if (compressThreshold == null)
            compressThreshold = "4096";
        addAttributes(vol, name, type, path, fileBits, fileGroupBits,
                mailboxBits, mailboxGroupBits, compress, compressThreshold);
        auth();
        Element resp = getTransport().invokeWithoutSession(req);
        vol = resp.getElement(AdminService.E_VOLUME);
        String id = vol.getAttribute(AdminService.A_ID);
        System.out.println("Volume " + id + " is created");
    }

    private void addAttributes(Element vol, String name, String type,
            String path, String fileBits, String fileGroupBits,
            String mailboxBits, String mailboxGroupBits, String compress,
            String compressThreshold) {
        
//        // validate numeric bits parameters if they are present
//        try {
//            int n;
//            if (fileBits != null) {
//                n = Integer.parseInt(fileBits);
//                if (n < 0)
//                    throw new IllegalArgumentException("bits parameter cannot be negative");
//            }
//            if (fileGroupBits != null) {
//                n = Integer.parseInt(fileGroupBits);
//                if (n < 0)
//                    throw new IllegalArgumentException(
//                            "bits parameter cannot be negative");
//            }
//            if (mailboxBits != null) {
//                n = Integer.parseInt(mailboxBits);
//                if (n < 0)
//                    throw new IllegalArgumentException(
//                            "bits parameter cannot be negative");
//            }
//            if (mailboxGroupBits != null) {
//                n = Integer.parseInt(mailboxGroupBits);
//                if (n < 0)
//                    throw new IllegalArgumentException(
//                            "bits parameter cannot be negative");
//            }
//        } catch (NumberFormatException e) {
//            throw new IllegalArgumentException("at least one value of the bits parameters is not a valid number");
//        }

        // validate compress parameter
        if (compress != null) {
            if (!"true".equals(compress) && !"false".equals(compress))
                throw new IllegalArgumentException("expecting true or false for compress option");
        }
        
        // the parameters may be null in the case of modifications
        if (type != null) {
            short t = VolumeUtil.getTypeId(type);
            if (t < 0)
                throw new IllegalArgumentException("invalid volume type: " + type);
            vol.addAttribute(AdminService.A_VOLUME_TYPE, t);
        }
        if (name != null)
            vol.addAttribute(AdminService.A_VOLUME_NAME, name);
        if (path != null)
            vol.addAttribute(AdminService.A_VOLUME_ROOTPATH, path);
//        if (fileBits != null)
//            vol.addAttribute(AdminService.A_VOLUME_FBITS, fileBits);
//        if (fileGroupBits != null)
//            vol.addAttribute(AdminService.A_VOLUME_FGBITS, fileGroupBits);
//        if (mailboxBits != null)
//            vol.addAttribute(AdminService.A_VOLUME_MBITS, mailboxBits);
//        if (mailboxGroupBits != null)
//            vol.addAttribute(AdminService.A_VOLUME_MGBITS, mailboxGroupBits);
        if (compress != null)
            vol.addAttribute(AdminService.A_VOLUME_COMPRESS_BLOBS, "true".equals(compress));
        if (compressThreshold != null)
            vol.addAttribute(AdminService.A_VOLUME_COMPRESSION_THRESHOLD,
                    compressThreshold);
    }
    
    protected String getCommandUsage() {
        return "zmvolume {-a | -d | -l | -e | -dc | -sc } <options>";
    }

    protected void setupCommandLineOptions() {
        super.setupCommandLineOptions();
        Options options = getOptions();
        OptionGroup og = new OptionGroup();
        og.addOption(new Option(O_A, "add", false, "Adds a volume."));
        og.addOption(new Option(O_D, "delete", false, "Deletes a volume."));
        og.addOption(new Option(O_L, "list", false, "Lists volumes."));
        og.addOption(new Option(O_E, "edit", false, "Edits a volume."));
        og.addOption(new Option(O_DC, "displayCurrent", false, "Displays the current volumes."));
        og.addOption(new Option(O_SC, "setCurrent", false, "Sets the current volume."));
        og.addOption(new Option(O_TS, "turnOffSecondary", false, "Turns off the current secondary message volume"));
        options.addOptionGroup(og);
        options.addOption(O_ID, "id", true, "Volume ID");
        options.addOption(O_T, "type", true,
            "Volume type (primaryMessage, secondaryMessage, or index)");
        options.addOption(O_N, "name", true, "volume name");
        options.addOption(O_P, "path", true, "Root path");
//        options.addOption(O_FB, "fileBits", true, "File bits; default is 12");
//        options.addOption(O_FGB, "fileGroupBits", true, "File group bits; default is 8");
//        options.addOption(O_MB, "mailboxBits", true, "Mailbox bits; default is 12");
//        options.addOption(O_MGB, "mailboxGroupBits", true, "Mailbox group bits; default is 8");
        options.addOption(O_C, "compress", true, "Compress blobs; \"true\" or \"false\"");
        options.addOption(O_CT, "compressionThreshold", true, "Compression threshold; default 4KB");
    }
    
    protected void usage(ParseException e) {
        if (e != null) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
        }

        System.out.println(getCommandUsage());
        printOpt(O_A, 0);
        printOpt(O_N, 2);
        printOpt(O_T, 2);
        printOpt(O_P, 2);
//        printOpt(O_FB, 2);
//        printOpt(O_FGB, 2);
//        printOpt(O_MB, 2);
//        printOpt(O_MGB, 2);
        printOpt(O_C, 2);
        printOpt(O_CT, 2);
        printOpt(O_E, 0);
        printOpt(O_ID, 2);
        System.out.println("  any of the options listed under -a can also be specified " );
        System.out.println("  to have its value modified.");
        printOpt(O_D, 0);
        printOpt(O_ID, 2);
        printOpt(O_L, 0);
        printOpt(O_ID, 2);
        System.out.println("  -id is optional.");
        printOpt(O_DC, 0);
        printOpt(O_SC, 0);
        printOpt(O_ID, 2);
        printOpt(O_TS, 0);
    }
    
    private static final String PADDING = "                                                  ";
    
    private void printOpt(String optStr, int leftPad) {
        Options options = getOptions();
        Option opt = options.getOption(optStr);
        StringBuffer buf = new StringBuffer();
        buf.append(PADDING.substring(0, leftPad));
        buf.append("-" + opt.getOpt() + ",--" + opt.getLongOpt() + (opt.hasArg() ? " <arg>" : ""));
        
        buf.append(PADDING.substring(0, 35-buf.length()));
        buf.append(opt.getDescription());
        System.out.println(buf.toString());
    }

    /**
     * Returns the type id for the given name.  If the name is not recognized,
     * returns <code>-1</code>.
     */
    public static short getTypeId(String name) {
        if (name.equalsIgnoreCase("primaryMessage")) {
            return Volume.TYPE_MESSAGE;
        }
        if (name.equalsIgnoreCase("secondaryMessage")) {
            return Volume.TYPE_MESSAGE_SECONDARY;
        }
        if (name.equalsIgnoreCase("index")) {
            return Volume.TYPE_INDEX;
        }
        return -1;
    }

    /**
     * Returns the human-readable name for the given volume type.
     */
    public static String getTypeName(short type) {
        switch (type) {
            case Volume.TYPE_MESSAGE: return "primaryMessage";
            case Volume.TYPE_MESSAGE_SECONDARY: return "secondaryMessage";
            case Volume.TYPE_INDEX: return "index";
        }
        return "Unrecognized type " + type;
    }
}
