package com.zimbra.cs.ml;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

/**
 * Classification task configuration stored in the zimbraMachineLearningTaskConfig LDAP attribute.
 */
public class LdapClassificationTaskConfigProvider extends ClassificationTaskConfigProvider {

    private Map<String, TaskConfig> taskClassifierMap = new HashMap<>();
    private Map<String, String> ldapValueMap = new HashMap<>();

    public LdapClassificationTaskConfigProvider() throws ServiceException {
        parseConfigs(getLocalServer().getMachineLearningTaskConfig());
    }

    private Server getLocalServer() throws ServiceException {
        return Provisioning.getInstance().getLocalServer();
    }

    private static String toLdapValue(String taskName, String classifierLabel) {
        return String.format("%s:%s", taskName, classifierLabel);
    }

    private static String toLdapValue(String taskName, String classifierLabel, float threshold) {
        return String.format("%s:%s:%.2f", taskName, classifierLabel, threshold);
    }

    private void parseConfigs(String[] configStrings) {
        if (configStrings == null) {
            ZimbraLog.ml.warn("no classifier configuration found in LDAP");
        }
        for (String configString: configStrings) {
            parseConfig(configString);
        }
    }

    private void parseConfig(String configStr) {
        String[] toks = configStr.split(":");
        if (toks.length < 2 || toks.length > 3) {
            ZimbraLog.ml.warn("incorrect classifier configuration string: %s", configStr);
            return;
        }
        String taskName = toks[0];
        String classifierLabel = toks[1];
        TaskConfig taskConfig;
        if (toks.length == 3) {
            float threshold;
            try {
                threshold = Float.parseFloat(toks[2]);
            } catch (NumberFormatException e) {
                ZimbraLog.ml.warn("incorrect probability threshold: %s", toks[2]);
                return;
            }
            taskConfig = new TaskConfig(classifierLabel, threshold);
        } else {
            taskConfig = new TaskConfig(classifierLabel);
        }
        taskClassifierMap.put(taskName, taskConfig);
        ldapValueMap.put(taskName, configStr);
    }

    @Override
    public Map<String, TaskConfig> getConfigMap() {
        return taskClassifierMap;
    }


    @Override
    protected void assign(String taskName, String classifierLabel, Float threshold) throws ServiceException {
        TaskConfig curAssignment = taskClassifierMap.get(taskName);
        if (curAssignment != null) {
            clearAssignment(taskName);
        }
        String encoded = threshold != null ? toLdapValue(taskName, classifierLabel, threshold) : toLdapValue(taskName, classifierLabel);
        ldapValueMap.put(taskName, encoded);
        taskClassifierMap.put(taskName,  new TaskConfig(classifierLabel, threshold));
        getLocalServer().addMachineLearningTaskConfig(encoded);
    }

    @Override
    public void clearAssignment(String taskName) throws ServiceException {
        String assignedClassifier = ldapValueMap.get(taskName);
        if (!Strings.isNullOrEmpty(assignedClassifier)) {
            getLocalServer().removeMachineLearningTaskConfig(ldapValueMap.get(taskName));
            taskClassifierMap.remove(taskName);
        }
    }
}