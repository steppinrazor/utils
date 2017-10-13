package com.krs.utils.retry.strategy;

import com.google.common.base.Preconditions;
import com.krs.utils.retry.RetryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by IntelliJ IDEA.
 * Date: Mar 6, 2014
 * Time: 4:04:20 PM<p/>
 * <p>
 * A strategy that sleeps an exponentially increasing amount of time between subsequent retries, starting
 * at a minimum duration and increasing the sleep time after each retry up to a maximum duration.
 *
 * @author Kareem Shabazz
 */
@ThreadSafe
public final class ExponentialDelayRetryStrategy implements RetryStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExponentialDelayRetryStrategy.class);

    private final long minWaitMillis;
    private final long maxWaitMillis;
    private final int maxAttempts;
    private final AtomicLong retryCount = new AtomicLong(0);

    /**
     * Create an exponential retry strategy that sleeps {@code minTime} on the first retry and increases
     * exponentially the amount of sleep time on each subsequent retry, up to a maximum of {@code maxTime},
     * up to a maximum number of attempts.
     *
     * @param maxUnit     unit for max time.
     * @param maxTime     the amount of time to max sleep (defaults to 5 minutes).
     * @param maxAttempts the maximum number of retries to attempt (defaults to {@link Integer.MAX_VALUE}).
     * @throws IllegalArgumentException if the min time is negative or greater than the max time,
     *                                  or max attempts is negative.
     */
    public ExponentialDelayRetryStrategy(
            TimeUnit maxUnit,
            int maxTime,
            int maxAttempts) {

        this.minWaitMillis = 5l;
        this.maxWaitMillis = TimeUnit.MILLISECONDS.convert((long) maxTime, maxUnit);
        this.maxAttempts = maxAttempts;

        Preconditions.checkArgument(maxAttempts > 0, "maxAttempts [%s] cannot be negative.", maxAttempts);
        Preconditions.checkArgument(maxWaitMillis > 0, "min [%s] time cannot be negative.", minWaitMillis);
        Preconditions.checkArgument(maxWaitMillis > minWaitMillis, "min [%s] time cannot be greater than max [%s] time", minWaitMillis, maxWaitMillis);
    }

    /**
     * Create an exponential retry strategy that sleeps five milliseconds on the first retry and increases
     * exponentially the amount of sleep time on each subsequent retry, up to a maximum of five minutes,
     * for an unlimited number of attempts.
     */
    public ExponentialDelayRetryStrategy() {
        this(TimeUnit.MINUTES, 5, Integer.MAX_VALUE);
    }

    /**
     * Invoked immediately before the thread is put to sleep. This implementation logs the retry as
     * an warn.
     *
     * @param t           the exception which caused the retry attempt.
     * @param retryCount  the number of attempts that have occurred.
     * @param sleepMillis the number of milliseconds to sleep.
     */
    void beforeSleep(Throwable t, long retryCount, long sleepMillis) {
        LOGGER.warn("Waiting {} ms before retry attempt #{}", sleepMillis, retryCount, t);
    }

    /**
     * Causes the current thread to sleep an exponentially increasing amount of time upon each invocation.
     * If the thread is interrupted, a {@link com.krs.utils.retry.RetryException} is thrown and the retry logic is deferred to
     * the handler of the exception, if any.
     *
     * @param t the exception which caused the retry attempt.
     */
    @Override
    public void beforeRetry(Throwable t) throws RetryException {
        long i = retryCount.incrementAndGet();
        if (i > maxAttempts) {
            throw new RetryException(t);
        }

        long sleepMillis = (long) Math.min(minWaitMillis * Math.exp(i - 1), maxWaitMillis);

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
