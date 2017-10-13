package com.krs.utils;

/**
 * <p>
 *     Environment methods:
 *     <ul>
 *         <li>Static console print methods to both std out + err</li>
 *     </ul>
 * </p>
 * Created by KR Shabazz on 3/8/16 12:05 AM.
 */
public final class Env {
    private Env(){}

    public static void consoleOut(String s, Object... o) {
        System.out.println(consoleString(s, o));
    }

    public static void consoleErr(String s, Object... o) {
        System.err.println(consoleString(s, o));
    }

    static String consoleString(String s, Object... o){
        return threadNameWithBldr().append(" ").append(String.format(s, o)).toString();
    }

    public static void printStackTrace(Throwable t) {
        t.printStackTrace(System.err);
    }

    private static StringBuilder threadNameWithBldr() {
        return new StringBuilder("[").append(Thread.currentThread().getName()).append("]");
    }
}
