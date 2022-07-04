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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.SoapCLI;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.CliUtil;
import com.zimbra.soap.admin.message.GetS3BucketConfigRequest;
import com.zimbra.soap.admin.message.GetS3BucketConfigResponse;
import com.zimbra.soap.admin.message.DeleteS3BucketConfigRequest;
import com.zimbra.soap.JaxbUtil;

public final class VolumeS3ConfigCLI extends SoapCLI {

    private static final String     O_A = "a";
    private static final String     O_D = "d";
    private static final String     O_L = "l";
    private static final String O_BID = "bid";
    private static final String   O_BN = "bn";
    private static final String   O_AK = "ak";
    private static final String     O_S = "s";
    private static final String   O_DP = "dp";
    private static final String     O_R = "r";
    private static final String O_ACC = "acc";
    private static final String   O_NS = "ns";
    private static final String   O_PP = "pp";
    private static final String O_URL = "url";


    private ZAuthToken auth;
    private String bucketId;

    private String bucketName;
    private String accessKey;
    private String secret;
    private String destPath;
    private String region;
    private String account;
    private String namespace;
    private String proxyPort;
    private String accountPort;
    private String url;

    private VolumeS3ConfigCLI() throws ServiceException {
        super();
        setupCommandLineOptions();
    }

    @Override
    protected void setupCommandLineOptions() {
        super.setupCommandLineOptions();

        // Main Option Group
        OptionGroup og = new OptionGroup();
        og.addOption(new Option(O_A, "add", false, "Adds a s3BucketConfig."));
        og.addOption(new Option(O_D, "delete", false, "Deletes a s3BucketConfig."));
        og.addOption(new Option(O_L, "list", false, "Lists all s3BucketConfig."));
        og.setRequired(true);

        // Options
        Options options = getOptions();
        options.addOptionGroup(og);
        options.addOption(O_BN, "bucketName", true, "Bucket Name");
        options.addOption(O_AK, "accessKey", true, "Access Key");
        options.addOption(O_S, "secret", true, "Secret");
        options.addOption(O_DP, "destPath", true, "Destination Path");
        options.addOption(O_R, "region", true, "Region");
        options.addOption(O_ACC, "account", true, "Account");
        options.addOption(O_NS, "nameSpace", true, "Name Space");
        options.addOption(O_PP, "proxyPort", true, "Proxy Port");
        options.addOption(O_URL, "url", true, "URL");
    }

    private void setArgs(CommandLine cl) throws ServiceException, ParseException, IOException {
        auth = getZAuthToken(cl);
        bucketId = cl.getOptionValue(O_BID);
    }

    @Override
    protected String getCommandUsage() {
        return "zms3config {-a | -d | -l } <options>";
    }

    private void prints3BucketConfig() {
        System.out.println("  globalBucketUUID: ");
        System.out.println("        bucketName: ");
        System.out.println("     storeProvider: ");
        System.out.println("          protocol: ");
        System.out.print("           storeType: ");
        System.out.print("           accessKey: ");
        System.out.print("              secret: ");
        System.out.print("              region: ");
        System.out.print("     destinationPath: ");
        System.out.print("                 url: ");
        System.out.print("        bucketStatus: ");
    }

    /**
     * This method validate the attributes in del command.
     *
     * @param
     * @throws ParseException
     */
    private void validateDelCommand() throws ParseException {
        if (Strings.isNullOrEmpty(bucketId)) {
            throw new ParseException("Bucket ID must be specified while deleting S3BucketConfig");
        }
    }

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
                util.lstS3BucketConfig();
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

    private void addS3BucketConfig() {

    }

    private void delS3BucketConfig() throws IOException, ServiceException {
        DeleteS3BucketConfigRequest req = new DeleteS3BucketConfigRequest();
        auth(auth);
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(req));
        Element ele = getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(req));
        GetS3BucketConfigResponse resp = JaxbUtil.elementToJaxb(ele);
    }

    private void lstS3BucketConfig() throws IOException, ServiceException {
        GetS3BucketConfigRequest req = new GetS3BucketConfigRequest();
        auth(auth);
        getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(req));
        Element ele = getTransport().invokeWithoutSession(JaxbUtil.jaxbToElement(req));
        GetS3BucketConfigResponse resp = JaxbUtil.elementToJaxb(ele);

        // ToDo - Validate all global buckets are valid or not, How ?
    }
}
