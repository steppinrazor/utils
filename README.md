#### Example usages of utils ####

###### TimeStampedGUID (version 4 GUIDs with timestamps - to second accuracy)  ######

```java
/*
Returns a Supplier of modified <a href="https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_4_.28random.29">version 4
 UUIDs</a> where the first 6 bytes hold the current UTC time (to the second) and the remaining 10 bytes are random, in the following hexadecimal form:
     
      yyMMddhh-mmss-4xxx-zxxx-xxxxxxxxxxxx where x is any hexadecimal digit and z is one of 8, 9, a or b. e.g.:
          16032203-0401-4868-82e2-9c03bb78ddf2 for March 22, 2016 03:04:01 am
     
      The resulting UUIDs contain 74 bits of entropy, after subtracting 6 bits for versioning
      metadata, and so can be modeled as the current time (to second accuracy) concatenated with a
      random number between 0 and 18,889,465,931,478,580,854,784 (~19e21).

     ****** The year offset must be set as a VM property "-DguidYearOffset=2000" *******
*/
Supplier<UUID> supplier = TimeStampedGUID.supplier();
UUID ts = supplier.get(); //16032203-0401-4868-82e2-9c03bb78ddf2

Stream<UUID> stream = TimeStampedGUID.stream(); //and similar for iterator()
```


JMH benchmarks on TimeStampedGUID
<pre>
Benchmark                                   Mode    Cnt    Score    Error   Units
BenchmarkTimeStampedGUID.measureIterator   thrpt     20  551.477 ± 22.561  ops/ms
BenchmarkTimeStampedGUID.measureStream     thrpt     20  555.698 ± 20.735  ops/ms
BenchmarkTimeStampedGUID.measureSupplier   thrpt     20  575.892 ± 24.846  ops/ms

BenchmarkTimeStampedGUID.measureIterator    avgt     20    0.002 ±  0.001   ms/op
BenchmarkTimeStampedGUID.measureStream      avgt     20    0.002 ±  0.001   ms/op
BenchmarkTimeStampedGUID.measureSupplier    avgt     20    0.002 ±  0.001   ms/op

BenchmarkTimeStampedGUID.measureIterator  sample  11052    0.002 ±  0.001   ms/op
BenchmarkTimeStampedGUID.measureStream    sample  11401    0.002 ±  0.001   ms/op
BenchmarkTimeStampedGUID.measureSupplier  sample  11344    0.002 ±  0.001   ms/op
</pre>

###### RetryStrategy + RetryPolicy ######

```java
/*
A retry strategy by itself does not describe which failures allow an operation to be retried but rather
 * what should happen between retry attempts. To be useful for retrying operations, a retry strategy is
 * wrapped as a {@link com.krs.utils.retry.RetryPolicy}, which provides domain-specific
 * classification of which failures allow retries.
*/
static final int MAX_ATTEMPTS = 5, MAX_SECONDS = 10;

//delay a retry for 10 seconds up to a limit of MAX_ATTEMPTS
RetryStrategy drs = new DelayRetryStrategy(10, TimeUnit.SECONDS, MAX_ATTEMPTS);

//retry with exponentially increasing delay up to a MAX_SECONDS with MAX_ATTEMPTS
RetryStrategy ers = new ExponentialDelayRetryStrategy(TimeUnit.SECONDS, MAX_SECONDS, MAX_ATTEMPTS);

RetryPolicy rp = RetryUtils.buildRetryPolicy((Throwable) -> {
  //this is a Function<? super Throwable, Boolean>
  //assess if the incoming throwable should be retried
  //then return boolean
  return true; 
}, ers);

//canonical usage
while (true) {
    try {
        return someOperation();
    }
    catch (Exception e) {
        if (rp.canRetry(e)) {
            rp.beforeRetry(e);
            continue;
        }
        throw e;
    }
}
```

###### PropertyImporter ######

Simple class to import Java-style properties from configuration files with the following features:

* Variable substitution/interpolation
* Import environment variables with the prefix `${env}`
* Import system variables with the prefix `${sys}`
* Reference the temporary directory `${TEMP_DIR}`
* Reference the system line separator `${LINE_SEP}`
* Import properties from other property files from the file system `#include path/to/file` - throw an exception if file not found
* `#includeif path/to/file` - does not throw an exception if file not found
* `#includecp file/on/classpath` - does not throw an exception if file not found

###### Example: ######
```java
#include included_prop.config #would through an error if file was missing
#includeif /home/shabazzk/_DEV/does.not.exist.prop #does not throw an error even though missing

# this is where the work is
name=kareem
surname=shabazz
fullname=${name} ${surname}
kareem.shabazz=this is my full name
nested=${${name}.${surname}}
imported_from_file=${db.password.all}
temp.dir=${TEMP_DIR}
path=${env.PATH}
os=${sys.os.name}
boolean.prop=false
boolean.prop2=true
integer.valid.prop=10
integer.invalid.prop=Bang
integer.blank.prop=

```

###### Other useful classes ######
* `ThreadUtils` - utilities like named `ThreadFactory`, exception-handling wrappers for `Runnable` and `Callable`
* `ReflectiveTestHelper` - inject on `private static final` fields, helper for testing
* `CurrencyLiterals` - simple Enum of a few currencies with their Unicode symbols
