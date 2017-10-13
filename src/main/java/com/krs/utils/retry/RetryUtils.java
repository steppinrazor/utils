package com.krs.utils.retry;

import com.google.common.base.Preconditions;
import com.krs.utils.retry.strategy.RetryStrategy;

import java.util.function.Function;

/**
 * Created: 2014-04-28 09:14<p/>
 * <p>
 * Factory and utility methods for {@link RetryPolicy}.
 *
 * @author Kareem Shabazz
 */

public final class RetryUtils {

    private RetryUtils() {
    }

    /**
     * Returns a retry policy that delegates to the provided callback to determine if a failure allows an
     * operation to be retried, with the specified strategy providing the actual retry behavior.<p/>
     * <p>
     * The callback should return true if an exception allows for the operation to be retried, false
     * otherwise.
     *
     * @param canRetryCallback a function that provides logic for {@link RetryPolicy#canRetry(Throwable)}.
     * @param strategy         the strategy that provides logic for {@link RetryPolicy#beforeRetry(Throwable)}.
     * @return a retry policy wrapping the callback and strategy.
     * @throws IllegalArgumentException if any argument is null.
     */
    public static RetryPolicy buildRetryPolicy(
            final Function<? super Throwable, Boolean> canRetryCallback,
            final RetryStrategy strategy) {

        Preconditions.checkNotNull(canRetryCallback);
        Preconditions.checkNotNull(strategy);

        return new RetryPolicy() {

            @Override
            public boolean canRetry(Throwable t) {
                return
                        canRetryCallback.apply(t) ||
                                t instanceof RetryException;  // RetryException means some earlier policy deferred to us.
            }

            @Override
            public void beforeRetry(Throwable t) throws RetryException {
                strategy.beforeRetry(t);
            }
        };
    }
}
