package com.zimbra.cs.ml.feature;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.analytics.AccountEventMetrics;
import com.zimbra.cs.event.analytics.AccountEventMetrics.MetricKey;
import com.zimbra.cs.event.analytics.EventMetric.MetricInitializer;
import com.zimbra.cs.event.analytics.EventMetric.MetricParams;
import com.zimbra.cs.event.analytics.EventMetric.MetricType;
import com.zimbra.cs.event.analytics.EventMetricManager;
import com.zimbra.cs.event.analytics.IncrementableMetric;
import com.zimbra.cs.event.analytics.IncrementableMetric.Increment;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.ml.feature.FeatureParam.ParamKey;
import com.zimbra.cs.ml.feature.FeatureSpec.KnownFeature;

/**
 * Classifier feature based off an {@link EventMetric}
 */
public class MetricFeature<T extends IncrementableMetric<S, I>, S, I extends Increment> extends Feature<S> {

    private String accountId;
    private MetricType metricType;
    private MetricParams<T, S, I> metricParams;

    public MetricFeature(KnownFeature featureType, String accountId, MetricType metricType, MetricParams<T, S, I> metricParams) {
        super(featureType);
        this.accountId = accountId;
        this.metricType = metricType;
        this.metricParams = metricParams;
    }

    @Override
    public S getFeatureValue() throws ServiceException {
        AccountEventMetrics metrics = EventMetricManager.getInstance().getMetrics(accountId);
        MetricKey<T, S, I> key = new MetricKey<T, S, I>(metricType, metricParams);
        return metrics.getMetric(key).getValue();
    }

    /**
     * Subclasses of this abstract factory return MetricFeature instances
     */
    public static abstract class Factory<M extends IncrementableMetric<S, I>, S, I extends Increment> extends FeatureFactory<Message, S> {

        protected MetricInitializer<M, S, I> initializer = null;

        @Override
        public void setParams(FeatureParams params) throws ServiceException {
            //check if a custom initializer is set
            MetricInitializer<M, S, I> initializer = params.get(ParamKey.METRIC_INITIALIZER, (MetricInitializer<M, S, I>) null);
            if (initializer != null) {
                setInitializer(initializer);
            }
            setMetricParams(params);
        }

        protected abstract void setMetricParams(FeatureParams params) throws ServiceException;

        /**
         * Override the default EventMetric initializer with a custom one
         */
        @VisibleForTesting
        public void setInitializer(MetricInitializer<M, S, I> initializer) {
            this.initializer = initializer;
        }
    }
}