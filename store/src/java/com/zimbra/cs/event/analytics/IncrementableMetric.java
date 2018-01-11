package com.zimbra.cs.event.analytics;


/**
 * Representation of intermediate data necessary to compute an {@link EventMetric}
 */
public interface IncrementableMetric<T, I extends IncrementableMetric.Increment> {

    /**
     * return the value of the metric
     */
    public T getValue();

    /**
     * Increments the internal metric state
     */
    public void increment(I increment);

    public static interface Increment {}
}