/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.volume;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpException;

import com.google.common.base.Strings;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.SoapCLI;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.CreateVolumeRequest;
import com.zimbra.soap.admin.message.CreateVolumeResponse;
import com.zimbra.soap.admin.message.DeleteVolumeRequest;
import com.zimbra.soap.admin.message.GetAllVolumesRequest;
import com.zimbra.soap.admin.message.GetAllVolumesResponse;
import com.zimbra.soap.admin.message.GetVolumeRequest;
import com.zimbra.soap.admin.message.GetVolumeResponse;
import com.zimbra.soap.admin.message.ModifyVolumeRequest;
import com.zimbra.soap.admin.message.SetCurrentVolumeRequest;
import com.zimbra.soap.admin.type.VolumeInfo;

public final class VolumeCLI extends SoapCLI {

    private static final String O_A = "a";
    private static final String O_D = "d";
    private static final String O_L = "l";
    private static final String O_E = "e";
    private static final String O_DC = "dc";
    private static final String O_SC = "sc";
    private static final String O_TS = "ts";
    private static final String O_ID = "id";
    private static final String O_T = "t";
    private static final String O_N = "n";
    private static final String O_P = "p";
    private static final String O_C = "c";
    private static final String O_CT = "ct";

    private VolumeCLI() throws ServiceException {
        super();
        setupCommandLineOptions();
    }

    private ZAuthToken auth;
    private String id;
    private String type;
    private String name;
    private String path;
    private String compress;
    private String compressThreshold;

    private void setArgs(CommandLine cl) throws ServiceException, ParseException, IOException {
        auth = getZAuthToken(cl);
        id = cl.getOptionValue(O_ID);
        type = cl.getOptionValue(O_T);
        name = cl.getOptionValue(O_N);
        path = cl.getOptionValue(O_P);
        compress = cl.getOptionValue(O_C);
        compressThreshold = cl.getOptionValue(O_CT);
    }

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
            if (cl == null) {
                return;
            }
            util.setArgs(cl);

            if (cl.hasOption(O_A)) {
                util.addVolume();
            } else if (cl.hasOption(O_D)) {
                util.deleteVolume();
            } else if (cl.hasOption(O_L)) {
                util.getVolume();
            } else if (cl.hasOption(O_E)) {
                util.editVolume();
            } else if (cl.hasOption(O_DC)) {
                util.getCurrentVolumes();
            } else if (cl.hasOption(O_SC)) {
                util.setCurrentVolume();
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

    private void setCurrentVolume() throws ParseException, SoapFaultException, IOException, ServiceException, NumberFormatException, HttpException {
        if (id == null) {
            throw new ParseException("id is missing");
        }

        auth(auth);
        GetVolumeResponse resp = JaxbUtil.elementToJaxb(getTransport().invokeWithoutSession(
                JaxbUtil.jaxbToElement(new GetVolumeRequest(Short.valueOf(id)))));
        VolumeInfo vol = resp.getVolume();
        SetCurrentVolumeRequest req = new SetCurrentVolumeRequest(vol.getId(), vol.getType());
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(req));

        System.out.println("Volume " + id + " is now the current " + toTypeName(vol.getType()) + " volume.");
    }

