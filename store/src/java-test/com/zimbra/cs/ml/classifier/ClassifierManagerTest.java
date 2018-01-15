package com.zimbra.cs.ml.classifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyTimeRange;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.ml.DummyMachineLearningBackend;
import com.zimbra.cs.ml.MachineLearningBackend;
import com.zimbra.cs.ml.classifier.Classifier.ClassifiableType;
import com.zimbra.cs.ml.feature.FeatureParam;
import com.zimbra.cs.ml.feature.FeatureParam.ParamKey;
import com.zimbra.cs.ml.feature.FeatureParams;
import com.zimbra.cs.ml.feature.FeatureSet;
import com.zimbra.cs.ml.feature.FeatureSpec;
import com.zimbra.cs.ml.feature.FeatureSpec.KnownFeature;
import com.zimbra.cs.ml.schema.ClassifierInfo;
import com.zimbra.cs.ml.schema.ClassifierSpec;
import com.zimbra.cs.ml.schema.MessageClassificationInput;
import com.zimbra.cs.ml.schema.TrainingData;
import com.zimbra.cs.ml.schema.TrainingData.TrainingDocument;
import com.zimbra.cs.ml.schema.TrainingSetInfo;
import com.zimbra.cs.ml.schema.TrainingSpec;
import com.zimbra.qa.unittest.TestUtil;

public class ClassifierManagerTest {

    private static final String USER = "classifierManagerTest";

    private static final String[] EXCLUSIVE_CLASSES = new String[] {"exclusive1", "exclusive2"};
    private static final String[] OVERLAPPING_CLASSES = new String[] {"overlapping1", "overlapping2"};

