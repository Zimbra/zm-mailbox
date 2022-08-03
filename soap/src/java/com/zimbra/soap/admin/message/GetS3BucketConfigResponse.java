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

package com.zimbra.soap.admin.message;

import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.GlobalS3BucketConfiguration;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_GET_S3_BUCKET_CONFIG_RESPONSE)
public final class GetS3BucketConfigResponse {

    /**
     * @zm-api-field-description Information about globalS3BucketConfigs
     */
    @XmlElement(name = "globalS3BucketConfigurations", required = true)
    private final List<GlobalS3BucketConfiguration> globalS3BucketConfigurations = Lists.newArrayList();

    public void setGlobalS3BucketConfigurations(Collection<GlobalS3BucketConfiguration> list) {
        globalS3BucketConfigurations.clear();
        if (list != null && !list.isEmpty()) {
            globalS3BucketConfigurations.addAll(list);
        }
    }

    public List<GlobalS3BucketConfiguration> getGlobalS3BucketConfigurations() {
        return globalS3BucketConfigurations;
    }

    public void addGlobalS3BucketConfiguration(GlobalS3BucketConfiguration globalS3BucketConfig) {
        globalS3BucketConfigurations.add(globalS3BucketConfig);
    }

}
