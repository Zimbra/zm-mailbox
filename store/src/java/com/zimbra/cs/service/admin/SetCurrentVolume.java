/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.admin;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.localconfig.LocalConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.events.volume.PrimaryVolChangeEventPublisher;
import com.zimbra.cs.store.events.volume.PrimaryVolChangedEvent;
import com.zimbra.cs.store.helper.ClassHelper;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.Volume.StoreType;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.SetCurrentVolumeRequest;
import com.zimbra.soap.admin.message.SetCurrentVolumeResponse;
import org.dom4j.DocumentException;

import static com.zimbra.common.localconfig.LC.zimbra_class_store;

public final class SetCurrentVolume extends AdminDocumentHandler {

    @Override
    public Element handle(Element req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        return zsc.jaxbToElement(handle((SetCurrentVolumeRequest) zsc.elementToJaxb(req), ctx));
    }

    private SetCurrentVolumeResponse handle(SetCurrentVolumeRequest req, Map<String, Object> ctx)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        checkRight(zsc, ctx, Provisioning.getInstance().getLocalServer(), Admin.R_manageVolume);

        short volId = req.getId() > 0 ? req.getId() : Volume.ID_NONE;
        VolumeManager.getInstance().setCurrentVolume(req.getType(), volId);
        Volume volume = VolumeManager.getInstance().getVolume(volId);

        // if its primary volume then
        if (Volume.TYPE_MESSAGE == volume.getType()) {
            // set current store manager
            setCurrentStoreManager(volume);
        }
        return new SetCurrentVolumeResponse();
    }

    private void setCurrentStoreManager(Volume volume) throws ServiceException {
         String storeManagerClass = volume.getStoreManagerClass();
        if (StringUtil.isNullOrEmpty(storeManagerClass)) {
            ZimbraLog.store.error("store_manager is not set for volume[%s] in database", volume.getId());
            return;
        }
        if (LC.zimbra_class_store.value().equals(storeManagerClass)) {
            ZimbraLog.store.error("store_manager class is same as set zimbra_class_store", LC.zimbra_class_store.value());
            return;
        }
        if (!ClassHelper.isClassExist(storeManagerClass)) {
            throw ServiceException.OPERATION_DENIED(" store manager class " + storeManagerClass + " not found on classPath");
        }
        //
        try {
            // default store config
            LocalConfig localConfig = new LocalConfig(null);
            localConfig.set(zimbra_class_store.key(), storeManagerClass);
            localConfig.save();
            LC.reload();

            // verify if its set correctly or not
//            if (!LC.zimbra_class_store.value().equals(storeManagerClass)) {
//                throw ServiceException.OPERATION_DENIED("not able to update store manager class " + storeManagerClass + " in cached zimbra_class_store attr");
//            }
            // reset current store Manager
            StoreManager.resetStoreManager();

        } catch (DocumentException | ConfigException | IOException e) {
            throw ServiceException.FAILURE("Enable to update zimbra_class_store attribute", e);
        }
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_manageVolume);
    }
}
