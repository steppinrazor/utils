package com.krs.utils.retry;

/**
 * Describes a policy for retrying operations.<p/>
 * <p>
 * The {@link #canRetry(Throwable)} method identifies whether or not an exception allows for the
 * operation to be retried. The {@link #beforeRetry(Throwable)} method implements behavior required by
 * the policy upon each retry.<p/>
 * <p>
 * Code that uses a retry policy must abide by the following contract:
 * <ol>
 * <li>The retry policy is first asked if an exception {@code e} can be retried by invoking
 * the {@link #canRetry(Throwable)} method.</li>
 * <li>Only if that method returns true will {@link #beforeRetry(Throwable)} be invoked,
 * passing in {@code e}.</li>
 * </ol>
 * <p>
 * For example:
 * <pre><tt>
 *      while (true) {
 *          try {
 *              return someOperation();
 *          }
 *          catch (Exception e) {
 *              if (policy.canRetry(e)) {
 *                  policy.beforeRetry(e);
 *                  continue;
 *              }
 *              throw e;
 *          }
 *      }
 * </tt></pre>
 * <p>
 * Normally, a RetryPolicy wraps a {@link com.krs.utils.retry.strategy.RetryStrategy} providing
 * the logic for {@link #beforeRetry(Throwable)}, with the wrapper providing the domain-specific logic
 * for {@link #canRetry(Throwable)}.
 *
 * @author Kareem Shabazz
 * @see com.krs.utils.retry.strategy.RetryStrategy
 * @see RetryUtils#buildRetryPolicy
 */
public interface RetryPolicy {

    /**
     * Returns true if the specified exception allows for the operation to be retried.
     *
     * @param t the exception which caused the operation to fail.
     * @return true if the exception allows for the operation to be retried.
     */
    boolean canRetry(@SuppressWarnings("SameParameterValue") Throwable t);

    /**
     * Implements this policy's behavior upon each retry. This method is invoked if and only if
     * {@link #canRetry(Throwable)} returns true. A normal return from this method signifies the
     * failed operation should be retried. Throwing a RetryException from this method signifies this
     * policy cannot handle the attempted retry (for example, a max retry count has been reached).
     * Otherwise, throwing any other exception signifies the failure prohibits retrying the operation.
     *
     * @param t the exception which caused the operation to fail.
     * @throws RetryException if the retry attempt cannot be handled by this strategy.
     */
    void beforeRetry(Throwable t) throws RetryException;

    RetryPolicy NO_RETRY = new RetryPolicy() {

        @Override
        public boolean canRetry(Throwable t) {
            return false;
        }

        @Override
        public void beforeRetry(Throwable t) throws RetryException {
            //do nothing
        }
    };
}

