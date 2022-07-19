/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

import com.google.common.base.Strings;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.SoapCLI;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.GlobalExternalStoreConfigConstants;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.admin.message.CreateS3BucketConfigRequest;
import com.zimbra.soap.admin.message.CreateS3BucketConfigResponse;
import com.zimbra.soap.admin.message.GetS3BucketConfigRequest;
import com.zimbra.soap.admin.message.GetS3BucketConfigResponse;
import com.zimbra.soap.admin.message.DeleteS3BucketConfigRequest;
import com.zimbra.soap.admin.message.DeleteS3BucketConfigResponse;
import com.zimbra.soap.type.GlobalExternalStoreConfig;
import com.zimbra.soap.type.GlobalS3BucketConfiguration;
import com.zimbra.soap.JaxbUtil;

public final class VolumeS3ConfigCLI extends SoapCLI {

    private static final String  O_A    = "a";
    private static final String  O_D    = "d";
    private static final String  O_L    = "l";
    private static final String  O_BID  = "bid";
    private static final String  O_BN   = "bn";
    private static final String  O_AK   = "ak";
    private static final String  O_SK   = "sk";
    private static final String  O_DP   = "dp";
    private static final String  O_R    = "r";
    private static final String  O_ACC  = "acc";
    private static final String  O_NS   = "ns";
    private static final String  O_PP   = "pp";
    private static final String  O_URL  = "url";
    private static final String  O_SP   = "t";

    private ZAuthToken auth;
    private String storeProvider = "";
    private String bucketId = "";
    private String bucketName = "";
    private String accessKey = "";;
    private String secretKey = "";
    private String destPath = "";
    private String region = "";
    private String account = "";
    private String namespace = "";
    private String proxyPort = "";
    private String accountPort = "";
    private String url = "";

    private VolumeS3ConfigCLI() throws ServiceException {
        super();
        setupCommandLineOptions();
    }

    @Override
    protected void setupCommandLineOptions() {
        super.setupCommandLineOptions();

        // Main Option Group
        OptionGroup og = new OptionGroup();
        og.addOption(new Option(O_A, "add", false, "Adds a s3BucketConfig"));
        og.addOption(new Option(O_D, "delete", false, "Deletes a s3BucketConfig"));
        og.addOption(new Option(O_L, "list", false, "Lists all s3BucketConfig"));
        og.setRequired(true);

        // Options
        Options options = getOptions();
        options.addOptionGroup(og);
        options.addOption(O_BN, "bucketName", true, "Specify name for s3BucketConfig");
        options.addOption(O_AK, "accessKey", true, "Specify access key for s3BucketConfig");
        options.addOption(O_SK, "secretKey", true, "Specify secret key for s3BucketConfig");
        options.addOption(O_DP, "destPath", true, "Specify destination path for s3BucketConfig");
        options.addOption(O_R, "region", true, "Specify region for s3BucketConfig");
        options.addOption(O_URL, "url", true, "Specify url for s3BucketConfig");
        options.addOption(O_SP, "type", true, "Specify storeProvider to add s3BucketConfig");
        options.addOption(O_BID, "bucketID", true, "Specify bucket ID to delete s3BucketConfig");
    }

    private void setArgs(CommandLine cl) throws ServiceException, ParseException, IOException {
        auth          = getZAuthToken(cl);
        bucketId      = cl.getOptionValue(O_BID);
        bucketName    = cl.getOptionValue(O_BN);
        accessKey     = cl.getOptionValue(O_AK);
        secretKey     = cl.getOptionValue(O_SK);
        destPath      = cl.getOptionValue(O_DP);
        region        = cl.getOptionValue(O_R);
        url           = cl.getOptionValue(O_URL);
        storeProvider = cl.getOptionValue(O_SP);
    }

    @Override
    protected String getCommandUsage() {
        return "zms3config {-a | -d | -l } <options>";
    }

    @Override
    protected void usage(ParseException e) {
        if (e != null) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
        }

        // command usasge
        System.err.println(getCommandUsage());