    private ClassifierRegistry registry;
    private ClassifierManager manager;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        TestUtil.createAccount(USER);
        registry = new LdapClassifierRegistry();
        manager = ClassifierManager.getInstance();
        MachineLearningBackend.setFactory(DummyMachineLearningBackend.Factory.class.getName());
    }

    @After
    public void tearDown() throws Exception {
        for (String id: manager.getAllClassifierInfo().keySet()) {
            manager.deleteClassifier(id);
        }
        MailboxTestUtil.clearData();
    }

    private FeatureSet<Message> getFeatureSet() throws ServiceException {
        FeatureSet<Message> fs = new FeatureSet<Message>();
        fs.addFeatureSpec(new FeatureSpec<Message>(KnownFeature.IS_PART_OF_CONVERSATION));
        fs.addFeatureSpec(new FeatureSpec<Message>(KnownFeature.COMBINED_FREQUENCY)
                .addParam(new FeatureParam<>(ParamKey.TIME_RANGE, ContactFrequencyTimeRange.FOREVER)));
        fs.addFeatureSpec(new FeatureSpec<Message>(KnownFeature.EVENT_RATIO)
                .addParam(new FeatureParam<>(ParamKey.NUMERATOR, EventType.READ))
                .addParam(new FeatureParam<>(ParamKey.DENOMINATOR, EventType.SEEN)));
        return fs;
    }

    private ClassifierSpec getSpec() {
        return new ClassifierSpec(8, 52, EXCLUSIVE_CLASSES, OVERLAPPING_CLASSES);
    }

    private MessageClassifier getClassifier(String id, String label) throws ServiceException {
        ClassifierInfo info = new ClassifierInfo(id, 8, 52, EXCLUSIVE_CLASSES, OVERLAPPING_CLASSES, 0, 0);
        return new MessageClassifier(id, label, getFeatureSet(), info);
    }

    @Test
    public void testEncodeClassifier() throws Exception {
        MessageClassifier classifier = getClassifier("id1", "testClassifier");
        String encoded = registry.encode(classifier);
        Classifier<?> decodedClassifier = registry.decode(encoded);
        assertEquals("wrong id in decoded classifier", "id1", decodedClassifier.getId());
        assertEquals("wrong label in decoded classifier", "testClassifier", decodedClassifier.getLabel());
        FeatureSet<Message> fs = classifier.getFeatureSet();
        FeatureSet<?> decodedFeatureSet = decodedClassifier.getFeatureSet();
        assertEquals("wrong number feature specs in decoded classifier", fs.getAllFeatureSpecs().size(), decodedFeatureSet.getAllFeatureSpecs().size());
        for (FeatureSpec<Message> spec: fs.getAllFeatureSpecs()) {
            KnownFeature feature = spec.getFeature();
            FeatureParams params = spec.getParams();
            boolean found = false;
            for(FeatureSpec<?> decodedFeatureSpec: decodedFeatureSet.getFeatureSpecs(feature)) {
                if (decodedFeatureSpec.getParams().equals(params)) {
                    found = true;
                }
            }
            if (!found) {
                fail(spec.toString() + " not found in decoded classifier");
            }
        }
    }

    @Test
    public void testClassifierRegistry() throws Exception {
        String id1 = "id1";
        String id2 = "id2";
        String label1 = "label1";
        String label2 = "label2";
        assertFalse("label1 shouldn't exist in registry", registry.labelExists(label1));
        MessageClassifier classifier1 = getClassifier(id1, label1);
        MessageClassifier sameLabel = getClassifier(id2, label1);
        registry.register(classifier1);
        assertTrue("label1 should exist in registry", registry.labelExists(label1));
        assertSame("should see classifier1 by label", classifier1, registry.getByLabel(label1));
        try {
            registry.register(sameLabel);
            fail("should not be able to register a classifier with the same label");
        } catch (ServiceException e){
            assertEquals("should see FAILURE error", ServiceException.FAILURE, e.getCode());
        }
        MessageClassifier classifier2 = getClassifier(id2, label2);
        registry.register(classifier2);
        assertTrue("label2 should exist in registry", registry.labelExists(label2));
        assertSame("should see classifier2 by label", classifier2, registry.getByLabel(label2));
        assertSame("should see classifier1", classifier1, registry.getById(id1));
        assertSame("should see classifier2", classifier2, registry.getById(id2));

        Map<String, Classifier<?>> classifiers = registry.getAllClassifiers();
        assertEquals("should see 2 classifiers", 2, classifiers.size());
        assertEquals("id1 should map to classifier1", classifier1, classifiers.get(id1));
        assertEquals("id2 should map to classifier2", classifier2, classifiers.get(id2));

        registry.delete(id1);
        assertFalse("label1 should not longer exist", registry.labelExists(label1));
        try {
            registry.getById(id1);
        } catch (ServiceException e){
            assertEquals("should see FAILURE error", ServiceException.FAILURE, e.getCode());
        }
        assertEquals("registry should have one classifier", 1, registry.getAllClassifiers().size());
    }

    @Test
    public void testManageClassifiers() throws Exception {

        Classifier<Message> c1 = manager.registerClassifier(new ClassifierData<Message>(ClassifiableType.MESSAGE, "test1", getSpec(), getFeatureSet()));
        Classifier<Message> c2 = manager.registerClassifier(new ClassifierData<Message>(ClassifiableType.MESSAGE, "test2", getSpec(), getFeatureSet()));

        String id1 = c1.getId();
        String id2 = c2.getId();

        Map<String, Classifier<?>> classifiers = manager.getAllClassifiers();
        assertEquals("should see 2 classifiers", 2, classifiers.size());
        assertTrue("id1 should be in the map", classifiers.containsKey(id1));
        assertTrue("id2 should be in the map", classifiers.containsKey(id2));

        assertEquals("id1 label is test1", "test1", manager.getClassifierById(id1).getLabel());
        assertEquals("id2 label is test2", "test2", manager.getClassifierById(id2).getLabel());

        manager.deleteClassifier(id1);
        classifiers = manager.getAllClassifiers();
        assertEquals("should see 1 classifier", 1, classifiers.size());
        assertTrue("id2 should be in the map", classifiers.containsKey(id2));
    }

    @Test
    public void testTrainClassifier() throws Exception {

        Classifier<Message> classifier = manager.registerClassifier(new ClassifierData<Message>(ClassifiableType.MESSAGE, "test1", getSpec(), getFeatureSet()));

        TrainingData data = new TrainingData();
        MessageClassificationInput input1 = new MessageClassificationInput().setText("foo");
        MessageClassificationInput input2 = new MessageClassificationInput().setText("bar");
        data.addDoc(new TrainingDocument(input1, EXCLUSIVE_CLASSES[0], OVERLAPPING_CLASSES));
        data.addDoc(new TrainingDocument(input2, EXCLUSIVE_CLASSES[1], OVERLAPPING_CLASSES));
        TrainingSpec spec = new TrainingSpec(data);
        spec.setPercentHoldout((float)0.5);
        spec.setEpochs(10);
        spec.setLearningRate((float) 0.5);
        spec.setPersist(true);
        ClassifierInfo info = classifier.train(spec);
        TrainingSetInfo trainingSetInfo = info.getTrainingSet();
        assertEquals("epoch from dummy handler should be 0", 0, info.getEpoch());
        assertEquals("should have 1 training doc", 1, trainingSetInfo.getNumTrain());
        assertEquals("should have 1 test doc", 1, trainingSetInfo.getNumTest());
    }
}
