package com.zimbra.util;

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2025 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for retrieving and counting Zimbra domains based on the value of a specified domain feature attribute.
 * <p>
 * Provides methods to get all domains where a given feature is enabled and to count such domains.
 */
public class DomainFilterBasedOnFeature {

    private static final String TRUE = "TRUE";
    private static final String FEATURE_NAME_VALIDATOR = "^[a-zA-Z]+$";
    /**
     * The entry point of the program.
     *
     * <p>This method expects two command-line arguments:</p>
     * <ol>
     *     <li><b>featureName</b> — the name of the feature to check for in domains.</li>
     *     <li><b>attrs</b> — a comma-separated list of attribute names to retrieve.</li>
     * </ol>
     *
     * <p>It retrieves all domains where the specified feature is enabled and prints each domain's name
     * and associated value to the console, separated by a comma.</p>
     *
     * @param args Command-line arguments where:
     *             <ul>
     *                 <li><code>args[0]</code> is the feature name to check.</li>
     *                 <li><code>args[1]</code> is a comma-separated list of attribute names.</li>
     *             </ul>
     * @throws ServiceException if there is an error while retrieving domain information.
     */
    public static void main(String[] args) throws ServiceException {
        try {
            if (args.length != 2) {
                throw new IllegalArgumentException("Invalid number of arguments. Expected 2 arguments: featureName and attrs.");
            }
            String featureName = args[0];
            String[] attrs = args[1].split(",");
            if (!featureName.matches(FEATURE_NAME_VALIDATOR) || Arrays.stream(attrs).anyMatch(attr -> !attr.matches(FEATURE_NAME_VALIDATOR))) {
                throw new IllegalArgumentException("Invalid input. featureName and attrs should contain only alphabets.");
            }
            if (featureName.isEmpty() || attrs.length == 0) {
                throw new IllegalArgumentException("Invalid input. featureName and attrs cannot be empty.");
            }
            Map<String, String> domains = getAllDomainsAndIdWithFeatureEnabled(featureName, attrs);
            for (Map.Entry<String, String> entry : domains.entrySet()) {
                System.out.println(entry.getKey() + "," + entry.getValue());
            }
        } catch (IllegalArgumentException | ServiceException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Retrieves all Zimbra domains that have the specified feature enabled (attribute value set to {@code TRUE}).
     *
     * @param featureName the name of the domain attribute representing the feature to check
     * @return a map containing domain names as keys and their corresponding domain IDs as values, for domains where the feature is enabled
     * @throws ServiceException if there is an issue fetching domain entries from the Provisioning service
     */
    private static Map<String, String> getAllDomainsAndIdWithFeatureEnabled(String featureName, String[] attrs) throws ServiceException {
        Map<String, String> domainMap = new HashMap<>();
        Provisioning prov = Provisioning.getInstance();
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            @Override
            public void visit(NamedEntry entry) throws ServiceException {
                if (null == entry) {
                    return;
                }
                String featureValue = entry.getAttr(featureName);
                if (TRUE.equalsIgnoreCase(Optional.ofNullable(featureValue).orElse("").trim())) {
                    String domainName = entry.getName();
                    String domainId = entry.getAttr(Provisioning.A_zimbraId);
                    if (domainName != null && domainId != null) {
                        domainMap.put(domainName, domainId);
                    }
                }
            }
        };
        prov.getAllDomains(visitor, attrs);
        return domainMap;
    }
}

