/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016, 2022 Synacor, Inc.
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
import com.zimbra.soap.admin.type.VolumeExternalInfo;
import com.zimbra.soap.admin.type.VolumeExternalOpenIOInfo;
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

    private static final String O_SMC = "smc";
    
    /** attributes for external storetype **/
    private static final String O_ST = "st";
    private static final String O_VP = "vp";
    private static final String O_STP = "stp";
    private static final String O_BID = "bid";

    /** attributes for store type OpenIO **/
    private static final String O_AP = "ap";
    private static final String O_PP = "pp";
    private static final String O_ACCOUNT = "acc";
    private static final String O_NS = "ns";
    private static final String O_URL = "url";
    private static final String OPENIO = "OPENIO";

    private static final String NOT_ALLOWED_INTERNAL = " is not allowed for internal storetype";
    private static final String NOT_ALLOWED_EXTERNAL = " is not allowed for external storetype";
    private static final String NOT_ALLOWED = " is not allowed for edit";
    private static final String INVALID_STORE_TYPE = "invalid storetype";
    private static final String HELP_EXTERNAL_NAME = "  only name can be edited for external store volumes ";
    private static final String MISSING_ATTRS = " is missing";
    private static final String NOT_ALLOWED_ID = "id cannot be specified when adding a volume";

    private static final String H_STORE_TYPE = "Store type: internal or external";
    private static final String H_STORAGE_TYPE = "Name of the store provider (S3, ObjectStore, OpenIO)";
    private static final String H_BUCKET_ID = "S3 Bucket ID";
    private static final String H_VOLUME_PREFIX = "Volume Preifx";
    private static final String H_URL = "URL of OpenIO";
    private static final String H_PROXY_PORT = "Proxy port";
    private static final String H_ACCOUNT_PORT = "Account port";
    private static final String H_ACCOUNT = "Name of account";
    private static final String H_NAME_SPACE = "Namespace";
    private static final String H_STORE_MANAGER_CLASS = "Optional parameter to specify non-default store manager class path";

    private static final String A_ID = "id";
    private static final String A_TYPE = "type";
    private static final String A_PATH = "path";
    private static final String A_NAME = "name";
    private static final String A_COMPRESS = "compress";
    private static final String A_COMPRESS_THRESHOLD = "compressThreshold";
    private static final String A_STORE_TYPE = "storeType";

    private static final String A_STORE_MANAGER_CLASS = "storeManagerClass";
    private static final String A_STORAGE_TYPE = "storageType";
    private static final String A_BUCKET_ID = "bucketId";
    private static final String A_VOLUME_PREFIX = "volumePrefix";
    private static final String A_VOLUME_S3 = "S3";
    
    private static final String A_URL = "url";
    private static final String A_PROXY_PORT = "proxyPort";
    private static final String A_ACCOUNT_PORT = "accountPort";
    private static final String A_NAME_SPACE = "nameSpace";
    private static final String A_ACCOUNT = "account";

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
    private String storeType;
    private String volumePrefix;
    private String storageType;
    private String bucketId;
    
    // openIO Attributes
    private String url;
    private String proxyPort;
    private String accountPort;
    private String nameSpace;
    private String account;
    private String storeManagerClass;

    private void setArgs(CommandLine cl) throws ServiceException, ParseException, IOException {
        auth = getZAuthToken(cl);
        id = cl.getOptionValue(O_ID);
        type = cl.getOptionValue(O_T);
        name = cl.getOptionValue(O_N);
        path = cl.getOptionValue(O_P);
        compress = cl.getOptionValue(O_C);
        compressThreshold = cl.getOptionValue(O_CT);
        storeType = cl.getOptionValue(O_ST);
        storeType = storeType == null ? null : storeType.toUpperCase();
        volumePrefix = cl.getOptionValue(O_VP);
        storageType = cl.getOptionValue(O_STP);
        bucketId = cl.getOptionValue(O_BID);
        if(OPENIO.equals(storageType)) {
            url = cl.getOptionValue(O_URL);
            proxyPort = cl.getOptionValue(O_PP);
            accountPort = cl.getOptionValue(O_AP);
            nameSpace = cl.getOptionValue(O_NS);
            account = cl.getOptionValue(O_ACCOUNT);
        }
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
            throw new ParseException(A_ID + MISSING_ATTRS);
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
        System.out.println("                   Volume id: " + vol.getId());
        System.out.println("                        name: " + vol.getName());
        System.out.println("                        type: " + toTypeName(vol.getType()));
        System.out.println("                        path: " + vol.getRootPath());
        System.out.print("                  compressed: " + vol.isCompressBlobs());
        
        if (vol.isCompressBlobs()) {
            System.out.println("\t                 threshold: " + vol.getCompressionThreshold() + " bytes");
        } else {
            System.out.println();
        }
        System.out.println("                     current: " + vol.isCurrent());
        if(vol.getStoreType() == 2) {
            VolumeExternalInfo volumeExternalInfo = vol.getVolumeExternalInfo();
            if (A_VOLUME_S3.equals(volumeExternalInfo.getStorageType())) {
                System.out.println("                      prefix: " + volumeExternalInfo.getVolumePrefix());
                System.out.println(" globalBucketConfigurationId: " + volumeExternalInfo.getGlobalBucketConfigurationId());
                System.out.println("                 storageType: " + volumeExternalInfo.getStorageType());
                System.out.println("       useIntelligentTiering: " + volumeExternalInfo.isUseIntelligentTiering());
                System.out.println("         useInFrequentAccess: " + volumeExternalInfo.isUseInFrequentAccess());
                System.out.println("useInFrequentAccessThreshold: " + volumeExternalInfo.getUseInFrequentAccessThreshold());
            }
        }
        System.out.println();
    }
    
    private void deleteVolume() throws ParseException, SoapFaultException, IOException, ServiceException, HttpException {
        if (id == null) {
            throw new ParseException(A_ID + MISSING_ATTRS);
        }

        DeleteVolumeRequest req = new DeleteVolumeRequest(Short.parseShort(id));
        auth(auth);
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(req));
        System.out.println("Deleted volume " + id);
    }

    private void editVolume() throws ParseException, SoapFaultException, IOException, ServiceException, HttpException {
        if (Strings.isNullOrEmpty(id)) {
            throw new ParseException(A_ID + MISSING_ATTRS);
        }

        GetVolumeRequest getVolumeRequest = new GetVolumeRequest(Short.parseShort(id));
        auth();
        GetVolumeResponse getVolumeResponse = JaxbUtil
                .elementToJaxb(getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(getVolumeRequest)));
        Volume.StoreType enumStoreType = (1 == getVolumeResponse.getVolume().getStoreType()) ? Volume.StoreType.INTERNAL
                : Volume.StoreType.EXTERNAL;

        VolumeInfo vol = new VolumeInfo();
        validateEditCommand(vol, enumStoreType);
        ModifyVolumeRequest req = new ModifyVolumeRequest(Short.parseShort(id), vol);
        auth(auth);
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(req));
        System.out.println("Edited volume " + id);
    }

    private void addVolume() throws ParseException, SoapFaultException, IOException, ServiceException, HttpException {
        if (id != null) {
            throw new ParseException(NOT_ALLOWED_ID);
        }
        if (Strings.isNullOrEmpty(type)) {
            throw new ParseException(A_TYPE + MISSING_ATTRS);
        }
        if (Strings.isNullOrEmpty(name)) {
            throw new ParseException(A_NAME + MISSING_ATTRS);
        }

        VolumeInfo vol = new VolumeInfo();
        vol.setType(toType(type));
        vol.setName(name);
        vol.setCompressBlobs(compress != null ? Boolean.parseBoolean(compress) : false);
        vol.setCompressionThreshold(compressThreshold != null ? Long.parseLong(compressThreshold) : 4096L);
        if (!Strings.isNullOrEmpty(storeManagerClass)) {
            vol.setStoreManagerClass(storeManagerClass);
        }
        validateAddCommand(vol);
        CreateVolumeRequest req = new CreateVolumeRequest(vol);
        auth();
        CreateVolumeResponse resp = JaxbUtil.elementToJaxb(getTransport().invokeWithoutSession(
                JaxbUtil.jaxbToElement(req)));
        System.out.println("Volume " + resp.getVolume().getId() + " is created");
    }
    
    /**
     * This method validate the attributes in edit command.
     * 
     * @param volumeInfo, volStoreType
     * @throws ParseException
     */

    private void validateEditCommand(VolumeInfo volumeInfo, Volume.StoreType volStoreType) throws ParseException {
        if (volStoreType.equals(Volume.StoreType.INTERNAL)) {
            if (!Strings.isNullOrEmpty(type)) {
                volumeInfo.setType(toType(type));
            }
            if (!Strings.isNullOrEmpty(path)) {
                volumeInfo.setRootPath(path);
            }
            if (!Strings.isNullOrEmpty(compress)) {
                volumeInfo.setCompressBlobs(Boolean.parseBoolean(compress));
            }
            if (!Strings.isNullOrEmpty(compressThreshold)) {
                volumeInfo.setCompressionThreshold(Long.parseLong(compressThreshold));
            }
        } else if (volStoreType.equals(Volume.StoreType.EXTERNAL)) {
            if (!Strings.isNullOrEmpty(type)) {
                throw new ParseException(A_TYPE + NOT_ALLOWED_EXTERNAL);
            }
            if (!Strings.isNullOrEmpty(path)) {
                throw new ParseException(A_PATH + NOT_ALLOWED_EXTERNAL);
            }
            if (!Strings.isNullOrEmpty(compress)) {
                throw new ParseException(A_COMPRESS + NOT_ALLOWED_EXTERNAL);
            }
            if (!Strings.isNullOrEmpty(compressThreshold)) {
                throw new ParseException(A_COMPRESS_THRESHOLD + NOT_ALLOWED_EXTERNAL);
            }
            if(OPENIO.equals(storageType)) {
                if (!Strings.isNullOrEmpty(url)) {
                    throw new ParseException(A_URL + NOT_ALLOWED_EXTERNAL);
                }
                if (!Strings.isNullOrEmpty(nameSpace)) {
                    throw new ParseException(A_NAME_SPACE + NOT_ALLOWED_EXTERNAL);
                }
                if (!Strings.isNullOrEmpty(proxyPort)) {
                    throw new ParseException(A_PROXY_PORT + NOT_ALLOWED_EXTERNAL);
                }
                if (!Strings.isNullOrEmpty(accountPort)) {
                    throw new ParseException(A_ACCOUNT_PORT + NOT_ALLOWED_EXTERNAL);
                }
            }
        } else {
            throw new ParseException(INVALID_STORE_TYPE);
        }
        if (!Strings.isNullOrEmpty(name)) {
            volumeInfo.setName(name);
        }
        if (!Strings.isNullOrEmpty(storeType)) {
            throw new ParseException(A_STORE_TYPE + NOT_ALLOWED);
        }
        if (!Strings.isNullOrEmpty(storageType)) {
            throw new ParseException(A_STORAGE_TYPE + NOT_ALLOWED);
        }
        if (!Strings.isNullOrEmpty(bucketId)) {
            throw new ParseException(A_BUCKET_ID + NOT_ALLOWED);
        }
        if (!Strings.isNullOrEmpty(volumePrefix)) {
            throw new ParseException(A_VOLUME_PREFIX + NOT_ALLOWED);
        }
    }

    /**
     * This method validate the attributes in add command.
     * 
     * @param volumeInfo
     * @throws ParseException
     */

    private void validateAddCommand(VolumeInfo volumeInfo) throws ParseException {
        if (Strings.isNullOrEmpty(storeType) || Volume.StoreType.INTERNAL.name().equals(storeType)) {
            if (!Strings.isNullOrEmpty(storageType)) {
                throw new ParseException(A_STORAGE_TYPE + NOT_ALLOWED_INTERNAL);
            }
            if (!Strings.isNullOrEmpty(bucketId)) {
                throw new ParseException(A_BUCKET_ID + NOT_ALLOWED_INTERNAL);
            }
            if (!Strings.isNullOrEmpty(volumePrefix)) {
                throw new ParseException(A_VOLUME_PREFIX + NOT_ALLOWED_INTERNAL);
            }
            if (Strings.isNullOrEmpty(path)) {
                throw new ParseException(A_PATH + MISSING_ATTRS);
            }
            volumeInfo.setRootPath(path);
        } else if (Volume.StoreType.EXTERNAL.name().equals(storeType)) {
            if(storageType.equalsIgnoreCase(OPENIO)) {
                VolumeExternalOpenIOInfo volumeExternalOpenIOInfo = new VolumeExternalOpenIOInfo();
                if (!Strings.isNullOrEmpty(storageType)) {
                    volumeExternalOpenIOInfo.setStorageType(storageType);
                }
                if (!Strings.isNullOrEmpty(url)) {
                    volumeExternalOpenIOInfo.setUrl(url);
                } else {
                    throw new ParseException(A_URL + MISSING_ATTRS);
                }
                if (!Strings.isNullOrEmpty(nameSpace)) {
                    volumeExternalOpenIOInfo.setNameSpace(nameSpace);
                } else {
                    throw new ParseException(A_NAME_SPACE + MISSING_ATTRS);
                }
                if (!Strings.isNullOrEmpty(proxyPort)) {
                    volumeExternalOpenIOInfo.setProxyPort(Integer.parseInt(proxyPort));
                } else {
                    throw new ParseException(A_PROXY_PORT + MISSING_ATTRS);
                }
                if (!Strings.isNullOrEmpty(accountPort)) {
                    volumeExternalOpenIOInfo.setAccountPort(Integer.parseInt(accountPort));
                } else {
                    throw new ParseException(A_ACCOUNT_PORT + MISSING_ATTRS);
                }
                if (!Strings.isNullOrEmpty(account)) {
                    volumeExternalOpenIOInfo.setAccount(account);
                } else {
                    throw new ParseException(A_ACCOUNT + MISSING_ATTRS);
                }
                volumeInfo.setVolumeExternalOpenIOInfo(volumeExternalOpenIOInfo);
            } else {
                VolumeExternalInfo volumeExternalInfo = new VolumeExternalInfo();
                if (!Strings.isNullOrEmpty(storageType)) {
                    volumeExternalInfo.setStorageType(storageType);
                }
                if (!Strings.isNullOrEmpty(bucketId)) {
                    volumeExternalInfo.setGlobalBucketConfigurationId(bucketId);
                }
                if (!Strings.isNullOrEmpty(volumePrefix)) {
                    volumeExternalInfo.setVolumePrefix(volumePrefix);
                }
                volumeInfo.setVolumeExternalInfo(volumeExternalInfo);
            }
            volumeInfo.setStoreType((short) Volume.StoreType.EXTERNAL.getStoreType());
        } else {
            throw new ParseException(INVALID_STORE_TYPE);
        }
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
        options.addOption(new Option(O_ST, A_STORE_TYPE, true, H_STORE_TYPE));
        options.addOption(new Option(O_VP, A_VOLUME_PREFIX, true, H_VOLUME_PREFIX));
        options.addOption(new Option(O_STP, A_STORAGE_TYPE, true, H_STORAGE_TYPE));
        options.addOption(new Option(O_BID, A_BUCKET_ID, true, H_BUCKET_ID));
        //OpenIO
        options.addOption(new Option(O_AP, A_ACCOUNT_PORT, true, H_ACCOUNT_PORT));
        options.addOption(new Option(O_PP, A_PROXY_PORT, true, H_PROXY_PORT));
        options.addOption(new Option(O_URL, A_URL, true, H_URL));
        options.addOption(new Option(O_NS, A_NAME_SPACE, true, H_NAME_SPACE));
        options.addOption(new Option(O_ACCOUNT, A_ACCOUNT, true, H_ACCOUNT));
        options.addOption(new Option(O_SMC, A_STORE_MANAGER_CLASS, true, H_STORE_MANAGER_CLASS));
    }

    @Override
    protected void usage(ParseException e) {
        if (e != null) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
        }

        System.err.println(getCommandUsage());
        printOpt(O_A, 0);
        printOpt(O_N, 2);
        System.err.println(HELP_EXTERNAL_NAME);
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
        printOpt(O_ST, 0);
        printOpt(O_VP, 0);
        printOpt(O_STP, 0);
        printOpt(O_BID, 0);
        printOpt(O_PP, 0);
        printOpt(O_AP, 0);
        printOpt(O_ACCOUNT, 0);
        printOpt(O_URL, 0);
        printOpt(O_NS, 0);

        printOpt(O_SMC, 0);
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