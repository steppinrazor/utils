package com.krs.utils;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Supplier;

import static com.krs.utils.TimeStampedGUID.iterator;
import static com.krs.utils.TimeStampedGUID.supplier;
import static java.lang.Integer.getInteger;
import static java.lang.String.format;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Created by KR Shabazz on 3/6/16 9:19 PM.
 */
public class TimeStampedGUIDTest {
    @BeforeClass
    public void setup(){
        setProperty("guidYearOffset", "2000");
    }

    @AfterClass
    public void cleanup(){
        clearProperty("guidYearOffset");
    }

    @Test
    public void testTimestampedGuidIteratorGenerationAreAllUnique() throws Exception {
        int size = 100_000, count = 0;

        HashSet<UUID> uniqs = new HashSet<>(size);
        for (Iterator<UUID> it = iterator(); count++ < size && it.hasNext(); )
            uniqs.add(it.next());

        assertThat(uniqs.size()).isEqualTo(size);
    }

    @Test
    public void testDateTimeFormat() {
        Supplier<UUID> s = supplier();
        ZonedDateTime dt0 = ZonedDateTime.now(ZoneId.of("UTC"));
        String uuid = s.get().toString();
        //just on the infinitessimally small chance that between the creation of 'dt0' and 'uuid' it was right at the cusp of
        //the seconds rollover and they do not match we sample the time again
        ZonedDateTime dt1 = ZonedDateTime.now(ZoneId.of("UTC"));

        if (!matchDateTimePrefix(uuid, dt0) && !matchDateTimePrefix(uuid, dt1)) {
            fail(format("Could not match prefix of UUID [%s] against either sample date [%s] or [%s]", uuid, dt0, dt1));
        }
    }

    @Test
    public void test19thCharacter() {
        String regex = "[89ab]";
        String uuid = supplier().get().toString();
        assertThat(uuid)
                .matches(u -> (uuid.charAt(19) + "").matches(regex), "regex " + regex + " matches 19th char");
    }

    boolean matchDateTimePrefix(String uuid, ZonedDateTime dt) {
        return uuid.startsWith(prefixFormat(dt));
    }

    String prefixFormat(ZonedDateTime dt) {
        //yyMMddhh-mmss-4xxx-zxxx-xxxxxxxxxxxx
        return format("%d%02d%02d%02d-%02d%02d-4", dt.getYear() - getInteger("guidYearOffset"), dt.getMonthValue(), dt.getDayOfMonth(), dt.getHour(), dt.getMinute(), dt.getSecond());
    }
}