package com.zimbra.cs.ml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.UUIDUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.ml.query.CreateClassifierQuery;
import com.zimbra.cs.ml.query.DeleteClassifierQuery;
import com.zimbra.cs.ml.query.GetClassifierQuery;
import com.zimbra.cs.ml.query.ListClassifiersQuery;
import com.zimbra.cs.ml.query.MessageClassificationQuery;
import com.zimbra.cs.ml.query.TrainClassifierQuery;
import com.zimbra.cs.ml.schema.ClassificationResult;
import com.zimbra.cs.ml.schema.ClassifierInfo;
import com.zimbra.cs.ml.schema.ClassifierSpec;
import com.zimbra.cs.ml.schema.MessageClassificationInput;
import com.zimbra.cs.ml.schema.OverlappingClassification;
import com.zimbra.cs.ml.schema.TrainingData;
import com.zimbra.cs.ml.schema.TrainingData.TrainingDocument;
import com.zimbra.cs.ml.schema.TrainingSetInfo;
import com.zimbra.cs.ml.schema.TrainingSpec;

/**
 * Dummy MachineLearningBackend that returns mock MLQuery responses and stores ClassifierInfo in an in-memory map
 */
public class DummyMachineLearningBackend extends MachineLearningBackend {

    private Map<String, ClassifierInfo> knownClassifiers;

    public DummyMachineLearningBackend() {
        knownClassifiers = new HashMap<>();
    }

    private void addKnownClassifier(ClassifierInfo info) {
        ZimbraLog.ml.debug("Dummy ML backend is registering classifier with id=%s", info.getClassifierId());
        knownClassifiers.put(info.getClassifierId(), info);
    }

    @Override
    protected QueryCallback<CreateClassifierQuery, ClassifierInfo> getCreateClassifierCallback() {
        return new QueryCallback<CreateClassifierQuery, ClassifierInfo>() {

            @Override
            public ClassifierInfo run(CreateClassifierQuery query) {
                ClassifierSpec spec = query.getClassifierSpec();
                String id = spec.getClassifierId() == null ? UUIDUtil.generateUUID() : spec.getClassifierId();
                ClassifierInfo info = ClassifierInfo.fromSpec(id, spec);
                addKnownClassifier(info);
                return info;
            }
        };
    }

    @Override
    protected QueryCallback<DeleteClassifierQuery, Boolean> getDeleteClassifierCallback() {
        return new QueryCallback<DeleteClassifierQuery, Boolean>() {

            @Override
            public Boolean run(DeleteClassifierQuery query) {
                String idToDelete = query.getClassifierId();
                ClassifierInfo info = knownClassifiers.remove(idToDelete);
                return info != null;
            }
        };
    }

    @Override
    protected QueryCallback<ListClassifiersQuery, List<ClassifierInfo>> getListClassifiersCallback() {
        return new QueryCallback<ListClassifiersQuery, List<ClassifierInfo>>() {

            @Override
            public List<ClassifierInfo> run(ListClassifiersQuery query) {
                return new ArrayList<ClassifierInfo>(knownClassifiers.values());
            }
        };
    }

    @Override
    protected QueryCallback<TrainClassifierQuery, ClassifierInfo> getTrainClassifierCallback() {
        return new QueryCallback<TrainClassifierQuery, ClassifierInfo>() {

            @Override
            public ClassifierInfo run(TrainClassifierQuery query) throws ServiceException {
                TrainingSpec spec = query.getTrainingSpec();
                String classifierId = spec.getClassifierId();
                ClassifierInfo info = knownClassifiers.get(classifierId);
                if (info == null) {
                    throw ServiceException.INVALID_REQUEST("no classifier exists with id " + classifierId, null);
                }
                TrainingData td = spec.getTrainingData();
                if (spec.isPersist()) {
                    float holdout = spec.getPercentHoldout();
                    List<TrainingDocument> trainingDocs = td.getTrainingDocs();
                    int numTest = Math.round(trainingDocs.size() * holdout);
                    int numTrain = trainingDocs.size() - numTest;
                    TrainingSetInfo trainingSetInfo = new TrainingSetInfo(new Date().toString(), numTrain, numTest);
                    info.setTrainingSet(trainingSetInfo);
                }
                info.setEpoch(0);
                return info;
            }
        };
    }

    @Override
    protected QueryCallback<MessageClassificationQuery, ClassificationResult> getClassificationCallback() {
        return new QueryCallback<MessageClassificationQuery, ClassificationResult>() {

            @Override
            public ClassificationResult run(MessageClassificationQuery query) throws ServiceException {
                String classifierId = query.getClassifierId();
                ClassifierInfo info = knownClassifiers.get(classifierId);
                if (info == null) {
                    throw ServiceException.INVALID_REQUEST("no classifier exists with id " + classifierId, null);
                }
                MessageClassificationInput input = query.getInput();
                ClassificationResult classification = new ClassificationResult();
                classification.setUrl(input.getUrl());
                String subject;
                try {
                    subject = Mime.getSubject(input.getMimeMessage());
                } catch (MessagingException e) {
                    ZimbraLog.ml.error("error getting email subject in DummyMachineLearningBackend", e);
                    return classification;
                }
                if (subject != null) {
                    if (info.getExclusiveClasses() != null && info.getExclusiveClasses().length > 0) {
                        for (String classLabel: info.getExclusiveClasses()) {
                            if (subject.contains(classLabel.toLowerCase())) {
                                classification.setExclusiveClass(classLabel);
                                break;
                            }
                        }
                    }
                    if (info.getOverlappingClasses() != null && info.getOverlappingClasses().length > 0) {
                        List<OverlappingClassification> oc;
                        oc = Arrays.asList(info.getOverlappingClasses()).stream()
                                .map(c -> new OverlappingClassification(c, subject.contains(c.toLowerCase()) ? 1f : 0f))
                                .collect(Collectors.toList());
                        classification.setOverlappingClasses(oc.toArray(new OverlappingClassification[oc.size()]));
                    }
                }
                return classification;
            }
        };
    }

    @Override
    protected QueryCallback<GetClassifierQuery, ClassifierInfo> getGetClassifierCallback() {
        return new QueryCallback<GetClassifierQuery, ClassifierInfo>() {

            @Override
            public ClassifierInfo run(GetClassifierQuery query)
                    throws ServiceException {
                String classifierId = query.getClassifierId();
                ClassifierInfo info = knownClassifiers.get(classifierId);
                if (info == null) {
                    throw ServiceException.INVALID_REQUEST("no classifier exists with id " + classifierId, null);
                } else {
                    return info;
                }
            }
        };
    }

    public static class Factory implements MachineLearningBackend.Factory {

        private DummyMachineLearningBackend instance;

        @Override
        public MachineLearningBackend getMachineLearningBackend() throws ServiceException {
            if (instance == null) {
                instance = new DummyMachineLearningBackend();
            }
            return instance;
        }
    }
}