    private void unsetCurrentSecondaryMessageVolume() throws SoapFaultException, IOException, ServiceException, HttpException {
        SetCurrentVolumeRequest req = new SetCurrentVolumeRequest(Volume.ID_NONE, Volume.TYPE_MESSAGE_SECONDARY);
        auth(auth);
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(req));
        System.out.println("Turned off the current secondary message volume.");
    }

    private void getCurrentVolumes() throws SoapFaultException, IOException, ServiceException, HttpException {
        GetAllVolumesRequest req = new GetAllVolumesRequest();
        auth(auth);
        GetAllVolumesResponse all = JaxbUtil.elementToJaxb(getTransport().invokeWithoutSession(
                JaxbUtil.jaxbToElement(req)));
        for (VolumeInfo vol : all.getVolumes()) {
            if (vol.isCurrent()) {
                print(vol);
            }
        }
    }

    private void getVolume() throws IOException, ServiceException, HttpException {
        if (id == null) {
            GetAllVolumesRequest req = new GetAllVolumesRequest();
            auth(auth);
            GetAllVolumesResponse resp = JaxbUtil.elementToJaxb(getTransport().invokeWithoutSession(
                    JaxbUtil.jaxbToElement(req)));
            for (VolumeInfo vol : resp.getVolumes()) {
                print(vol);
            }
        } else {
            GetVolumeRequest req = new GetVolumeRequest(Short.parseShort(id));
            auth(auth);
            GetVolumeResponse resp = JaxbUtil.elementToJaxb(getTransport().invokeWithoutSession(
                    JaxbUtil.jaxbToElement(req)));
            print(resp.getVolume());
        }
    }

    private void print(VolumeInfo vol) {
        System.out.println("   Volume id: " + vol.getId());
        System.out.println("        name: " + vol.getName());
        System.out.println("        type: " + toTypeName(vol.getType()));
        System.out.println("        path: " + vol.getRootPath());
        System.out.print("  compressed: " + vol.isCompressBlobs());
        if (vol.isCompressBlobs()) {
            System.out.println("\t         threshold: " + vol.getCompressionThreshold() + " bytes");
        } else {
            System.out.println();
        }
        System.out.println("     current: " + vol.isCurrent());
        System.out.println();
    }

    private void deleteVolume() throws ParseException, SoapFaultException, IOException, ServiceException, HttpException {
        if (id == null) {
            throw new ParseException("id is missing");
        }

        DeleteVolumeRequest req = new DeleteVolumeRequest(Short.parseShort(id));
        auth(auth);
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(req));
        System.out.println("Deleted volume " + id);
    }

    private void editVolume() throws ParseException, SoapFaultException, IOException, ServiceException, HttpException {
        if (Strings.isNullOrEmpty(id)) {
            throw new ParseException("id is missing");
        }

        VolumeInfo vol = new VolumeInfo();
        if (!Strings.isNullOrEmpty(type)) {
            vol.setType(toType(type));
        }
        if (!Strings.isNullOrEmpty(name)) {
            vol.setName(name);
        }
        if (!Strings.isNullOrEmpty(path)) {
            vol.setRootPath(path);
        }
        if (!Strings.isNullOrEmpty(compress)) {
            vol.setCompressBlobs(Boolean.parseBoolean(compress));
        }
        if (!Strings.isNullOrEmpty(compressThreshold)) {
            vol.setCompressionThreshold(Long.parseLong(compressThreshold));
        }
        ModifyVolumeRequest req = new ModifyVolumeRequest(Short.parseShort(id), vol);
        auth(auth);
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(req));
        System.out.println("Edited volume " + id);
    }

    private void addVolume() throws ParseException, SoapFaultException, IOException, ServiceException, HttpException {
        if (id != null) {
            throw new ParseException("id cannot be specified when adding a volume");
        }
        if (Strings.isNullOrEmpty(type)) {
            throw new ParseException("type is missing");
        }
        if (Strings.isNullOrEmpty(name)) {
            throw new ParseException("name is missing");
        }
        if (Strings.isNullOrEmpty(path)) {
            throw new ParseException("path is missing");
        }

        VolumeInfo vol = new VolumeInfo();
        vol.setType(toType(type));
        vol.setName(name);
        vol.setRootPath(path);
        vol.setCompressBlobs(compress != null ? Boolean.parseBoolean(compress) : false);
        vol.setCompressionThreshold(compressThreshold != null ? Long.parseLong(compressThreshold) : 4096L);
        CreateVolumeRequest req = new CreateVolumeRequest(vol);
        auth();
        CreateVolumeResponse resp = JaxbUtil.elementToJaxb(getTransport().invokeWithoutSession(
                JaxbUtil.jaxbToElement(req)));
        System.out.println("Volume " + resp.getVolume().getId() + " is created");
    }

    @Override
    protected String getCommandUsage() {
        return "zmvolume {-a | -d | -l | -e | -dc | -sc } <options>";
    }

    @Override
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
        options.addOption(O_T, "type", true, "Volume type (primaryMessage, secondaryMessage, or index)");
        options.addOption(O_N, "name", true, "volume name");
        options.addOption(O_P, "path", true, "Root path");
        options.addOption(O_C, "compress", true, "Compress blobs; \"true\" or \"false\"");
        options.addOption(O_CT, "compressionThreshold", true, "Compression threshold; default 4KB");
        options.addOption(SoapCLI.OPT_AUTHTOKEN);
        options.addOption(SoapCLI.OPT_AUTHTOKENFILE);
    }

    @Override
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

    private void printOpt(String optStr, int leftPad) {
        Options options = getOptions();
        Option opt = options.getOption(optStr);
        StringBuilder buf = new StringBuilder();
        buf.append(Strings.repeat(" ", leftPad));
        buf.append('-').append(opt.getOpt()).append(",--").append(opt.getLongOpt());
        if (opt.hasArg()) {
            buf.append(" <arg>");
        }
        buf.append(Strings.repeat(" ", 35 - buf.length()));
        buf.append(opt.getDescription());
        System.err.println(buf.toString());
    }

    /**
     * Returns the type id for the given name.
     */
    private short toType(String name) throws ParseException {
        if ("primaryMessage".equalsIgnoreCase(name)) {
            return Volume.TYPE_MESSAGE;
        }
        if ("secondaryMessage".equalsIgnoreCase(name)) {
            return Volume.TYPE_MESSAGE_SECONDARY;
        }
        if ("index".equalsIgnoreCase(name)) {
            return Volume.TYPE_INDEX;
        }
        throw new ParseException("invalid volume type: " + name);
    }

    /**
     * Returns the human-readable name for the given volume type.
     */
    private String toTypeName(short type) {
        switch (type) {
            case Volume.TYPE_MESSAGE:
                return "primaryMessage";
            case Volume.TYPE_MESSAGE_SECONDARY:
                return "secondaryMessage";
            case Volume.TYPE_INDEX:
                return "index";
        }
        return "Unrecognized type " + type;
    }
}
