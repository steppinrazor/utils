package com.krs.utils.retry.strategy;

import com.krs.utils.retry.RetryException;
import com.krs.utils.retry.RetryUtils;

/**
 * Created: 2014-04-28 15:37<p/>
 * <p>
 * An object that provides custom behavior when operations are retried.<p/>
 * <p>
 * A retry strategy by itself does not describe which failures allow an operation to be retried but rather
 * what should happen between retry attempts. To be useful for retrying operations, a retry strategy is
 * wrapped as a {@link com.krs.utils.retry.RetryPolicy}, which provides domain-specific
 * classification of which failures allow retries.
 *
 * @author Kareem Shabazz
 * @see RetryUtils#buildRetryPolicy(java.util.function.Function, RetryStrategy)
 */
@FunctionalInterface
public interface RetryStrategy {

    /**
     * Implements this strategy's behavior upon each retry. Returning from this method signifies the retry
     * attempt may proceed normally. Throwing a {@link RetryException} from this method signifies this
     * strategy cannot handle the attempted retry (for example, a max retry count has been reached).
     * Otherwise, throwing any other type of exception signifies the retry attempt has failed and should
     * be aborted.
     *
     * @param t the exception which caused the operation to fail.
     * @throws RetryException if the retry attempt cannot be handled by this strategy.
     */
    void beforeRetry(Throwable t) throws RetryException;
}
