package com.krs.utils.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * Date: Aug 27, 2014
 * Time: 4:12:30 PM<p/>
 *
 * An exception handler that logs throwables, and their callstacks, as errors.
 *
 * @author krs
 */

final class ExceptionLogger implements Thread.UncaughtExceptionHandler {
    static final ExceptionLogger INSTANCE = new ExceptionLogger();
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionLogger.class);

    private ExceptionLogger() {
    }

    public void uncaughtException(Thread t, Throwable e) {
        LOGGER.error("Uncaught exception for thread: {}", t, e);
    }
}
