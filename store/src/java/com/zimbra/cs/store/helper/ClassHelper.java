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