        // add option
        printOpt(O_A, 0);
        printOpt(O_SP, 2);
        printOpt(O_BN, 2);
        printOpt(O_AK, 2);
        printOpt(O_SK, 2);
        printOpt(O_DP, 2);
        printOpt(O_R, 2);
        printOpt(O_URL, 2);
        printOpt(O_BID, 2);

        // delete option
        printOpt(O_D, 0);
        printOpt(O_BID, 2);

        // list option
        printOpt(O_L, 0);
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
     * This method lists all S3 Bucket Configs
     *
     * @param
     * @throws IOException, ServiceException
     */
    private void printS3BucketConfig(GlobalS3BucketConfiguration item) {
        System.out.println("  globalBucketUUID: " + item.getGlobalBucketUUID());
        System.out.println("        bucketName: " + item.getBucketName());
        System.out.println("     storeProvider: " + item.getStoreProvider());
        System.out.println("          protocol: " + item.getProtocol());
        System.out.println("         storeType: " + item.getProtocol());
        System.out.println("         accessKey: " + item.getAccessKey());
        System.out.println("         secretKey: " + item.getSecretKey());
        System.out.println("            region: " + item.getRegion());
        System.out.println("   destinationPath: " + item.getDestinationPath());
        System.out.println("               url: " + item.getUrl());
        System.out.println("      bucketStatus: " + item.getBucketStatus());
        System.out.println("-------------------------------------------------");
        System.out.println();
    }

    /**
     * This method validate the attributes in del command
     *
     * @param
     * @throws ParseException
     */
    private void validateDelCommand() throws ParseException {
        if (Strings.isNullOrEmpty(bucketId)) {
            throw new ParseException("Bucket ID must be specified while deleting S3BucketConfig");
        }
    }

    /**
     * This method validate the attributes in del command
     *
     * @param
     * @throws ParseException
     */
    private void validateAddCommand() throws ParseException {
        if (storeProvider.equalsIgnoreCase("S3")) {
            validateS3Params();
        } else if (storeProvider.equalsIgnoreCase("Ceph")) {
            validateCephParams();
        } else if (storeProvider.equalsIgnoreCase("StorageGrid")) {
            validateStorageGridParams();
        } else {
            throw new ParseException("Store Provider must be specified while creating S3BucketConfig");
        }
    }

    /**
     * This method validate S3 Params
     *
     * @param
     * @throws ParseException
     */
    private void validateS3Params() throws ParseException {
        validateSecretKey();
        validateBucketName();
        validateAccessKey();
        validateDestinationPath();
        validateRegion();
    }

    /**
     * This method validate Ceph Params
     *
     * @param
     * @throws ParseException
     */
    private void validateCephParams() throws ParseException {
        validateSecretKey();
        validateBucketName();
        validateAccessKey();
        validateDestinationPath();
        validateURL();
    }

    /**
     * This method validate StorageGrid Params
     *
     * @param
     * @throws ParseException
     */
    private void validateStorageGridParams() throws ParseException {
        validateSecretKey();
        validateBucketName();
        validateAccessKey();
        validateDestinationPath();
        validateURL();
    }

    /**
     * This method validate the attribute BucketId
     *
     * @param
     * @throws ParseException
     */
    private void validateBucketId() throws ParseException {
        if (Strings.isNullOrEmpty(bucketId)) {
            throw new ParseException("Bucket ID must be specified while deleting S3BucketConfig");
        }
    }

    /**
     * This method validate the attribute Bucket Name
     *
     * @param
     * @throws ParseException
     */
    private void validateBucketName() throws ParseException {
        if (Strings.isNullOrEmpty(bucketName)) {
            throw new ParseException("Bucket Name must be specified while creating S3BucketConfig");
        }
    }

    /**
     * This method validate the attribute Access Key
     *
     * @param
     * @throws ParseException
     */
    private void validateAccessKey() throws ParseException {
        if (Strings.isNullOrEmpty(accessKey)) {
            throw new ParseException("Access Key must be specified while creating S3BucketConfig");
        }
    }

