package com.krs.utils;

import org.assertj.core.data.MapEntry;
import org.testng.annotations.Test;

import java.util.Properties;

import static com.krs.utils.PropertyImporter.importFromSysProp;
import static com.krs.utils.PropertyImporter.importProperties;
import static java.lang.System.getProperty;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kareem Shabazz Date: 2013.12.04 Time: 9:59 AM EST
 */
public class PropertyImporterTest {
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    @Test
    public void testNormalUseCaseWithIncludeFile() throws Exception {
        String loc = loader.getResource("prop_file_importer/main_prop.config").getFile();
        Properties props = new Properties();
        importProperties(loc, props, props);
        assertThat(props.getProperty("fullname")).isEqualTo("kareem shabazz");
        assertThat(props.getProperty("imported")).isEqualTo("s3cr3t");
        assertThat(props.getProperty("nested")).isEqualTo("this is my full name");
        assertThat(props.getProperty("temp.dir")).isEqualTo(getProperty("java.io.tmpdir"));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testConfigWithMissingMandatoryIncludeFile() throws Exception {
        String loc = loader.getResource("prop_file_importer/config_with_missing_mandatory_include.config").getFile();
        Properties props = new Properties();
        importProperties(loc, props, props);
    }

    @Test
    public void testConfigWithMissingNonMandatoryIncludeFile() throws Exception {
        String loc =
                loader.getResource("prop_file_importer/config_with_missing_non_mandatory_include.config").getFile();
        Properties props = new Properties();
        importProperties(loc, props, props);
        assertThat(props.getProperty("fullname")).isEqualTo("kareem shabazz");
    }

    @Test
    public void testConfigWithSpaceBeforeIncludeDirective() throws Exception {
        String loc = loader.getResource("prop_file_importer/config_with_space_before_include.config").getFile();
        importProperties(loc);
        assertThat(getProperty("fullname")).isEqualTo("kareem shabazz");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testExceptionWhenTriesToExpandMissingProperty() throws Exception {
        String loc = loader.getResource("prop_file_importer/config_with_missing_property_to_expand.config").getFile();
        Properties props = new Properties();
        importProperties(loc, props, props);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*cannot find file.*")
    public void testThrowsExceptionWhenPropertyFileMissing() throws Exception {
        importProperties("does.not.exist");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*filePath cannot be null or empty.*")
    public void testThrowsExceptionWhenPropertyFileNameIsNull() throws Exception {
        importProperties(null);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testThrowsExceptionWhenIncludeEmpty() {
        String loc = loader.getResource("prop_file_importer/config_with_empty_include.config").getFile();
        Properties props = new Properties();
        importProperties(loc, props, props);
    }

    @Test
    public void testIgnoresUnknownKeyword() {
        Properties props = new Properties();
        String loc = loader.getResource("prop_file_importer/config_with_unknown_keyword.config").getFile();
        importProperties(loc, props, props);
        assertThat(props).contains(MapEntry.entry("fullname", "kareem shabazz"));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testFormatExceptionWhenDebugArgNotANumber() {
        String loc = loader.getResource("prop_file_importer/config_with_bogus_debug_arg.config").getFile();
        Properties props = new Properties();
        importProperties(loc, props, props);
    }

    @Test
    public void testNormalMultiLineUseCaseWithIncludeFile() throws Exception {
        String loc = loader.getResource("prop_file_importer/main_prop_multiline.config").getFile();
        Properties props = new Properties();
        importProperties(loc, props, props);
        assertThat(props.getProperty("fullname")).isEqualTo("kareem shabazz");
        assertThat(props.getProperty("multi")).isEqualTo("this is simulating multi-line for kareem shabazz");
    }

    @Test
    public void testNormalUseCaseUsingImportFromProperty() throws Exception {
        String loc = loader.getResource("prop_file_importer/main_prop.config").getFile();
        System.setProperty("my.config", loc);
        importFromSysProp("my.config");
        assertThat(getProperty("fullname")).isEqualTo("kareem shabazz");
        assertThat(getProperty("imported")).isEqualTo("s3cr3t");
        assertThat(getProperty("nested")).isEqualTo("this is my full name");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testImportFromPropertyThrowsExceptionWhenPropNull() throws Exception {
        System.getProperties().remove("my.config");
        importFromSysProp("my.config");
        assertThat(getProperty("fullname")).isEqualTo("kareem shabazz");
        assertThat(getProperty("imported")).isEqualTo("s3cr3t");
        assertThat(getProperty("nested")).isEqualTo("this is my full name");
    }

    @Test
    public void testImportFileOnClasspath() throws Exception {
        String loc = loader.getResource("prop_file_importer/config_with_classpath_include.config").getFile();
        importProperties(loc);
        assertThat(getProperty("dev1.db.instance")).isEqualTo("machine-1234");
        assertThat(getProperty("dev2.db.instance")).isEqualTo("machine-4567");
        assertThat(getProperty("db.password.all")).isEqualTo("s3cr3t");
    }

    @Test
    public void testImportFileOnClasspathThatDoesNotExistThrowsNoException() throws Exception {
        String loc = loader.getResource("prop_file_importer/missing_classpath_include.config").getFile();
        importProperties(loc);
    }

    @Test
    public void testImportFileExpandEnvVariable() throws Exception {
        String loc = loader.getResource("prop_file_importer/expand_env_variable.config").getFile();
        Properties props = new Properties();
        importProperties(loc, props, props);
        assertThat(props.getProperty("path")).isEqualTo(System.getenv("PATH"));
        assertThat(props.getProperty("sep")).isEqualTo(System.lineSeparator());
        assertThat(props.getProperty("temp")).isEqualTo(getProperty("java.io.tmpdir"));
        assertThat(props.getProperty("temp2")).isEqualTo(getProperty("java.io.tmpdir"));
    }
}

