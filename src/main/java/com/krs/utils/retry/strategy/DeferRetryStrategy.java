package com.krs.utils.retry.strategy;


import com.krs.utils.retry.RetryException;

/**
 * Created by IntelliJ IDEA.
 * Date: Apr 29, 2014
 * Time: 11:33:39 AM<p/>
 * <p>
 * A strategy that throws a {@link RetryException} when a retry is attempted. This strategy defers
 * retry behavior to a handler higher up on the stack, if any. Its primary purpose is to map retryable
 * exceptions into instances of {@link RetryException} to communicate to other handlers higher up on
 * the stack, if they exist, that a retry is possible.
 *
 * @author Kareem Shabazz
 */
public final class DeferRetryStrategy implements RetryStrategy {

    @Override
    public void beforeRetry(Throwable t) throws RetryException {
        throw t instanceof RetryException ? (RetryException) t : new RetryException(t);
    }
}