    /**
     * This method validate the attribute Destination Path
     *
     * @param
     * @throws ParseException
     */
    private void validateDestinationPath() throws ParseException {
        if (Strings.isNullOrEmpty(destPath)) {
            throw new ParseException("Destination Path must be specified while creating S3BucketConfig");
        }
    }

    /**
     * This method validate the attribute Region
     *
     * @param
     * @throws ParseException
     */
    private void validateRegion() throws ParseException {
        if (Strings.isNullOrEmpty(region)) {
            throw new ParseException("Region must be specified while creating S3BucketConfig");
        }
    }

    /**
     * This method validate the attribute URL
     *
     * @param
     * @throws ParseException
     */
    private void validateURL() throws ParseException {
        if (Strings.isNullOrEmpty(region)) {
            throw new ParseException("URL must be specified while creating S3BucketConfig");
        }
    }

    /**
     * This method validate the attribute Secret Key
     *
     * @param
     * @throws ParseException
     */
    private void validateSecretKey() throws ParseException {
        if (Strings.isNullOrEmpty(secretKey)) {
            throw new ParseException("Secret Key must be specified while creating S3BucketConfig");
        }
    }

    /**
     * This method validate the attribute storeProvider
     *
     * @param
     * @throws ParseException
     */
    private void validateStoreProvider() throws ParseException {
        if (Strings.isNullOrEmpty(storeProvider)) {
            throw new ParseException("StoreProvider must be specified while creating S3BucketConfig");
        }
    }

