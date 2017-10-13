package com.krs.utils.threads;


import org.slf4j.Logger;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import static com.krs.utils.testing.ReflectiveTestHelper.injectMockLogger;
import static com.krs.utils.threads.ThreadUtils.guard;
import static com.krs.utils.threads.ThreadUtils.newNamedDaemonThreadFactory;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * Date: Aug 27, 2014
 * Time: 3:58:38 PM<p/>
 *
 * @author krs
 */

public class ThreadUtilsTest {
    final ExecutorService service = newFixedThreadPool(1);
    final RuntimeException defaultException = new RuntimeException("msg");
    
    @Test
    public void testRunnableGuardAppliesHandlerCatchesException() throws Exception {
        Thread.UncaughtExceptionHandler mockHandler = mock(Thread.UncaughtExceptionHandler.class);
        Runnable sut = guard(newRunner(defaultException), mockHandler);
        service.submit(sut).get();

        verify(mockHandler).uncaughtException(any(Thread.class), eq(defaultException));
    }

    @Test
    public void testRunnableGuardDefaultHandlerLogsException() throws Exception {
        Logger mockLogger = injectMockLogger(ExceptionLogger.INSTANCE);
        Runnable sut = guard(newRunner(defaultException));
        service.submit(sut).get();

        verify(mockLogger).error(anyString(), any(Thread.class), eq(defaultException));
    }

    @Test
    public void testCallableGuardAppliesHandlerCatchesException() throws Exception {
        Thread.UncaughtExceptionHandler mockHandler = mock(Thread.UncaughtExceptionHandler.class);
        String defaultRes = "default";
        Callable<Optional<String>> sut = guard(newCaller(defaultException), defaultRes, mockHandler);
        Future<Optional<String>> f = service.submit(sut);

        verify(mockHandler).uncaughtException(any(Thread.class), eq(defaultException));
        assertThat(f.get().get()).isEqualToIgnoringCase(defaultRes);
    }

    @Test
    public void testCallableGuardDefaultHandlerLogsException() throws Exception {
        String defaultRes = "default";
        Logger mockLogger = injectMockLogger(ExceptionLogger.INSTANCE);
        Callable<Optional<String>> sut = guard(newCaller(defaultException), defaultRes);
        Future<Optional<String>> f = service.submit(sut);

        assertThat(f.get().get()).isEqualToIgnoringCase(defaultRes);
        verify(mockLogger).error(anyString(), any(Thread.class), eq(defaultException));
    }

    @Test
    public void testCallableGuardNoDefaultResult() throws Exception {
        Callable<Optional<String>> sut = guard(newCaller(defaultException));
        Future<Optional<String>> f = service.submit(sut);
        assertThat(f.get().isPresent()).isFalse();
    }

    @Test
    public void testCallableGuardNoException() throws Exception {
        String test = "it works", defaultRes = "default";
        Thread.UncaughtExceptionHandler mockHandler = mock(Thread.UncaughtExceptionHandler.class);
        Callable<Optional<String>> sut = guard(() -> test, defaultRes, mockHandler);
        Future<Optional<String>> f = service.submit(sut);

        assertThat(f.get().get()).isEqualTo(test);
        verifyZeroInteractions(mockHandler);
    }

    @Test
    public void testNamedDaemonFactory(){
        ThreadFactory f = newNamedDaemonThreadFactory("krs-%d");
        Thread t = f.newThread(() -> {});
        assertThat(t.getName()).isEqualTo("krs-0");
        assertThat(t.isDaemon()).isTrue();

        t = f.newThread(() -> {
        });
        assertThat(t.getName()).isEqualTo("krs-1");
    }

    Runnable newRunner(RuntimeException e) {
        return () -> {
            throw e;
        };
    }

    Callable<String> newCaller(RuntimeException e) {
        return () -> {
            throw e;
        };
    }
}
