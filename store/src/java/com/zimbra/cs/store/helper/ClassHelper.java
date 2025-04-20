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
package com.zimbra.cs.store.helper;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.extension.ExtensionUtil;

/**
 * Class helper
 */
public class ClassHelper {

    /**
     * Check if the class exist on class path
     * @param classPath
     * @return
     */
    public static boolean isClassExist(String classPath) {
        if (!StringUtil.isNullOrEmpty(classPath)) {
            try {
                try {
                    Class.forName(classPath);
                } catch (ClassNotFoundException e) {
                    ExtensionUtil.findClass(classPath);
                }
                return true;
            } catch (Exception e) {
                ZimbraLog.store.error("unable to initialize blob store", e);
            }
        }
        return false;
    }

    /**
     * Get Class instance by className
     * @param className
     * @return
     * @throws Throwable
     */
    public static Object getZimbraClassInstanceBy(String className) throws ReflectiveOperationException {
        try {
            return Class.forName(className).newInstance();
        } catch (Exception e) {
            return ExtensionUtil.findClass(className).newInstance();
        }
    }
}