    /**
     * Main function
     *
     * @param
     */
    public static void main(String[] args) {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zms3config", BuildInfo.VERSION);
        VolumeS3ConfigCLI util = null;
        try {
            util = new VolumeS3ConfigCLI();
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
                util.addS3BucketConfig();
            } else if (cl.hasOption(O_D)) {
                util.delS3BucketConfig();
            } else if (cl.hasOption(O_L)) {
                util.lstS3BucketConfigs();
            } else {
                throw new ParseException("No action (-a,-d,-l) is specified");
            }
            System.exit(0);
        } catch (ParseException e) {
            util.usage(e);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
        }
        System.exit(1);
    }

    /**
     * This method adds S3 Bucket Config
     *
     * @param
     * @throws IOException, ServiceException, ParseException
     */
    private void addS3BucketConfig() throws IOException, ServiceException, ParseException {
        validateAddCommand();
        CreateS3BucketConfigResponse s3BucketConfigResponse = resp_CreateS3BucketConfigRequest();

        // fetch bucket id of newly created S3 Bucket Config
        String bid = "";
        for (Attr item: s3BucketConfigResponse.getAttrs()) {
            if (item.getKey().equals(GlobalExternalStoreConfigConstants.A_S3_GLOBAL_BUCKET_UUID)) {
                bid = item.getValue();
                break;
            }
        }

        // check S3BucketConfig is created or not
        if (StringUtil.isNullOrEmpty(bid) || !isS3BucketConfigExists(bid)) {
            System.out.println("Error: unable to create s3BucketConfig");
        } else {
            System.out.println("Sucess: s3BucketConfig sucessfully created");
        }
    }

    /**
     * This method delete S3 Bucket Config
     *
     * @param
     * @throws IOException, ServiceException, ParseException
     */
    private void delS3BucketConfig() throws IOException, ServiceException, ParseException {
        validateDelCommand();
        resp_DeleteS3BucketConfigRequest(bucketId);
        if (isS3BucketConfigExists(bucketId)) {
            System.out.println("Error: unable to delete s3BucketConfig");
        } else {
            System.out.println("Sucess: s3BucketConfig sucessfully deleted");
        }
    }

    /**
     * This method lists all S3 Bucket Configs
     *
     * @param
     * @throws IOException, ServiceException
     */
    private void lstS3BucketConfigs() throws IOException, ServiceException {
        GetS3BucketConfigResponse s3BucketConfigResponse = resp_GetS3BucketConfigRequest();
        GlobalExternalStoreConfig glbExtStoreConfig = s3BucketConfigResponse.getGlobalExternalStoreConfig();
        List<GlobalS3BucketConfiguration> s3BucketConfigs = glbExtStoreConfig.getGlobalS3BucketConfigurations();
        for (GlobalS3BucketConfiguration item : s3BucketConfigs) {
            printS3BucketConfig(item);
        }
    }

    /**
     * This method fetches response for GetS3BucketConfigRequest
     *
     * @param
     * @throws IOException, ServiceException
     */
    private GetS3BucketConfigResponse resp_GetS3BucketConfigRequest() throws IOException, ServiceException {
        GetS3BucketConfigRequest s3BucketConfigRequest = new GetS3BucketConfigRequest();
        auth(auth);
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(s3BucketConfigRequest));
        Element s3BucketConfigElement = getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(s3BucketConfigRequest));
        return JaxbUtil.elementToJaxb(s3BucketConfigElement);
    }

    /**
     * This method fetches response for DeleteS3BucketConfigRequest
     *
     * @param
     * @throws IOException, ServiceException
     */
    private DeleteS3BucketConfigResponse resp_DeleteS3BucketConfigRequest(String b_Id) throws IOException, ServiceException {
        DeleteS3BucketConfigRequest s3BucketConfigRequest = new DeleteS3BucketConfigRequest();
        // System.out.println("***LOG*** Bucket ID: " + b_Id);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(GlobalExternalStoreConfigConstants.A_S3_GLOBAL_BUCKET_UUID, b_Id);
        s3BucketConfigRequest.setAttrs(attrs);
        // s3BucketConfigRequest.addAttr(GlobalExternalStoreConfigConstants.A_S3_GLOBAL_BUCKET_UUID, b_Id);
        auth(auth);
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(s3BucketConfigRequest));
        Element s3BucketConfigElement = getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(s3BucketConfigRequest));
        return JaxbUtil.elementToJaxb(s3BucketConfigElement);
    }

    /**
     * This method fetches response for CreateS3BucketConfigRequest
     *
     * @param
     * @throws IOException, ServiceException
     */
    private CreateS3BucketConfigResponse resp_CreateS3BucketConfigRequest() throws IOException, ServiceException {
        CreateS3BucketConfigRequest s3BucketConfigRequest = new CreateS3BucketConfigRequest();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(GlobalExternalStoreConfigConstants.A_S3_STORE_PROVIDER, storeProvider);
        attrs.put(GlobalExternalStoreConfigConstants.A_S3_BUCKET_NAME, bucketName);
        attrs.put(GlobalExternalStoreConfigConstants.A_S3_ACCESS_KEY, accessKey);
        attrs.put(GlobalExternalStoreConfigConstants.A_S3_SECRET_KEY, secretKey);
        attrs.put(GlobalExternalStoreConfigConstants.A_S3_DESTINATION_PATH, destPath);
        if (storeProvider.equalsIgnoreCase("S3")) {
            attrs.put(GlobalExternalStoreConfigConstants.A_S3_REGION, region);
        } else {
            attrs.put(GlobalExternalStoreConfigConstants.A_S3_URL, url);
        }
        s3BucketConfigRequest.setAttrs(attrs);
        auth(auth);
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(s3BucketConfigRequest));
        Element s3BucketConfigElement = getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(s3BucketConfigRequest));
        return JaxbUtil.elementToJaxb(s3BucketConfigElement);
    }

    /**
     * This method checks whether the s3BucketConfig exists or not
     *
     * @param
     * @throws IOException, ServiceException
     */
    private boolean isS3BucketConfigExists(String b_Id) throws IOException, ServiceException {
        GetS3BucketConfigResponse s3BucketConfigResponse = resp_GetS3BucketConfigRequest();
        GlobalExternalStoreConfig glbExtStoreConfig = s3BucketConfigResponse.getGlobalExternalStoreConfig();
        List<GlobalS3BucketConfiguration> s3BucketConfigs = glbExtStoreConfig.getGlobalS3BucketConfigurations();
        for (GlobalS3BucketConfiguration item : s3BucketConfigs) {
            if(item.getGlobalBucketUUID().equals(b_Id)) {
                return true;
            }
        }
        return false;
    }
}
