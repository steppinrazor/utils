package com.krs.utils.retry;

/**
 * Created by IntelliJ IDEA.
 * Date: Mar 25, 2014
 * Time: 5:07:05 PM<p/>
 * <p>
 * Exception thrown when a retry attempt cannot be performed by a {@link RetryPolicy} but should instead be
 * processed by a handler higher up on the stack, if any. It is similar in concept to {@link InterruptedException}
 * and {@link java.util.concurrent.TimeoutException}.
 *
 * @author krs
 */
public class RetryException extends RuntimeException {

    public RetryException() {
    }

    public RetryException(String message) {
        super(message);
    }

    public RetryException(Throwable cause) {
        super("Max retries reached.", cause);
    }
}
