package com.krs.utils;

import com.google.common.base.Preconditions;

import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by KR Shabazz on 3/6/16 8:57 PM.
 */
public final class TimeStampedGUID {
    private TimeStampedGUID() {
    }

    private static final int YEAR = Preconditions.checkNotNull(Integer.getInteger("guidYearOffset"), "'guidYearOffset' is not set");

    private static final int[] LOOKUP = new int[]{
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15,
            0x16, 0x17, 0x18, 0x19, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x30, 0x31,
            0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47,
            0x48, 0x49, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x60, 0x61, 0x62, 0x63,
            0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79,
            0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x90, 0x91, 0x92, 0x93, 0x94, 0x95,
            0x96, 0x97, 0x98, 0x99
    };

    /**
     * Returns a Supplier of modified <a href="https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_4_.28random.29">version 4
     * UUIDs</a> where the first 6 bytes hold the current UTC time and the remaining 10 bytes are random,
     * in the following hexadecimal form:
     * <p>
     * <pre>    yyMMddhh-mmss-4xxx-zxxx-xxxxxxxxxxxx</pre>
     * where x is any hexadecimal digit and z is one of 8, 9, a or b. Example:
     * <pre>    10070605-2232-42fe-9a13-c4bf80c6b99c</pre>
     *
     * The resulting UUIDs contain 74 bits of entropy, after subtracting 6 bits for versioning
     * metadata, and so can be modeled as the current time (to second accuracy) concatenated with a
     * random number between 0 and 18,889,465,931,478,580,854,784 (~19e21).
     *
     * @see UUID
     */
    public static Supplier<UUID> supplier() {
        return new Supplier<UUID>() {
            final SecureRandom numberGenerator = new SecureRandom();
            final ZoneId utc = ZoneId.of("UTC");

            @Override
            public UUID get() {
                byte[] randomBytes = new byte[10];
                numberGenerator.nextBytes(randomBytes);

                ZonedDateTime dt = ZonedDateTime.now(utc);
                long msb = LOOKUP[dt.getYear() - YEAR];
                msb = (msb << 8) | LOOKUP[dt.getMonthValue()];
                msb = (msb << 8) | LOOKUP[dt.getDayOfMonth()];
                msb = (msb << 8) | LOOKUP[dt.getHour()];
                msb = (msb << 8) | LOOKUP[dt.getMinute()];
                msb = (msb << 8) | LOOKUP[dt.getSecond()];
                msb = (msb << 8) | ((randomBytes[0] & 0x0f) | 0x40);  // set to version 4
                msb = (msb << 8) | (randomBytes[1] & 0xff);

                long lsb = (randomBytes[2] & 0x3f) | 0x80;  // set to IETF variant
                lsb = (lsb << 8) | (randomBytes[3] & 0xff);
                lsb = (lsb << 8) | (randomBytes[4] & 0xff);
                lsb = (lsb << 8) | (randomBytes[5] & 0xff);
                lsb = (lsb << 8) | (randomBytes[6] & 0xff);
                lsb = (lsb << 8) | (randomBytes[7] & 0xff);
                lsb = (lsb << 8) | (randomBytes[8] & 0xff);
                lsb = (lsb << 8) | (randomBytes[9] & 0xff);

                return new UUID(msb, lsb);
            }
        };
    }

    public static Stream<UUID> stream() {
        return Stream.generate(supplier());
    }

    public static Iterator<UUID> iterator() {
        return stream().iterator();
    }
}
