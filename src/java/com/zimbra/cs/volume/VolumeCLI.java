/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.volume;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.SoapCLI;

public final class VolumeCLI extends SoapCLI {

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
    protected static final String O_C = "c";
    protected static final String O_CT = "ct";

    protected VolumeCLI() throws ServiceException {
        super();
        setupCommandLineOptions();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zmvolume", BuildInfo.VERSION);
        VolumeCLI util = null;
        try {
            util = new VolumeCLI();
        } catch (ServiceException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        try {
            CommandLine cl = util.getCommandLine(args);
            if (cl == null)
                return;

            ZAuthToken zat = getZAuthToken(cl);

            String id = cl.getOptionValue(O_ID);
            String type = cl.getOptionValue(O_T);
            String name = cl.getOptionValue(O_N);
            String path = cl.getOptionValue(O_P);
            String compress = cl.getOptionValue(O_C);
            String compressThreshold = cl.getOptionValue(O_CT);

            if (cl.hasOption(O_A)) {
                if (id != null)
                    throw new ParseException("id cannot be specified when adding a volume");
                util.addVolume(zat, name, type, path, null, null, null, null, compress, compressThreshold);
            } else if (cl.hasOption(O_D)) {
                if (id == null)
                    throw new ParseException("volume id is missing");
                util.deleteVolume(zat, id);
            } else if (cl.hasOption(O_L)) {
                util.listVolumes(zat, id, true);
            } else if (cl.hasOption(O_E)) {
                if (id == null)
                    throw new ParseException("volume id is missing");
                util.editVolume(zat, id, name, type, path, null, null, null, null, compress, compressThreshold);
            } else if (cl.hasOption(O_DC)) {
                util.displayCurrentVolumes(zat);
            } else if (cl.hasOption(O_SC)) {
                if (id == null)
                    throw new ParseException("volume id is missing");
                short shortId = Short.parseShort(id);
                if (shortId < 0)
                    throw new ParseException("id cannot be less than 0");
                util.setCurrentVolume(zat, shortId);
            } else if (cl.hasOption(O_TS)) {
                util.unsetCurrentSecondaryMessageVolume(zat);
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

    private void setCurrentVolume(ZAuthToken zat, short id) throws SoapFaultException, IOException, ServiceException {
        Integer idInt = new Integer(id);
        Map<Integer, Map<String, Object>> vols = getVolumes(zat, idInt.toString(), true);
        Map<String, Object> vol = vols.get(idInt);
        if (vol == null) {
            System.err.println("Volume " + id + " does not exist");
            System.exit(1);
        }
        short type = (short) ((Integer) vol.get(AdminConstants.A_VOLUME_TYPE)).intValue();
        Element req = new Element.XMLElement(AdminConstants.SET_CURRENT_VOLUME_REQUEST);
        req.addAttribute(AdminConstants.A_VOLUME_TYPE, type);
        req.addAttribute(AdminConstants.A_ID, id);
        auth(zat);
        getTransport().invokeWithoutSession(req);
        System.out.println("Volume " + id + " is now the current " +
                           VolumeCLI.getTypeName(type) + " volume.");
    }

    private void unsetCurrentSecondaryMessageVolume(ZAuthToken zat)
    throws SoapFaultException, IOException, ServiceException {
        Element req = new Element.XMLElement(AdminConstants.SET_CURRENT_VOLUME_REQUEST);
        req.addAttribute(AdminConstants.A_VOLUME_TYPE, Volume.TYPE_MESSAGE_SECONDARY);
        req.addAttribute(AdminConstants.A_ID, Volume.ID_NONE);
        auth(zat);
        getTransport().invokeWithoutSession(req);
        System.out.println("Turned off the current secondary message volume.");
    }

    private void displayCurrentVolumes(ZAuthToken zat)
    throws SoapFaultException, IOException, ServiceException {
        Map<Integer, Map<String, Object>> vols = getVolumes(zat, null, true);
        Element req = new Element.XMLElement(AdminConstants.GET_CURRENT_VOLUMES_REQUEST);
        Element resp = getTransport().invokeWithoutSession(req);
        for (Iterator<Element> it = resp.elementIterator(AdminConstants.E_VOLUME); it.hasNext(); ) {
            Element volElem = it.next();
            Integer key = new Integer(volElem.getAttribute(AdminConstants.A_ID));
            Map<String, Object> vol = vols.get(key);
            listVolume(vol);
        }
    }

    private Map<Integer, Map<String, Object>> getVolumes(ZAuthToken zat, String id, boolean auth)
    throws SoapFaultException, IOException, ServiceException {
        // Use a TreeMap, so results are ordered by id.
        Map<Integer, Map<String, Object>> vols = new TreeMap<Integer, Map<String, Object>>();
        if (auth)
            auth(zat);
        Element req = null;
        if (id == null) {
            req = new Element.XMLElement(AdminConstants.GET_ALL_VOLUMES_REQUEST);
        } else {
            req = new Element.XMLElement(AdminConstants.GET_VOLUME_REQUEST);
            req.addAttribute(AdminConstants.A_ID, id);
        }
        Element resp = getTransport().invokeWithoutSession(req);
        for (Iterator<Element> it = resp.elementIterator(AdminConstants.E_VOLUME); it.hasNext(); ) {
            Element volElem = it.next();
            String vid = volElem.getAttribute(AdminConstants.A_ID);
            String name = volElem.getAttribute(AdminConstants.A_VOLUME_NAME);
            short type = (short) volElem.getAttributeLong(AdminConstants.A_VOLUME_TYPE);
            String path = volElem.getAttribute(AdminConstants.A_VOLUME_ROOTPATH);
            boolean compressed = volElem.getAttributeBool(AdminConstants.A_VOLUME_COMPRESS_BLOBS);
            boolean isCurrent = volElem.getAttributeBool(AdminConstants.A_VOLUME_IS_CURRENT);
            String threshold = volElem.getAttribute(AdminConstants.A_VOLUME_COMPRESSION_THRESHOLD);

            Map<String, Object> vol = new HashMap<String, Object>();
            Integer key = new Integer(vid);
            vol.put(AdminConstants.A_ID, key);
            vol.put(AdminConstants.A_VOLUME_NAME, name);
            vol.put(AdminConstants.A_VOLUME_TYPE, new Integer(type));
            vol.put(AdminConstants.A_VOLUME_ROOTPATH, path);
            vol.put(AdminConstants.A_VOLUME_COMPRESS_BLOBS, Boolean.valueOf(compressed));
            vol.put(AdminConstants.A_VOLUME_COMPRESSION_THRESHOLD, new Integer(threshold));
            vol.put(AdminConstants.A_VOLUME_IS_CURRENT, Boolean.valueOf(isCurrent));
            vols.put(key, vol);
        }

        return vols;
    }

    private void listVolume(Map<String, Object> vol) {
        short vid = (short) ((Integer) vol.get(AdminConstants.A_ID)).intValue();
        short type = (short) ((Integer) vol.get(AdminConstants.A_VOLUME_TYPE)).intValue();
        boolean compressed = ((Boolean) vol.get(AdminConstants.A_VOLUME_COMPRESS_BLOBS)).booleanValue();
        int threshold = ((Integer) vol.get(AdminConstants.A_VOLUME_COMPRESSION_THRESHOLD)).intValue();

        System.out.println("   Volume id: " + vid);
        System.out.println("        name: " + vol.get(AdminConstants.A_VOLUME_NAME));
        System.out.println("        type: " + VolumeCLI.getTypeName(type));
        System.out.println("        path: " + vol.get(AdminConstants.A_VOLUME_ROOTPATH));
        System.out.print("  compressed: " + compressed);
        if (compressed)
            System.out.println("\t         threshold: " + threshold + " bytes");
        else
            System.out.println();
        System.out.println("     current: " + vol.get(AdminConstants.A_VOLUME_IS_CURRENT));
        System.out.println();
    }

    private void listVolumes(ZAuthToken zat, String id, boolean auth)
    throws SoapFaultException, IOException, ServiceException {
        Map<Integer, Map<String, Object>> vols = getVolumes(zat, id, auth);
        for (Iterator<Integer> iter = vols.keySet().iterator(); iter.hasNext(); ) {
            Integer key = iter.next();
            Map<String, Object> vol = vols.get(key);
            listVolume(vol);
        }
    }

    private void deleteVolume(ZAuthToken zat, String id) throws SoapFaultException, IOException, ServiceException {
        Element req = new Element.XMLElement(AdminConstants.DELETE_VOLUME_REQUEST);
        req.addAttribute(AdminConstants.A_ID, id);
        auth(zat);
        getTransport().invokeWithoutSession(req);
        System.out.println("Deleted volume " + id);
    }

    private void editVolume(ZAuthToken zat, String id, String name, String type, String path,
            String fileBits, String fileGroupBits, String mailboxBits,
            String mailboxGroupBits, String compress, String compressThreshold)
    throws ParseException, SoapFaultException, IOException, ServiceException {

        Element req = new Element.XMLElement(AdminConstants.MODIFY_VOLUME_REQUEST);
        req.addAttribute(AdminConstants.A_ID, id);
        Element vol = req.addElement(AdminConstants.E_VOLUME);
        addAttributes(vol, name, type, path, fileBits, fileGroupBits,
                mailboxBits, mailboxGroupBits, compress, compressThreshold);
        auth(zat);
        getTransport().invokeWithoutSession(req);
        System.out.println("Edited volume " + id);
    }

    private void addVolume(ZAuthToken zat, String name, String type, String path,
            String fileBits, String fileGroupBits, String mailboxBits,
            String mailboxGroupBits, String compress, String compressThreshold)
            throws ParseException, SoapFaultException, IOException, ServiceException {

        if (name == null || type == null || path == null)
            throw new ParseException("at least one of the required parameters (name, type, path) is missing");
        Element req = new Element.XMLElement(AdminConstants.CREATE_VOLUME_REQUEST);
        Element vol = req.addElement(AdminConstants.E_VOLUME);
        if (compress == null)
            compress = "false";
        if (compressThreshold == null)
            compressThreshold = "4096";
        addAttributes(vol, name, type, path, fileBits, fileGroupBits,
                mailboxBits, mailboxGroupBits, compress, compressThreshold);
        auth(zat);
        Element resp = getTransport().invokeWithoutSession(req);
        vol = resp.getElement(AdminConstants.E_VOLUME);
        String id = vol.getAttribute(AdminConstants.A_ID);
        System.out.println("Volume " + id + " is created");
    }

    private void addAttributes(Element vol, String name, String type,
            String path, String fileBits, String fileGroupBits,
            String mailboxBits, String mailboxGroupBits, String compress,
            String compressThreshold)
    throws ParseException {

        // validate compress parameter
        if (compress != null) {
            if (!"true".equals(compress) && !"false".equals(compress))
                throw new ParseException("expecting true or false for compress option");
        }

        // the parameters may be null in the case of modifications
        if (type != null) {
            short t = VolumeCLI.getTypeId(type);
            if (t < 0)
                throw new ParseException("invalid volume type: " + type);
            vol.addAttribute(AdminConstants.A_VOLUME_TYPE, t);
        }
        if (name != null)
            vol.addAttribute(AdminConstants.A_VOLUME_NAME, name);
        if (path != null)
            vol.addAttribute(AdminConstants.A_VOLUME_ROOTPATH, path);
        if (compress != null)
            vol.addAttribute(AdminConstants.A_VOLUME_COMPRESS_BLOBS, "true".equals(compress));
        if (compressThreshold != null)
            vol.addAttribute(AdminConstants.A_VOLUME_COMPRESSION_THRESHOLD,
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
        og.setRequired(true);
        options.addOptionGroup(og);
        options.addOption(O_ID, "id", true, "Volume ID");
        options.addOption(O_T, "type", true,
            "Volume type (primaryMessage, secondaryMessage, or index)");
        options.addOption(O_N, "name", true, "volume name");
        options.addOption(O_P, "path", true, "Root path");
        options.addOption(O_C, "compress", true, "Compress blobs; \"true\" or \"false\"");
        options.addOption(O_CT, "compressionThreshold", true, "Compression threshold; default 4KB");
        options.addOption(SoapCLI.OPT_AUTHTOKEN);
        options.addOption(SoapCLI.OPT_AUTHTOKENFILE);
    }

    protected void usage(ParseException e) {
        if (e != null) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
        }

        System.err.println(getCommandUsage());
        printOpt(O_A, 0);
        printOpt(O_N, 2);
        printOpt(O_T, 2);
        printOpt(O_P, 2);
        printOpt(O_C, 2);
        printOpt(O_CT, 2);
        printOpt(O_E, 0);
        printOpt(O_ID, 2);
        System.err.println("  any of the options listed under -a can also be specified " );
        System.err.println("  to have its value modified.");
        printOpt(O_D, 0);
        printOpt(O_ID, 2);
        printOpt(O_L, 0);
        printOpt(O_ID, 2);
        System.err.println("  -id is optional.");
        printOpt(O_DC, 0);
        printOpt(O_SC, 0);
        printOpt(O_ID, 2);
        printOpt(O_TS, 0);
        printOpt(SoapCLI.O_AUTHTOKEN, 0);
        printOpt(SoapCLI.O_AUTHTOKENFILE, 0);
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
        System.err.println(buf.toString());
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
