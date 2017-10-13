package com.krs.utils.threads;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: shabazzk
 * Date: Jul 19, 2014
 * Time: 4:57:50 PM
 *
 * This class wraps a ThreadFactory, and renames the threads it creates using a format string. The format string
 * should be in the form expected by String.format(). It is passed the thread ID as an integer substitution.
 *
 */
@ThreadSafe
public class NamedThreadFactory implements ThreadFactory {
    private final ThreadFactory threadFactory;
    private final AtomicInteger threadNumber = new AtomicInteger(0);
    private final String nameFormat;

    /**
     * Constructs a NamedThreadFactory using the default Executor thread factory.
     *
     * @param nameFormat the format string used to name new Threads, in the form expected by String.format(). The
     * format string is passed the thread ID as an integer substitution during Thread construction. E.g.: "Worker %d"
     * @see Executors#defaultThreadFactory()
     */
    public NamedThreadFactory(String nameFormat) {
        this(Executors.defaultThreadFactory(), nameFormat);
    }

    /**
     * Constructs a NamedThreadFactory that wraps an already existing thread factory.
     *
     * @param nameFormat the format string used to name new Threads, in the form expected by String.format(). The
     * format string is passed the thread ID as an integer substitution during Thread construction. E.g.: "Worker %d"
     * @param threadFactory the thread factory to wrap.
     */
    public NamedThreadFactory(ThreadFactory threadFactory, String nameFormat) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(nameFormat));
        Preconditions.checkNotNull(threadFactory);

        this.threadFactory = threadFactory;
        this.nameFormat = nameFormat;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = threadFactory.newThread(r);
        t.setName(String.format(nameFormat, threadNumber.getAndIncrement()));
        return t;
    }
}
