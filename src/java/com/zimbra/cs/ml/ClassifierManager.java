package com.zimbra.cs.ml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.mahout.classifier.sgd.CrossFoldLearner;
import org.apache.mahout.classifier.sgd.ModelSerializer;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.analytics.BehaviorManager;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;

/** Class for executing high-level operations on a classifier, such as training, loading, saving, updating training data, etc
 *
 * @author iraykin
 *
 */
public class ClassifierManager {
	private static final float DEFAULT_PERCENT_TEST = new Float(0.2);
	private static final float DEFAULT_MIN_ACCURACY = new Float(0.9);
	private static final int DEFAULT_BEHAVIOR_HORIZON_DAYS = 10000;
	private static Map <Integer, Classifier> cache = new HashMap<Integer, Classifier>(); //replace with guava cache

	public static MahoutClassifier load(Mailbox mbox) throws ServiceException {
		String path = getPath(mbox);
		try {
			InputStream in = new FileInputStream(path);
			CrossFoldLearner learner = ModelSerializer.readBinary(in, CrossFoldLearner.class);
			ZimbraLog.analytics.info(String.format("loaded classifier for mailbox %s from %s", mbox.getId(), path));
			return new MahoutClassifier(learner);
		} catch (IOException e) {
			throw ServiceException.UNKNOWN_DOCUMENT("no such file", e);
		}
	}

	public static void save(MahoutClassifier classifier, Mailbox mbox) throws ServiceException {
		String path = getPath(mbox);
		try {
			ModelSerializer.writeBinary(path, classifier.getLearner());
			ZimbraLog.analytics.info(String.format("saved classifier for mailbox %s to %s", mbox.getId(), path));
		} catch (IOException e) {
			throw ServiceException.FAILURE("failed to serialize model", e);
		}
	}

	private static String getPath(Mailbox mbox) throws ServiceException {
		return "/opt/zimbra/ml/Classifier_" + String.valueOf(mbox.getId());
	}

	public static Classifier getClassifier(Mailbox mbox) {
		if (cache.containsKey(mbox.getId())) {
			return cache.get(mbox.getId());
		} else {
			Classifier cls;
			try {
				cls = load(mbox);
				cache.put(mbox.getId(), cls);
			} catch (ServiceException e) {
				/* don't cache dummy classifier, because we want want
				   to load the real one as soon as it becomes available */
				cls = new DummyClassifier();
			}
			return cls;
		}
	}

	public static void updateCache(MahoutClassifier classifier, Mailbox mbox) {
		cache.put(mbox.getId(), classifier);
	}

	public static void trainAndSave(Mailbox mbox) throws ServiceException {
		trainAndSave(mbox, DEFAULT_PERCENT_TEST, DEFAULT_MIN_ACCURACY);
	}

	public static void trainAndSave(Mailbox mbox, float percentTest, float minAccuracy) throws ServiceException {
		OperationContext octxt = new OperationContext(mbox.getAccount());
		ModelTrainer trainer = new ModelTrainer(percentTest);
		trainer.setMinAccuracy(minAccuracy);
		TrainingSet trainingSet = new TrainingSet(mbox);
		FeatureExtractor extractor = FeatureExtractor.withAllFeatures();
		Pair<MahoutClassifier, CrossValidation> results = trainer.trainFull(trainingSet, extractor, DbPool.getConnection(), octxt);
		if (results == null) {
			return;
		}
		MahoutClassifier classifier = results.getFirst();
		save(classifier, mbox);
		updateCache(classifier, mbox);
		CrossValidation validation = results.getSecond();
		validation.output(System.out);
	}

	public static void updateTrainingSetFromBehaviors(Mailbox mbox) throws ServiceException {
		updateTrainingSetFromBehaviors(mbox, DEFAULT_BEHAVIOR_HORIZON_DAYS, TimeUnit.DAYS);
	}

	public static void updateTrainingSetFromBehaviors(Mailbox mbox, Integer behaviorHorizon, TimeUnit horizonTimeUnit) throws ServiceException {
		TrainingSetUpdate updater = new TrainingSetUpdate(mbox, behaviorHorizon, horizonTimeUnit);
		BehaviorClassifier classifier = TrainingSetUpdate.getStandardClassifier();
		BehaviorManager manager = BehaviorManager.getFactory().getBehaviorManager();
		updater.update(manager, classifier, DbPool.getConnection());
	}

	public static void clearTrainingData(Mailbox mbox, InternalLabel label) throws ServiceException {
		DbConnection conn = DbPool.getConnection();
		if (label == null) {
			for (InternalLabel l: InternalLabel.values()) {
				mbox.getTrainingSet().deleteAllByLabel(l, conn);
			}
		} else {
			mbox.getTrainingSet().deleteAllByLabel(label, conn);
		}
	}

	public static void updateTrainingSetAndRetrain(Mailbox mbox, Integer behaviorHorizon, TimeUnit horizonTimeUnit, Integer percentTest, float minAccuracy) throws ServiceException {
		updateTrainingSetFromBehaviors(mbox, behaviorHorizon, horizonTimeUnit);
		trainAndSave(mbox, percentTest, minAccuracy);
	}
}
