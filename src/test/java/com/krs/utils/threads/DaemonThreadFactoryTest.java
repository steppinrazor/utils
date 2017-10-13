package com.krs.utils.threads;

import org.testng.annotations.Test;

import java.util.concurrent.ThreadFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by KR Shabazz on 3/8/16 9:48 PM.
 */
public class DaemonThreadFactoryTest {

    @Test
    public void testDefaultImplementationCreatesDaemonThreads() {
        ThreadFactory f = new DaemonThreadFactory();
        Thread t = f.newThread(() -> {});
        assertThat(t.isDaemon()).isTrue();
    }
}