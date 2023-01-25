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
import com.zimbra.common.soap.AdminConstants;
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
    private String unified;

    // openIO Attributes
    private String url;
    private String proxyPort;
    private String accountPort;
    private String nameSpace;
    private String account;
    private String storeManagerClass;

    private String useInfrequentAccess;
    private String useInfrequentAccessThreshold;
    private String useIntelligentTiering;

    private void setArgs(CommandLine cl) throws ServiceException, ParseException, IOException {
        auth = getZAuthToken(cl);
        id = cl.getOptionValue(VolumeCLIConstants.O_ID);
        type = cl.getOptionValue(VolumeCLIConstants.O_T);
        name = cl.getOptionValue(VolumeCLIConstants.O_N);
        path = cl.getOptionValue(VolumeCLIConstants.O_P);
        compress = cl.getOptionValue(VolumeCLIConstants.O_C);
        compressThreshold = cl.getOptionValue(VolumeCLIConstants.O_CT);
        storeType = cl.getOptionValue(VolumeCLIConstants.O_ST);
        storeType = storeType == null ? null : storeType.toUpperCase();
        volumePrefix = cl.getOptionValue(VolumeCLIConstants.O_VP);
        storageType = cl.getOptionValue(VolumeCLIConstants.O_STP);
        bucketId = cl.getOptionValue(VolumeCLIConstants.O_GBID);
        url = cl.getOptionValue(VolumeCLIConstants.O_URL);
        proxyPort = cl.getOptionValue(VolumeCLIConstants.O_PP);
        accountPort = cl.getOptionValue(VolumeCLIConstants.O_AP);
        nameSpace = cl.getOptionValue(VolumeCLIConstants.O_NS);
        account = cl.getOptionValue(VolumeCLIConstants.O_ACC);
        storeManagerClass = cl.getOptionValue(VolumeCLIConstants.O_SMC);
        useInfrequentAccess = cl.getOptionValue(VolumeCLIConstants.O_UFA);
        useInfrequentAccessThreshold = cl.getOptionValue(VolumeCLIConstants.O_UFAT);
        useIntelligentTiering = cl.getOptionValue(VolumeCLIConstants.O_UIT);
        unified = cl.getOptionValue(VolumeCLIConstants.O_UN);
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

            if (cl.hasOption(VolumeCLIConstants.O_A)) {
                util.addVolume();
            } else if (cl.hasOption(VolumeCLIConstants.O_D)) {
                util.deleteVolume();
            } else if (cl.hasOption(VolumeCLIConstants.O_L)) {
                util.getVolume();
            } else if (cl.hasOption(VolumeCLIConstants.O_E)) {
                util.editVolume();
            } else if (cl.hasOption(VolumeCLIConstants.O_DC)) {
                util.getCurrentVolumes();
            } else if (cl.hasOption(VolumeCLIConstants.O_SC)) {
                util.setCurrentVolume();
            } else if (cl.hasOption(VolumeCLIConstants.O_TS)) {
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
            throw new ParseException(AdminConstants.A_ID + VolumeCLIConstants.MISSING_ATTRS);
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
        System.out.println("           storeManagerClass: " + vol.getStoreManagerClass());
        if (vol.getStoreType() == Volume.StoreType.EXTERNAL.getStoreType()) {
            if (AdminConstants.A_VOLUME_S3.equals(extractStorageType(vol))) {
                VolumeExternalInfo volumeExternalInfo = vol.getVolumeExternalInfo();
                System.out.println("                      prefix: " + volumeExternalInfo.getVolumePrefix());
                System.out.println(" globalBucketConfigurationId: " + volumeExternalInfo.getGlobalBucketConfigurationId());
                System.out.println("                 storageType: " + volumeExternalInfo.getStorageType());
                System.out.println("       useIntelligentTiering: " + volumeExternalInfo.isUseIntelligentTiering());
                System.out.println("         useInFrequentAccess: " + volumeExternalInfo.isUseInFrequentAccess());
                System.out.println("useInFrequentAccessThreshold: " + volumeExternalInfo.getUseInFrequentAccessThreshold());
            } else if (AdminConstants.A_VOLUME_OPEN_IO.equals(extractStorageType(vol))) {
                VolumeExternalOpenIOInfo volumeExternalOpenIOInfo = vol.getVolumeExternalOpenIOInfo();
                System.out.println("                 storageType: " + volumeExternalOpenIOInfo.getStorageType());
                System.out.println("                         url: " + volumeExternalOpenIOInfo.getUrl());
                System.out.println("                     account: " + volumeExternalOpenIOInfo.getAccount());
                System.out.println("                   nameSpace: " + volumeExternalOpenIOInfo.getNameSpace());
                System.out.println("                   proxyPort: " + volumeExternalOpenIOInfo.getProxyPort());
                System.out.println("                 accountPort: " + volumeExternalOpenIOInfo.getAccountPort());
            }
        }
        System.out.println();
    }

    private void deleteVolume() throws ParseException, SoapFaultException, IOException, ServiceException, HttpException {
        if (id == null) {
            throw new ParseException(AdminConstants.A_ID + VolumeCLIConstants.MISSING_ATTRS);
        }

        DeleteVolumeRequest req = new DeleteVolumeRequest(Short.parseShort(id));
        auth(auth);
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(req));
        System.out.println("Deleted volume " + id);
    }

    private void editVolume() throws ParseException, SoapFaultException, IOException, ServiceException, HttpException {
        if (Strings.isNullOrEmpty(id)) {
            throw new ParseException(AdminConstants.A_ID + VolumeCLIConstants.MISSING_ATTRS);
        }

        GetVolumeRequest getVolumeRequest = new GetVolumeRequest(Short.parseShort(id));
        auth();
        GetVolumeResponse getVolumeResponse = JaxbUtil
                .elementToJaxb(getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(getVolumeRequest)));
        VolumeInfo respVolumeInfo = getVolumeResponse.getVolume();
        Volume.StoreType enumStoreType = (1 == respVolumeInfo.getStoreType()) ? Volume.StoreType.INTERNAL
                : Volume.StoreType.EXTERNAL;
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setCompressBlobs(respVolumeInfo.isCompressBlobs());
        volumeInfo.setCompressionThreshold(respVolumeInfo.getCompressionThreshold());
        validateEditCommand(volumeInfo, enumStoreType);
        ModifyVolumeRequest req = new ModifyVolumeRequest(Short.parseShort(id), volumeInfo);
        auth(auth);
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(req));
        System.out.println("Edited volume " + id);
    }

    private void addVolume() throws ParseException, SoapFaultException, IOException, ServiceException, HttpException {
        if (id != null) {
            throw new ParseException(VolumeCLIConstants.NOT_ALLOWED_ID);
        }
        if (Strings.isNullOrEmpty(type)) {
            throw new ParseException(AdminConstants.A_TYPE + VolumeCLIConstants.MISSING_ATTRS);
        }
        if (Strings.isNullOrEmpty(name)) {
            throw new ParseException(AdminConstants.A_NAME + VolumeCLIConstants.MISSING_ATTRS);
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
                if (volumeInfo.isCompressBlobs()) {
                    volumeInfo.setCompressionThreshold(Long.parseLong(compressThreshold));
                } else {
                    throw new ParseException("compressThreshold cannot be edited when the compress option is disabled for the volume.");
                }
            }
        } else if (volStoreType.equals(Volume.StoreType.EXTERNAL)) {
            if (!Strings.isNullOrEmpty(type)) {
                throw new ParseException(AdminConstants.A_TYPE + VolumeCLIConstants.NOT_ALLOWED_EXTERNAL);
            }
            if (!Strings.isNullOrEmpty(path)) {
                throw new ParseException(AdminConstants.A_PATH + VolumeCLIConstants.NOT_ALLOWED_EXTERNAL);
            }
            if (!Strings.isNullOrEmpty(compress)) {
                throw new ParseException(AdminConstants.A_VOLUME_COMPRESS_BLOBS + VolumeCLIConstants.NOT_ALLOWED_EXTERNAL);
            }
            if (!Strings.isNullOrEmpty(compressThreshold)) {
                throw new ParseException(AdminConstants.A_VOLUME_COMPRESSION_THRESHOLD + VolumeCLIConstants.NOT_ALLOWED_EXTERNAL);
            }
            if (!Strings.isNullOrEmpty(url)) {
                throw new ParseException(AdminConstants.A_VOLUME_URL + VolumeCLIConstants.NOT_ALLOWED_EXTERNAL);
            }
            if (!Strings.isNullOrEmpty(account)) {
                throw new ParseException(AdminConstants.A_VOLUME_ACCOUNT + VolumeCLIConstants.NOT_ALLOWED_EXTERNAL);
            }
            if (!Strings.isNullOrEmpty(nameSpace)) {
                throw new ParseException(AdminConstants.A_VOLUME_NAME_SPACE + VolumeCLIConstants.NOT_ALLOWED_EXTERNAL);
            }
            if (!Strings.isNullOrEmpty(proxyPort)) {
                throw new ParseException(AdminConstants.A_VOLUME_PROXY_PORT + VolumeCLIConstants.NOT_ALLOWED_EXTERNAL);
            }
            if (!Strings.isNullOrEmpty(accountPort)) {
                throw new ParseException(AdminConstants.A_VOLUME_ACCOUNT_PORT + VolumeCLIConstants.NOT_ALLOWED_EXTERNAL);
            }
        } else {
            throw new ParseException(VolumeCLIConstants.INVALID_STORE_TYPE);
        }
        if (!Strings.isNullOrEmpty(name)) {
            volumeInfo.setName(name);
        }
        if (!Strings.isNullOrEmpty(storeType)) {
            throw new ParseException(AdminConstants.A_VOLUME_STORE_TYPE + VolumeCLIConstants.NOT_ALLOWED);
        }
        if (!Strings.isNullOrEmpty(storageType)) {
            throw new ParseException(AdminConstants.A_VOLUME_STORAGE_TYPE + VolumeCLIConstants.NOT_ALLOWED);
        }
        if (!Strings.isNullOrEmpty(bucketId)) {
            throw new ParseException(AdminConstants.A_VOLUME_BUCKET_ID + VolumeCLIConstants.NOT_ALLOWED);
        }
        if (!Strings.isNullOrEmpty(volumePrefix)) {
            throw new ParseException(AdminConstants.A_VOLUME_VOLUME_PREFIX + VolumeCLIConstants.NOT_ALLOWED);
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
                throw new ParseException(AdminConstants.A_VOLUME_STORAGE_TYPE + VolumeCLIConstants.NOT_ALLOWED_INTERNAL);
            }
            if (!Strings.isNullOrEmpty(bucketId)) {
                throw new ParseException(AdminConstants.A_VOLUME_BUCKET_ID + VolumeCLIConstants.NOT_ALLOWED_INTERNAL);
            }
            if (!Strings.isNullOrEmpty(volumePrefix)) {
                throw new ParseException(AdminConstants.A_VOLUME_VOLUME_PREFIX + VolumeCLIConstants.NOT_ALLOWED_INTERNAL);
            }
            if (Strings.isNullOrEmpty(path)) {
                throw new ParseException(AdminConstants.A_PATH + VolumeCLIConstants.MISSING_ATTRS);
            }
            volumeInfo.setRootPath(path);
        } else if (Volume.StoreType.EXTERNAL.name().equals(storeType)) {
            if (AdminConstants.A_VOLUME_OPEN_IO.equalsIgnoreCase(storageType)) {
                VolumeExternalOpenIOInfo volumeExternalOpenIOInfo = new VolumeExternalOpenIOInfo();
                if (!Strings.isNullOrEmpty(storageType)) {
                    volumeExternalOpenIOInfo.setStorageType(storageType);
                }
                if (!Strings.isNullOrEmpty(url)) {
                    volumeExternalOpenIOInfo.setUrl(url);
                } else {
                    throw new ParseException(AdminConstants.A_VOLUME_URL + VolumeCLIConstants.MISSING_ATTRS);
                }
                if (!Strings.isNullOrEmpty(nameSpace)) {
                    volumeExternalOpenIOInfo.setNameSpace(nameSpace);
                } else {
                    throw new ParseException(AdminConstants.A_VOLUME_NAMESPACE + VolumeCLIConstants.MISSING_ATTRS);
                }
                if (!Strings.isNullOrEmpty(proxyPort)) {
                    volumeExternalOpenIOInfo.setProxyPort(Integer.parseInt(proxyPort));
                } else {
                    throw new ParseException(AdminConstants.A_VOLUME_PROXY_PORT + VolumeCLIConstants.MISSING_ATTRS);
                }
                if (!Strings.isNullOrEmpty(accountPort)) {
                    volumeExternalOpenIOInfo.setAccountPort(Integer.parseInt(accountPort));
                } else {
                    throw new ParseException(AdminConstants.A_VOLUME_ACCOUNT_PORT + VolumeCLIConstants.MISSING_ATTRS);
                }
                if (!Strings.isNullOrEmpty(account)) {
                    volumeExternalOpenIOInfo.setAccount(account);
                } else {
                    throw new ParseException(AdminConstants.A_ACCOUNT + VolumeCLIConstants.MISSING_ATTRS);
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
                if (!Strings.isNullOrEmpty(useInfrequentAccess)
                        && validateBoolean(AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS, useInfrequentAccess)) {
                    volumeExternalInfo.setUseInFrequentAccess(Boolean.parseBoolean(useInfrequentAccess));
                }
                if (!Strings.isNullOrEmpty(useIntelligentTiering)
                        && validateBoolean(AdminConstants.A_VOLUME_USE_INTELLIGENT_TIERING, useIntelligentTiering)) {
                    volumeExternalInfo.setUseIntelligentTiering(Boolean.parseBoolean(useIntelligentTiering));
                }
                if (!Strings.isNullOrEmpty(useInfrequentAccessThreshold)) {
                    volumeExternalInfo.setUseInFrequentAccessThreshold(Integer.parseInt(useInfrequentAccessThreshold));
                }
                if (!Strings.isNullOrEmpty(unified)
                        && validateBoolean(AdminConstants.A_VOLUME_UNIFIED, unified)) {
                    volumeExternalInfo.setUnified(Boolean.parseBoolean(unified));
                }
                volumeInfo.setVolumeExternalInfo(volumeExternalInfo);
            }
            volumeInfo.setStoreType((short) Volume.StoreType.EXTERNAL.getStoreType());
        } else {
            throw new ParseException(VolumeCLIConstants.INVALID_STORE_TYPE);
        }
    }

    private boolean validateBoolean(String param, String value) throws ParseException {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return true;
        }
        throw new ParseException("invalid value of " + param);
    }

    @Override
    protected String getCommandUsage() {
        return "Usage: " + VolumeCLIConstants.STR_MAIN_CMD_NAME +
                VolumeCLIConstants.STR_SPACE + VolumeCLIConstants.STR_MAIN_CLI_ARGS +
                " <options>";
    }

    @Override
    protected void setupCommandLineOptions() {
        super.setupCommandLineOptions();
        Options options = getOptions();
        OptionGroup og = new OptionGroup();
        og.addOption(new Option(VolumeCLIConstants.O_A,  VolumeCLIConstants.H_OPT_ADD,            false, VolumeCLIConstants.H_DESC_VOL_ADD));
        og.addOption(new Option(VolumeCLIConstants.O_D,  VolumeCLIConstants.H_OPT_DEL,            false, VolumeCLIConstants.H_DESC_VOL_DEL));
        og.addOption(new Option(VolumeCLIConstants.O_L,  VolumeCLIConstants.H_OPT_LIST,           false, VolumeCLIConstants.H_DESC_VOL_LST));
        og.addOption(new Option(VolumeCLIConstants.O_E,  VolumeCLIConstants.H_OPT_EDIT,           false, VolumeCLIConstants.H_DESC_VOL_EDIT));
        og.addOption(new Option(VolumeCLIConstants.O_DC, VolumeCLIConstants.H_OPT_DISP_CURR,      false, VolumeCLIConstants.H_DESC_VOL_DISP_CURR));
        og.addOption(new Option(VolumeCLIConstants.O_SC, VolumeCLIConstants.H_OPT_SET_CURR,       false, VolumeCLIConstants.H_DESC_VOL_SET_CURR));
        og.addOption(new Option(VolumeCLIConstants.O_TS, VolumeCLIConstants.H_OPT_TURN_OFF_SDRY,  false, VolumeCLIConstants.H_DESC_VOL_OFF_CURR));
        og.setRequired(true);
        options.addOptionGroup(og);
        options.addOption(VolumeCLIConstants.O_ID, AdminConstants.A_ID,                            true, VolumeCLIConstants.H_DESC_VOL_ID);
        options.addOption(VolumeCLIConstants.O_T,  AdminConstants.A_TYPE,                          true, VolumeCLIConstants.H_DESC_VOL_TYPE_PSI);
        options.addOption(VolumeCLIConstants.O_N,  AdminConstants.A_NAME,                          true, VolumeCLIConstants.H_DESC_VOL_NAME);
        options.addOption(VolumeCLIConstants.O_P,  AdminConstants.A_PATH,                          true, VolumeCLIConstants.H_DESC_VOL_RP);
        options.addOption(VolumeCLIConstants.O_C,  AdminConstants.A_VOLUME_COMPRESS_BLOBS,         true, VolumeCLIConstants.H_DESC_VOL_CB);
        options.addOption(VolumeCLIConstants.O_CT, AdminConstants.A_VOLUME_COMPRESSION_THRESHOLD,  true, VolumeCLIConstants.H_DESC_VOL_CT);
        options.addOption(SoapCLI.OPT_AUTHTOKEN);
        options.addOption(SoapCLI.OPT_AUTHTOKENFILE);
        options.addOption(new Option(VolumeCLIConstants.O_ST,    AdminConstants.A_VOLUME_STORE_TYPE,                   true, VolumeCLIConstants.H_DESC_VOL_ST));
        options.addOption(new Option(VolumeCLIConstants.O_VP,    AdminConstants.A_VOLUME_VOLUME_PREFIX,                true, VolumeCLIConstants.H_DESC_VOL_PRE));
        options.addOption(new Option(VolumeCLIConstants.O_STP,   AdminConstants.A_VOLUME_STORAGE_TYPE,                 true, VolumeCLIConstants.H_DESC_VOL_STP));
        options.addOption(new Option(VolumeCLIConstants.O_GBID,  AdminConstants.A_VOLUME_GLB_BUCKET_CONFIG_ID,         true, VolumeCLIConstants.H_DESC_VOL_BKT_ID));
        options.addOption(new Option(VolumeCLIConstants.O_UFA,   AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS,           true, VolumeCLIConstants.H_DESC_VOL_AWS_UFA));
        options.addOption(new Option(VolumeCLIConstants.O_UFAT,  AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS_THRESHOLD, true, VolumeCLIConstants.H_DESC_VOL_AWS_UFAT));
        options.addOption(new Option(VolumeCLIConstants.O_UIT,   AdminConstants.A_VOLUME_USE_INTELLIGENT_TIERING,      true, VolumeCLIConstants.H_DESC_VOL_AWS_UIT));
        options.addOption(new Option(VolumeCLIConstants.O_SMC,   AdminConstants.A_VOLUME_STORE_MANAGER_CLASS,          true, VolumeCLIConstants.H_DESC_VOL_SMC));
        options.addOption(new Option(VolumeCLIConstants.O_UN,    AdminConstants.A_VOLUME_UNIFIED,                      true,VolumeCLIConstants.H_DESC_VOL_UN));
        // OpenIO
        options.addOption(new Option(VolumeCLIConstants.O_AP,  AdminConstants.A_VOLUME_ACCOUNT_PORT, true, VolumeCLIConstants.H_DESC_VOL_AP));
        options.addOption(new Option(VolumeCLIConstants.O_PP,  AdminConstants.A_VOLUME_PROXY_PORT,   true, VolumeCLIConstants.H_DESC_VOL_PP));
        options.addOption(new Option(VolumeCLIConstants.O_URL, AdminConstants.A_VOLUME_URL,          true, VolumeCLIConstants.H_DESC_VOL_URL));
        options.addOption(new Option(VolumeCLIConstants.O_NS,  AdminConstants.A_VOLUME_NAME_SPACE,   true, VolumeCLIConstants.H_DESC_VOL_NS));
        options.addOption(new Option(VolumeCLIConstants.O_ACC, AdminConstants.A_ACCOUNT,             true, VolumeCLIConstants.H_DESC_VOL_ACC));
      }

    @Override
    protected void usage(ParseException e) {
        if (e != null) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
        }
        printLineWithLeftPad(getCommandUsage(), 0);

        printLineWithLeftPad(VolumeCLIConstants.STR_EMPTY, 0);
        printLineWithLeftPad(VolumeCLIConstants.H_TLE_INT_VOL_ADD, 0);
        printOptWithDesc(VolumeCLIConstants.O_A,   4, VolumeCLIConstants.H_DESC_VOL_ADD,        false);
        printOptWithDesc(VolumeCLIConstants.O_N,   8, VolumeCLIConstants.H_DESC_VOL_NAME,       true);
        printOptWithDesc(VolumeCLIConstants.O_T,   8, VolumeCLIConstants.H_DESC_VOL_TYPE_PSI,   true);
        printOptWithDesc(VolumeCLIConstants.O_P,   8, VolumeCLIConstants.H_DESC_VOL_RP,         true);
        printOptWithDesc(VolumeCLIConstants.O_ST,  8, VolumeCLIConstants.H_DESC_VOL_STI,        false);
        printOptWithDesc(VolumeCLIConstants.O_C,   8, VolumeCLIConstants.H_DESC_VOL_CB,         false);
        printOptWithDesc(VolumeCLIConstants.O_CT,  8, VolumeCLIConstants.H_DESC_VOL_CT,         false);
        printOptWithDesc(VolumeCLIConstants.O_SMC, 8, VolumeCLIConstants.H_DESC_VOL_SMC,        false);
        printLineWithLeftPad(VolumeCLIConstants.H_EXP_INT_VOL_ADD, 4);

        printLineWithLeftPad(VolumeCLIConstants.STR_EMPTY, 0);
        printLineWithLeftPad(VolumeCLIConstants.H_TLE_EXT_S3_VOL_ADD, 0);
        printOptWithDesc(VolumeCLIConstants.O_A,     4, VolumeCLIConstants.H_DESC_VOL_ADD,        false);
        printOptWithDesc(VolumeCLIConstants.O_N,     8, VolumeCLIConstants.H_DESC_VOL_NAME,       true);
        printOptWithDesc(VolumeCLIConstants.O_T,     8, VolumeCLIConstants.H_DESC_VOL_TYPE_PS,    true);
        printOptWithDesc(VolumeCLIConstants.O_ST,    8, VolumeCLIConstants.H_DESC_VOL_STE,        true);
        printOptWithDesc(VolumeCLIConstants.O_STP,   8, VolumeCLIConstants.H_DESC_VOL_STP_S3,     true);
        printOptWithDesc(VolumeCLIConstants.O_GBID,  8, VolumeCLIConstants.H_DESC_VOL_BKT_ID,     true);
        printOptWithDesc(VolumeCLIConstants.O_P,     8, VolumeCLIConstants.H_DESC_VOL_RP,         true);
        printOptWithDesc(VolumeCLIConstants.O_VP,    8, VolumeCLIConstants.H_DESC_VOL_PRE,        true);
        printOptWithDesc(VolumeCLIConstants.O_UIT,   8, VolumeCLIConstants.H_DESC_VOL_AWS_UIT,    false);
        printOptWithDesc(VolumeCLIConstants.O_UFA,   8, VolumeCLIConstants.H_DESC_VOL_AWS_UFA,    false);
        printOptWithDesc(VolumeCLIConstants.O_UFAT,  8, VolumeCLIConstants.H_DESC_VOL_AWS_UFAT,   false);
        printOptWithDesc(VolumeCLIConstants.O_SMC,   8, VolumeCLIConstants.H_DESC_VOL_SMC,        false);
        printOptWithDesc(VolumeCLIConstants.O_UN,    8, VolumeCLIConstants.H_DESC_VOL_UN,         false);
        printLineWithLeftPad(VolumeCLIConstants.H_NOTE_EXT_S3_VOL_ADD, 4);
        printLineWithLeftPad(VolumeCLIConstants.H_EXP_EXT_S3_VOL_ADD, 4);

        printLineWithLeftPad(VolumeCLIConstants.STR_EMPTY, 0);
        printLineWithLeftPad(VolumeCLIConstants.H_TLE_EXT_OI_VOL_ADD, 0);
        printOptWithDesc(VolumeCLIConstants.O_A,   4,  VolumeCLIConstants.H_DESC_VOL_ADD,       false);
        printOptWithDesc(VolumeCLIConstants.O_N,   8,  VolumeCLIConstants.H_DESC_VOL_NAME,      true);
        printOptWithDesc(VolumeCLIConstants.O_T,   8,  VolumeCLIConstants.H_DESC_VOL_TYPE_PS,   true);
        printOptWithDesc(VolumeCLIConstants.O_ST,  8,  VolumeCLIConstants.H_DESC_VOL_STE,       true);
        printOptWithDesc(VolumeCLIConstants.O_STP, 8,  VolumeCLIConstants.H_DESC_VOL_STP_OI,    true);
        printOptWithDesc(VolumeCLIConstants.O_PP,  8,  VolumeCLIConstants.H_DESC_VOL_PP,        true);
        printOptWithDesc(VolumeCLIConstants.O_AP,  8,  VolumeCLIConstants.H_DESC_VOL_AP,        true);
        printOptWithDesc(VolumeCLIConstants.O_ACC, 8,  VolumeCLIConstants.H_DESC_VOL_ACC,       true);
        printOptWithDesc(VolumeCLIConstants.O_URL, 8,  VolumeCLIConstants.H_DESC_VOL_URL,       true);
        printOptWithDesc(VolumeCLIConstants.O_NS,  8,  VolumeCLIConstants.H_DESC_VOL_NS,        true);
        printOptWithDesc(VolumeCLIConstants.O_P,   8,  VolumeCLIConstants.H_DESC_VOL_RP,        true);
        printOptWithDesc(VolumeCLIConstants.O_VP,  8,  VolumeCLIConstants.H_DESC_VOL_PRE,       true);
        printOptWithDesc(VolumeCLIConstants.O_SMC, 8,  VolumeCLIConstants.H_DESC_VOL_SMC,       false);
        printLineWithLeftPad(VolumeCLIConstants.H_NOTE_EXT_OI_VOL_ADD, 4);
        printLineWithLeftPad(VolumeCLIConstants.H_EXP_EXT_OI_VOL_ADD, 4);

        printLineWithLeftPad(VolumeCLIConstants.STR_EMPTY, 0);
        printLineWithLeftPad(VolumeCLIConstants.H_TLE_INT_VOL_EDIT, 0);
        printOptWithDesc(VolumeCLIConstants.O_E,   4,  VolumeCLIConstants.H_DESC_VOL_EDIT,      false);
        printOptWithDesc(VolumeCLIConstants.O_ID,  8,  VolumeCLIConstants.H_DESC_VOL_ID,        true);
        printOptWithDesc(VolumeCLIConstants.O_N,   8,  VolumeCLIConstants.H_DESC_VOL_NAME,      false);
        printOptWithDesc(VolumeCLIConstants.O_T,   8,  VolumeCLIConstants.H_DESC_VOL_TYPE_PSI,  false);
        printOptWithDesc(VolumeCLIConstants.O_P,   8,  VolumeCLIConstants.H_DESC_VOL_RP,        false);
        printOptWithDesc(VolumeCLIConstants.O_C,   8,  VolumeCLIConstants.H_DESC_VOL_CB,        false);
        printOptWithDesc(VolumeCLIConstants.O_CT,  8,  VolumeCLIConstants.H_DESC_VOL_CT,        false);
        printLineWithLeftPad(VolumeCLIConstants.H_EXP_INT_VOL_EDIT, 4);

        printLineWithLeftPad(VolumeCLIConstants.STR_EMPTY, 0);
        printLineWithLeftPad(VolumeCLIConstants.H_TLE_EXT_VOL_EDIT, 0);
        printOptWithDesc(VolumeCLIConstants.O_E,   4,  VolumeCLIConstants.H_DESC_VOL_EDIT,      false);
        printOptWithDesc(VolumeCLIConstants.O_ID,  8,  VolumeCLIConstants.H_DESC_VOL_ID,        true);
        printOptWithDesc(VolumeCLIConstants.O_N,   8,  VolumeCLIConstants.H_DESC_VOL_NAME,      true);
        printLineWithLeftPad(VolumeCLIConstants.H_NOTE_EXT_VOL_EDIT, 4);
        printLineWithLeftPad(VolumeCLIConstants.H_EXP_EXT_VOL_EDIT, 4);

        printLineWithLeftPad(VolumeCLIConstants.STR_EMPTY, 0);
        printLineWithLeftPad(VolumeCLIConstants.H_TLE_VOL_DEL, 0);
        printOptWithDesc(VolumeCLIConstants.O_D,   4,  VolumeCLIConstants.H_DESC_VOL_DEL,       false);
        printOptWithDesc(VolumeCLIConstants.O_ID,  8,  VolumeCLIConstants.H_DESC_VOL_ID,        true);
        printLineWithLeftPad(VolumeCLIConstants.H_EXP_VOL_DEL, 4);

        printLineWithLeftPad(VolumeCLIConstants.STR_EMPTY, 0);
        printLineWithLeftPad(VolumeCLIConstants.H_TLE_OTHER_VOL_OPTS, 0);
        printOptWithDesc(VolumeCLIConstants.O_L,   4,  VolumeCLIConstants.H_DESC_VOL_LST,       false);
        printOptWithDesc(VolumeCLIConstants.O_ID,  8,  VolumeCLIConstants.H_DESC_VOL_ID,        false);
        printOptWithDesc(VolumeCLIConstants.O_DC,  4,  VolumeCLIConstants.H_DESC_VOL_DISP_CURR, false);
        printOptWithDesc(VolumeCLIConstants.O_SC,  4,  VolumeCLIConstants.H_DESC_VOL_SET_CURR,  false);
        printOptWithDesc(VolumeCLIConstants.O_ID,  8,  VolumeCLIConstants.H_DESC_VOL_ID,        true);
        printOptWithDesc(VolumeCLIConstants.O_TS,  4,  VolumeCLIConstants.H_DESC_VOL_OFF_CURR,  false);
        printLineWithLeftPad(VolumeCLIConstants.STR_EMPTY, 0);
    }

    private void printOptWithDesc(String optStr, int leftPad, String desc, boolean isRequired) {
        Options options = getOptions();
        Option opt = options.getOption(optStr);
        StringBuilder buf = new StringBuilder();
        buf.append(Strings.repeat(" ", leftPad));
        buf.append('-').append(opt.getOpt()).append(",--").append(opt.getLongOpt());
        if (opt.hasArg()) {
            if (isRequired) {
                buf.append(" <required arg>");
            } else {
                buf.append(" <optional arg>");
            }
        }
        buf.append(Strings.repeat(VolumeCLIConstants.STR_SPACE, VolumeCLIConstants.INT_ALIGN_LEFT - buf.length()));
        buf.append(desc);
        System.out.println(buf.toString());
    }

    private void printLineWithLeftPad(String line, int leftPad) {
        StringBuilder buf = new StringBuilder();
        buf.append(Strings.repeat(VolumeCLIConstants.STR_SPACE, leftPad));
        buf.append(line);
        System.out.println(buf.toString());
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

    /**
     * Returns the storage type name for the given volume info type.
     */
    private String extractStorageType(VolumeInfo volInfo) {
        String result = null;
        if (null != volInfo.getVolumeExternalInfo()) {
            result = volInfo.getVolumeExternalInfo().getStorageType();
        } else if (null != volInfo.getVolumeExternalOpenIOInfo()) {
            result = volInfo.getVolumeExternalOpenIOInfo().getStorageType();
        }
        return result;
    }
}
