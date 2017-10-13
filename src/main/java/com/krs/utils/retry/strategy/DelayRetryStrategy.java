package com.krs.utils.retry.strategy;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.krs.utils.retry.RetryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created: 2014-04-28 11:46<p/>
 * <p>
 * A strategy that sleeps a specific amount of time between subsequent retries, up to a maximum number
 * of retries.<p/>
 * <p>
 * This class is thread safe.
 *
 * @author Kareem Shabazz
 */
@ThreadSafe
public final class DelayRetryStrategy implements RetryStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(DelayRetryStrategy.class);

    private final long sleepMillis;
    private final int maxAttempts;
    private final AtomicInteger retryCount = new AtomicInteger(0);

    /**
     * Constructs a strategy that sleeps a specific amount of time between retry attempts, up to a
     * maximum number of attempts.
     *
     * @param time        amount of time to sleep between retries.
     * @param unit        the unit of sleep time.
     * @param maxAttempts the maximum number of retries to attempt.
     * @throws IllegalArgumentException if maxAttempts or sleepTime is negative.
     */
    public DelayRetryStrategy(
            long time,
            TimeUnit unit,
            int maxAttempts) {

        Preconditions.checkArgument(maxAttempts > 0, "maxAttempts [%s] cannot be negative.", maxAttempts);
        Preconditions.checkArgument(time > 0, "sleepTime [%s] cannot be negative.", time);

        this.sleepMillis = unit.toMillis(time);
        this.maxAttempts = maxAttempts;
    }

    /**
     * Constructs a strategy that sleeps ten seconds between retry attempts for an unlimited number
     * of attempts.
     */
    public DelayRetryStrategy() {
        this(10, TimeUnit.SECONDS, Integer.MAX_VALUE);
    }

    /**
     * Invoked immediately before the thread is put to sleep. This implementation logs the retry as
     * an error.
     *
     * @param t           the exception which caused the retry attempt.
     * @param retryCount  the number of attempts that have occurred.
     * @param sleepMillis the number of milliseconds to sleep.
     */
    void beforeSleep(Throwable t, long retryCount, long sleepMillis) {
        LOGGER.error("Waiting {} ms before retry attempt #{}", sleepMillis, retryCount, t);
    }

    @Override
    public void beforeRetry(Throwable t) throws RetryException {
        long i = retryCount.incrementAndGet();
        if (i > maxAttempts) {
            throw new RetryException(t);
        }

        beforeSleep(t, i, sleepMillis);

        try {
            TimeUnit.MILLISECONDS.sleep(sleepMillis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            // If we're interrupted, presumably someone wants to cancel this and handle the retry in
            // their own way.
            throw new RetryException(ie);
        }
    }
}
