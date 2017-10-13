package com.krs.utils;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by KR Shabazz on 3/8/16 7:59 PM.
 */
public class EnvTest {
    @Test
    public void testConsoleStringOutput() {
        String testThread = Thread.currentThread().getName();
        assertThat(Env.consoleString("test %d-%d", 1, 2)).isEqualToIgnoringWhitespace("[" + testThread + "] test 1-2");
    }
}