package com.krs.utils.threads;

import com.google.common.base.Preconditions;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;

/**
 * Created by IntelliJ IDEA.
 * Date: Aug 26, 2014
 * Time: 7:39:09 PM<p/>
 *
 * @author krs
 */

public final class ThreadUtils {
    private ThreadUtils() {
    }

    /**
     * Returns an {@code UncaughtExceptionHandler} that logs uncaught throwables, and their stack
     * traces, as errors.
     *
     * @see Thread.UncaughtExceptionHandler
     */
    public static Thread.UncaughtExceptionHandler exceptionLogger() {
        return ExceptionLogger.INSTANCE;
    }

    /**
     * Returns a {@code Runnable} that wraps the specified {@code Runnable} with an exception handler
     * that catches {@code Throwable} and logs it as an error.
     *
     * @param inner the {@code Runnable} to guard
     */
    public static Runnable guard(Runnable inner) {
        return guard(inner, exceptionLogger());
    }

    /**
     * Returns a {@code Runnable} that wraps the specified {@code Runnable} with an exception handler
     * that catches {@code Throwable} and invokes the specified handler.
     *
     * @param inner the {@code Runnable} to guard
     * @param handler the {@code UncaughtExceptionHandler} to invoke when {@code Throwable} is caught.
     */
    public static Runnable guard(Runnable inner, Thread.UncaughtExceptionHandler handler) {
        return new RunnableGuard(inner, handler);
    }

    /**
     * Returns a {@code Callable} that wraps the specified {@code Callable} with an exception handler
     * that catches {@code Throwable} and logs it as an error. The resulting {@code Callable} returns
     * {@code null} if an exception is handled.
     *
     * @param inner the {@code Callable} to guard
     */
    public static <V> Callable<Optional<V>> guard(Callable<V> inner) {
        return guard(inner, null);
    }

    /**
     * Returns a {@code Callable} that wraps the specified {@code Callable} with an exception handler
     * that catches {@code Throwable} and logs it as an error. The resulting {@code Callable} returns the
     * provided default result after the exception is handled.
     *
     * @param inner the {@code Callable} to guard
     * @param defaultResult the result to return after an exception is handled.
     */
    public static <V> Callable<Optional<V>> guard(Callable<V> inner, V defaultResult) {
        return guard(inner, defaultResult, exceptionLogger());
    }

    /**
     * Returns a {@code Callable} that wraps the specified {@code Callable} with an exception handler
     * that catches {@code Throwable} and invokes the specified handler. The resulting {@code Callable}
     * returns the provided default result after the exception is handled.
     *
     * @param inner the {@code Callable} to guard
     * @param defaultResult the result to return after an exception is handled.
     * @param handler the {@code UncaughtExceptionHandler} to invoke when {@code Throwable} is caught.
     */
    public static <V> Callable<Optional<V>> guard(
            Callable<V> inner,
            V defaultResult,
            Thread.UncaughtExceptionHandler handler) {

        return new CallableGuard<V>(inner, defaultResult, handler);
    }

    /**
     * Returns a thread factory that produces named daemon threads.
     *
     * @param nameFormat the format string used to name new Threads, in the form expected by
     *                   String.format(). The format string is passed the thread ID as an integer substitution
     *                   during Thread construction. E.g.: "Worker %d"
     * @return the desired thread factory
     * @see NamedThreadFactory
     * @see DaemonThreadFactory
     */
    public static ThreadFactory newNamedDaemonThreadFactory(String nameFormat) {
        return new DaemonThreadFactory(new NamedThreadFactory(nameFormat));
    }

    /**
     * A class that wraps invocation of a {@code Runnable} with an exception handler that catches
     * {@code Throwable} and invokes an {@code UncaughtExceptionHandler}.
     *
     * @see Thread.UncaughtExceptionHandler
     */
    private static final class RunnableGuard implements Runnable {
        private final Runnable inner;
        private final Thread.UncaughtExceptionHandler handler;

        private RunnableGuard(Runnable inner, Thread.UncaughtExceptionHandler handler) {
            this.inner = Preconditions.checkNotNull(inner);
            this.handler = Preconditions.checkNotNull(handler);
        }

        public void run() {
            try {
                inner.run();
            } catch (Throwable t) {
                handler.uncaughtException(Thread.currentThread(), t);
            }
        }
    }

    /**
     * A class that wraps invocation of a {@code Callable} with an exception handler that catches
     * {@code Throwable} and invokes an {@code UncaughtExceptionHandler}. A predefined default
     * result is returned if an exception is handled.
     *
     * @see Thread.UncaughtExceptionHandler
     */
    private static final class CallableGuard<V> implements Callable<Optional<V>> {
        private final Callable<V> inner;
        private final V defaultResult;
        private final Thread.UncaughtExceptionHandler handler;

        public CallableGuard(Callable<V> inner, V defaultResult, Thread.UncaughtExceptionHandler handler) {
            this.inner = Preconditions.checkNotNull(inner);
            this.handler = Preconditions.checkNotNull(handler);
            this.defaultResult = defaultResult;
        }

        @Override
        public Optional<V> call() throws Exception {
            try {
                return Optional.ofNullable(inner.call());
            } catch (Throwable t) {
                handler.uncaughtException(Thread.currentThread(), t);
                return Optional.ofNullable(defaultResult);
            }
        }
    }
}
