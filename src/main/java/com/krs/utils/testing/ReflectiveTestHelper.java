package com.krs.utils.testing;

import org.mockito.Mockito;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Created by shabazzk on 2/24/2016.
 */
public final class ReflectiveTestHelper {
    private ReflectiveTestHelper() {
    }

    public static Logger injectMockLogger(Object classToInject) {
        return injectMockLogger(classToInject, "LOGGER");
    }

    public static Logger injectMockLogger(Class<?> classToInject) {
        return injectMockLogger(classToInject, "LOGGER");
    }

    public static Logger injectMockLogger(Class<?> classToInject, String loggerName) {
        Logger mockLogger = Mockito.mock(Logger.class);
        setPrivateStaticFinalField(classToInject, loggerName, mockLogger);
        return mockLogger;
    }

    public static Logger injectMockLogger(Object classToInject, String loggerName) {
        Logger mockLogger = Mockito.mock(Logger.class);
        setPrivateStaticFinalField(classToInject.getClass(), loggerName, mockLogger);
        return mockLogger;
    }

    /**
     * see <a href="http://www.javaspecialists.eu/archive/Issue161.html">Heinz Kabutz</a> about (<b>EVIL</b>) technique
     */
    public static void setPrivateStaticFinalField(Class<?> classToInjectIn, String variableName, Object value) {
        Field field = getFieldByNameIncludingSuperclasses(variableName, classToInjectIn);
        field.setAccessible(true);
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Field getFieldByNameIncludingSuperclasses(String fieldName, Class clazz) {
        Field retValue = null;

        try {
            retValue = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class superclass = clazz.getSuperclass();
            if (superclass != null) {
                retValue = getFieldByNameIncludingSuperclasses(fieldName, superclass);
            }
        }

        return retValue;
    }
}
