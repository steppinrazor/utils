package com.krs.utils.retry;


import com.krs.utils.retry.strategy.DeferRetryStrategy;
import com.krs.utils.retry.strategy.DelayRetryStrategy;
import com.krs.utils.retry.strategy.ExponentialDelayRetryStrategy;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static com.krs.utils.retry.RetryPolicy.NO_RETRY;
import static com.krs.utils.retry.RetryUtils.buildRetryPolicy;
import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * Created: 2014-04-28 14:05<p/>
 *
 * @author krs
 */

public class RetryUtilsTest {
    public static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException();

    @Test(timeOut = 1000,
            description = "Test that the exponential policy slowly backs off of successive retries. " +
                    "The task always throws a retryable failure. We interrupt the thread sleeping in the exponential " +
                    "policy after a short amount of time, and then count how many times the policy retried the task.")
    public void testDefaultExponentialRetryPolicy() {
        RetryPolicy policy;
        int maxRetries = 5;
        int[] retriesCounter = {0};
        long[] expIncreasingRetryPeriods = new long[maxRetries];

        policy = buildRetryPolicy(
                (Throwable t) -> {
                    retriesCounter[0]++;
                    return true;
                },
                new ExponentialDelayRetryStrategy());

        for (int i = 0; i < maxRetries; i++) {
            assertThat(policy.canRetry(RUNTIME_EXCEPTION)).isTrue();

            expIncreasingRetryPeriods[i] = currentTimeMillis();
            policy.beforeRetry(RUNTIME_EXCEPTION);
            expIncreasingRetryPeriods[i] = currentTimeMillis() - expIncreasingRetryPeriods[i];
        }

        for (int i = 1; i < maxRetries; i++) {
            assertThat((double) expIncreasingRetryPeriods[i] / expIncreasingRetryPeriods[i - 1]).isGreaterThan(1.0); //test exponential increase in delays
        }
        assertThat(maxRetries).isEqualTo(retriesCounter[0]);
    }

    @Test(timeOut = 1000, expectedExceptions = RetryException.class, expectedExceptionsMessageRegExp = "(?i).*max retries.*",
            description = "Test that the exponential policy slowly backs off of successive retries. " +
                    "The task always throws a retryable failure. We interrupt the thread sleeping in the exponential " +
                    "policy after a short amount of time, and then count how many times the policy retried the task.")
    public void testCustomExponentialRetryPolicy() {
        RetryPolicy policy;
        int[] retriesCount = {0};
        int maxRetries = 3;

        policy = buildRetryPolicy(
                (Throwable t) -> {
                    retriesCount[0]++;
                    return true;
                },
                new ExponentialDelayRetryStrategy(TimeUnit.SECONDS, 5, maxRetries));

        for (int i = 0; i < maxRetries; i++) {
            assertThat(policy.canRetry(RUNTIME_EXCEPTION)).isTrue();
            policy.beforeRetry(RUNTIME_EXCEPTION);
        }

        assertThat(maxRetries).isEqualTo(retriesCount[0]);

        policy.beforeRetry(RUNTIME_EXCEPTION);
    }

    @Test(expectedExceptions = RetryException.class, expectedExceptionsMessageRegExp = "(?i).*max retries.*",
            description = "Test that deferring retry policy doesn't do any retrying at all but simply propagates " +
                    "the failure in a retry exception.")
    public void testDeferringRetryPolicy() {
        RetryPolicy policy;
        int[] retriesCount = {0};

        policy = buildRetryPolicy(
                (Throwable t) -> {
                    retriesCount[0]++;
                    return true;
                },
                new DeferRetryStrategy());

        assertThat(policy.canRetry(RUNTIME_EXCEPTION)).isTrue();
        assertThat(1).isEqualTo(retriesCount[0]);

        policy.beforeRetry(RUNTIME_EXCEPTION);
    }

    @Test(timeOut = 1000, expectedExceptions = RetryException.class, expectedExceptionsMessageRegExp = "(?i).*max retries.*",
            description = "Test that the simple retry policy waits a set amount of time.")
    public void testSimpleRetryPolicy() {
        RetryPolicy policy;
        final int[] retryCount = {0};
        int maxRetries = 2;

        policy = buildRetryPolicy(
                (Throwable t) -> {
                    retryCount[0]++;
                    return true;
                },
                new DelayRetryStrategy(250, TimeUnit.MILLISECONDS, 2));

        for (int i = 0; i < maxRetries; i++) {
            assertThat(policy.canRetry(RUNTIME_EXCEPTION)).isTrue();
            policy.beforeRetry(RUNTIME_EXCEPTION);
        }
        assertThat(maxRetries).isEqualTo(retryCount[0]);

        policy.beforeRetry(RUNTIME_EXCEPTION);
    }

    @Test
    public void testNoRetryPolicy() {
        assertThat(NO_RETRY.canRetry(RUNTIME_EXCEPTION)).isFalse();
    }
}
