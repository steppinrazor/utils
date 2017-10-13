package com.krs.utils.threads;

import com.google.common.base.Preconditions;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 2014-05-16<p/>
 *
 * A thread factory that constructs daemon threads.
 *
 * @author krs
 */
@ThreadSafe
public final class DaemonThreadFactory implements ThreadFactory {
    private final ThreadFactory threadFactory;

    /**
     * Constructs a thread factory that sets each thread constructed by the default Executor thread
     * factory as a daemon thread.
     *
     * @see Executors#defaultThreadFactory()
     */
    public DaemonThreadFactory() {
        this(Executors.defaultThreadFactory());
    }

    /**
     * Constructs a thread factory that wraps the specified factory and sets each constructed
     * thread as a daemon thread.
     *
     * @param threadFactory the thread factory to wrap.
     */
    public DaemonThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = Preconditions.checkNotNull(threadFactory);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = threadFactory.newThread(r);
        t.setDaemon(true);
        return t;
    }
}
