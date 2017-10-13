package com.krs.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * <p>
 * This class wraps the standard Java property file syntax defined by the java.util.Properties.load() method. During
 * import of the property file, each line is first pre-processed for expansions and (#) directives and then passed
 * verbatim to the java.util.Properties.load() method. </p>
 * <p>
 * <p>Syntax: <b>${prop}</b> Any instance of ${prop} will be
 * expanded and replaced with the value of the property prop as defined earlier in the file or, depending on the
 * application, already existing in the System properties table. Expansions will continue until there are no more
 * instances of <b>${}</b> remaining on the line. </p>
 * <p>
 * <ul>
 * <li><b>#include</b> filename </li>
 * <li><b>#includeif</b> filename </li>
 * <li><b>#includecp</b> filename </li>
 * </ul>
 * <p>
 * <p>
 * <b>#include</b> directive will open and import the property file specified by filename before continuing to the next
 * line. Example: <i>#include foo.prop</i>. If the file does not exist, <b>#include</b> will generate an error.
 * </p>
 * <p>
 * <p>
 * <b>#includeif</b> is identical to #include except that it will continue importing if the file does not exist.
 * </p>
 * <p>
 * <p>
 * <b>#includecp</b> will import file_name if it is found on the classpath, if it is not found it is skipped
 * </p>
 * <p>
 * <p>
 * <b>#debug</b> <level></level> Specifying #debug followed by a numeric level will enable verbose output. The
 * higher the level, the more output. 0 is no output. 3 is max. Example: #debug 2
 * <p>
 * <p>
 * <p>
 * Additionally:<br />
 * <ul>
 * <li><b>${TEMP_DIR}</b>: is pre-defined to refer to {@code System.getProperty("java.io.tmpdir")}</li>
 * <li><b>${LINE_SEP}</b>: is pre-defined to refer to {@code System.lineSeparator()}</li>
 * <li><b>${sys.system.prop}</b>: prefix any system property with <b>sys.</b> to reference that prop via {@code System.getProperty(...)} </li>
 * <li><b>${env.environment.prop}</b>: prefix any environment property with <b>env.</b> to reference that prop via {@code System.getEnv(...)}</li>
 * </ul>
 * </p>
 * Kareem Shabazz Date: 2013.12.01 Time: 4:15 PM EST
 */
public final class PropertyImporter {
    private static final String COMMENT_CHAR = "#";
    private static final String DIRECTIVE = COMMENT_CHAR;
    private static final String LINE_CONTINUATION_CHAR = "\\";
    private static final String EXPANSION_OPEN = "${";
    private static final String EXPANSION_CLOSE = "}";
    private static final String ESCAPE_CHAR = "\\";
    private static final String[] ESCAPE_STRINGS = {ESCAPE_CHAR, COMMENT_CHAR};
    private static final String NEW_LINE = System.lineSeparator();
    private static final Map<Keyword, Pattern> DIRECTIVE_CAPTURE_PATTERNS = new HashMap<>();
    private static final int DEBUG_LEVEL_1 = 1;
    private static final int DEBUG_LEVEL_2 = 2;
    private static final int DEBUG_LEVEL_3 = 3;
    private final Properties props = new Properties(), sourceProps;

    // A stack of files opened by the importer, and the line number currently being read.
    private final ArrayDeque<Location> contextStack = new ArrayDeque<>();
    private int debugLevel = 0;

    private PropertyImporter(Properties sourceProps) {
        this.sourceProps = sourceProps;
        addPredefProps(this.sourceProps);
    }

    public static void importFromSysProp(final String propName) {
        String fileName = System.getProperty(propName);
        if (Strings.isNullOrEmpty(fileName)) {
            throw new IllegalStateException(
                    String.format("Cannot find file location [%s] for: %s", fileName, propName));
        }
        importProperties(fileName);
    }

    public static void importProperties(final String fileName) {
        // Import into the system properties.
        importProperties(fileName, System.getProperties(), System.getProperties());
    }

    public static void importProperties(final String filePath, final Properties sourceProps,
                                        final Properties resultProps) {
        // Import the properties in the file 'filePath' into the table 'resultProps'.
        // 'sourceProps', if specified, is the set of properties available during import. NOTE: source properties
        // are not included in the result set.

        Preconditions.checkNotNull(sourceProps);
        Preconditions.checkNotNull(resultProps);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(filePath), "filePath cannot be null or empty");

        PropertyImporter importer = new PropertyImporter(sourceProps);
        importer.importFile(filePath, false);

        if (importer.debugLevel == DEBUG_LEVEL_1) {
            dumpProps(importer.props);
        }

        // Incorporate the imported properties into the target table.
        resultProps.putAll(importer.props);

        if (importer.debugLevel >= DEBUG_LEVEL_2) {
            dumpProps(resultProps);
        }
    }

    private static boolean isComment(final String line) {
        return line.startsWith(COMMENT_CHAR) && Keyword.whichDirective(line) == null;
    }

    private static boolean isContinuableLine(final String line) {
        return !line.startsWith(COMMENT_CHAR);
    }

    private static boolean isLineContinued(final String line) {
        return isContinuableLine(line) && line.endsWith(LINE_CONTINUATION_CHAR);
    }

    private static boolean isEscaped(final String source, final int index) {
        // Return true if the character at source[index] is escaped. False otherwise.
        // A character is escaped if it is procceded by an unescaped escape char.
        int idx = index;
        if (idx >= source.length()) {
            return true;
        }
        if (idx <= 0) {
            return true;
        }
        // inspect the previous character.
        idx--;
        return (source.charAt(idx) != ESCAPE_CHAR.charAt(0) || !isEscaped(source, idx));
    }

    private static int lastUnescapedIndexOf(final String source, final int fromIndex) {
        int index = fromIndex;

        while (index >= 0) {
            index = source.lastIndexOf(PropertyImporter.EXPANSION_OPEN, index);
            if (isEscaped(source, index)) {
                return index;
            }
            index--;
        }
        return -1;
    }

    private static int unescapedIndexOf(final String source) {
        int index = 0;

        while (index < source.length()) {
            index = source.indexOf(PropertyImporter.EXPANSION_CLOSE, index);
            if (index == -1) {
                break;
            }
            if (isEscaped(source, index)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static String escape(final String source) {
        String src = source;
        // Look for instances in 'source' of all strings that must be escaped, and prepend them with the escape char.
        for (String s : ESCAPE_STRINGS) {
            src = src.replace(s, ESCAPE_CHAR + s);
        }

        // Special case, need to replace crlf with its escape sequence: \n
        src = src.replace("\n", "\\n");
        return src;
    }

    private static String getDirectiveArgument(final String line, final Keyword keyword) {
        // Directives have the syntax: "#keyword argument"
        // Use a regular expression to capture the argument given this syntax. Cache the regular expressions
        // so we don't have to recompile them for each call.

        Pattern pattern = DIRECTIVE_CAPTURE_PATTERNS.get(keyword);
        if (pattern == null) {
            String regex = DIRECTIVE + keyword.toString() + "[ \\t]+([^ \\t\\n\\r\\f]+)";
            pattern = Pattern.compile(regex);
            DIRECTIVE_CAPTURE_PATTERNS.put(keyword, pattern);
        }

        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        int directiveArgumentGroup = 1;
        return matcher.group(directiveArgumentGroup);
    }

    private static void dumpProps(final Properties props) {
        // Output all props sorted alphabetically.
        TreeSet<String> t = new TreeSet<>();

        for (Map.Entry<Object, Object> e : props.entrySet()) {
            String value = e.getValue().toString();
            // We don't want to display passwords in the logs,
            // but we also don't have a way to accurately tell which
            // entries are sensitive or not. This should cover
            // all the important cases.
            if (isPassword(e.getKey().toString().toLowerCase())) {
                value = "********";
            }

            t.add(e.getKey() + "=" + value);
        }

        for (String s : t) {
            Env.consoleOut(s);
        }
    }

    private static boolean isPassword(final String value) {
        return value.matches(".*(pass|pwd|cred).*");
    }

    // METHODS
    private void addPredefProps(Properties props) {
        props.put("TEMP_DIR", System.getProperty("java.io.tmpdir"));
        props.put("LINE_SEP", System.lineSeparator());
    }

    private void writeDebug(final int level, final String message) {
        if (debugLevel >= level) {
            Env.consoleOut(message);
        }
    }

    private IllegalStateException createIllegalStateException(final String message) {
        return createIllegalStateException(message, null);
    }

    private String createImportErrorMessage(final String message) {
        String msg = "PropertyImporter: " + message;
        if (!contextStack.isEmpty()) {
            // The top of the context stack, if it exists, represents the location of the error.
            msg += NEW_LINE + "\tat " + contextStack.peek().toString();
        }
        return msg;
    }

    private IllegalStateException createIllegalStateException(final String message, final Throwable underlyingException) {
        return new IllegalStateException(createImportErrorMessage(message), underlyingException);
    }

    private String lookupProperty(final String key) {
        if (key.startsWith("env.")) {
            String envVarName = key.substring("env.".length(), key.length());
            String result = System.getenv(envVarName);
            if (result == null && sourceProps != null) {
                result = sourceProps.getProperty(envVarName);
            }
            return result;
        } else if (key.startsWith("sys.")) {
            String envVarName = key.substring("sys.".length(), key.length());
            String result = System.getProperty(envVarName);
            if (result == null && sourceProps != null) {
                result = sourceProps.getProperty(envVarName);
            }
            return result;
        } else {
            String result = props.getProperty(key);
            if (result == null && sourceProps != null) {
                result = sourceProps.getProperty(key);
            }
            return result;
        }
    }

    private String processExpansions(final String line) {
        String expanding = line;
        int i = 0;
        writeDebug(DEBUG_LEVEL_3, i++ + "> " + expanding);

        // An expansion is defined as the sub-string "${name}", and is replaced by the value of property 'name'.
        // Expansions may be nested:
        // x.y=10 : index=y : value=${x.${index}} -> value=${x.y} -> value=10
        //ALGO:
        // Process all expansions in a depth-first, left-to-right order. Find the first '}' token in the
        // string. From that index, look backwards for the first '${' token. Expand the value and repeat until no
        // more expansions can be found.
        //
        // Note that expansion tokens must be ignored if escaped.

        while (true) {
            int endIndex = unescapedIndexOf(expanding);
            int startIndex = lastUnescapedIndexOf(expanding, endIndex);

            if (startIndex == -1 || endIndex == -1) {
                // Expansion cannot be found, so done.
                break;
            }

            String propertyName = expanding.substring(startIndex + EXPANSION_OPEN.length(), endIndex);
            String lookup = lookupProperty(propertyName);

            if (lookup == null) {
                throw createIllegalStateException("Property does not exist: " + propertyName);
            } else {
                // The line we are processing has been read from the property file and so will have certain
                // characters escaped according to Java's property file syntax. However, expanded values come
                // from the property table and are not escaped. Therefore, we need to escape the expansion
                // before inserting it into the line.
                //
                lookup = escape(lookup);
            }

            expanding = expanding.substring(0, startIndex) +
                    lookup +
                    expanding.substring(endIndex + EXPANSION_CLOSE.length());

            writeDebug(DEBUG_LEVEL_3, i++ + "> " + expanding);
        }

        return expanding;
    }

    private void importLine(final String line) throws IOException {
        if (!Strings.isNullOrEmpty(line))
            props.load(new StringReader(line));
    }

    private void processDirective(final String line, final Keyword keyword) //NOSONAR -  MethodCyclomaticComplexity
            throws IOException {
        // A directive is of the form: :keyword argument

        String expanded = processExpansions(line);

        String argument = getDirectiveArgument(expanded, keyword);
        writeDebug(DEBUG_LEVEL_2, expanded);

        switch (keyword) {
            case DEBUG: {
                if (argument == null) {
                    return;
                }
                // Set the debug level to the integer specified by argument.
                try {
                    debugLevel = Integer.parseInt(argument);
                    if (debugLevel < DEBUG_LEVEL_1) {
                        debugLevel = DEBUG_LEVEL_1;
                    } else if (debugLevel > DEBUG_LEVEL_3) {
                        debugLevel = DEBUG_LEVEL_3;
                    }
                } catch (NumberFormatException e) {
                    throw createIllegalStateException(
                            "#debug argument [" + argument + "] not valid, should be a number between 1-3", e);
                }
            }
            break;

            case INCLUDE:
            case INCLUDE_IF: {
                if (argument == null && keyword == Keyword.INCLUDE_IF) {
                    return;
                }

                importFile(argument, keyword == Keyword.INCLUDE_IF);
            }
            break;

            case INCLUDE_CP:
                if (argument == null) {
                    return;
                }
                importFileOnClasspath(argument);
                break;
            default:
                throw createIllegalStateException("unknown keyword: " + keyword.toString());
        }
    }

    private String readLogicalLine(final BufferedReader inputStream) throws IOException {
        // Java's property file syntax allows line continuation. This function reads line-by-line
        // from the input stream, but will batch together lines that are continued.
        Preconditions.checkNotNull(inputStream);

        if (!inputStream.ready()) {
            return null;
        }

        StringBuilder result = new StringBuilder("");
        while (inputStream.ready()) {
            // Increment the current line number.
            contextStack.peek().nextLineNum();

            String oneLine = inputStream.readLine();
            if (oneLine == null) {
                return null;
            }
            // Remove leading/trailing whitespace. makes analysis easier.
            result.append(oneLine.trim());
            if (!isLineContinued(oneLine)) {
                break;
            }
            // Break the line to separate it from the next one.
            result.append("\n");
        }

        return result.toString();
    }

    private void importFromReader(final BufferedReader reader) throws IOException {
        String line;
        Keyword keyword;

        while ((line = readLogicalLine(reader)) != null) {
            keyword = Keyword.whichDirective(line);
            if (isComment(line)) {
                writeDebug(DEBUG_LEVEL_3, COMMENT_CHAR + line);
            } else if (keyword != null) {
                processDirective(line, keyword);
            } else {
                line = processExpansions(line);

                // Commit the property into the Properties table.
                importLine(line);
            }
        }
    }

    private void importFile(final String fileName, //NOSONAR -  MethodCyclomaticComplexity
                            final boolean ignoreFileNotFound) {
        try {
            BufferedReader reader = null;

            try {
                if (fileName == null) {
                    throw createIllegalStateException("import fileName is null");
                }

                File file = new File(fileName);

                // If can't find the file, look in the directory of the file that #include'd this one.
                if (!file.isFile() && !contextStack.isEmpty()) {
                    writeDebug(DEBUG_LEVEL_2, String.format("Cannot find '%s'. Is directory executable?", file));
                    file = new File(contextStack.peek().file.getParentFile(), file.getName());
                }

                writeDebug(DEBUG_LEVEL_2, COMMENT_CHAR + "loading: " + file.getCanonicalPath());

                if (!file.isFile()) {
                    String notFoundMessage = COMMENT_CHAR + "cannot find file: " + file.getCanonicalPath();
                    writeDebug(DEBUG_LEVEL_2, notFoundMessage);

                    if (ignoreFileNotFound) {
                        // Skip this file and keep going.
                        return;
                    }
                    throw createIllegalStateException(notFoundMessage);
                }

                if (!file.canRead()) {
                    throw createIllegalStateException("cannot read file: " + file.getCanonicalPath());
                }

                // Start importing the file.
                contextStack.push(new Location(file));
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                importFromReader(reader);
                contextStack.pop();
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        } catch (FileNotFoundException e) {
            throw createIllegalStateException("File not found: ", e);
        } catch (IOException e) {
            throw createIllegalStateException("IO exception: ", e);
        }
    }

    //classpath:jar handling measures

    /**
     * <p> <ol> <li>A config file referring to a claasspath config file, that classpath config file <b>MUST</b> must not
     * have any <i>realtive paths</i> only other classpath includes or full fle system path</li> <li>If the file
     * #includecp refers to does not exist then this is simply ignored</li> </ol> </p>
     */
    private void importFileOnClasspath(final String fileOnClasspath)   //NOSONAR -  MethodCyclomaticComplexity
    {
        FileOutputStream tempFileStream = null;
        InputStream classpathFileStream = null;
        BufferedReader inputStream = null;
        Path tempFilePath = null;
        String fileName = fileOnClasspath.substring(fileOnClasspath.lastIndexOf('/') + 1);
        try { //NOSONAR - false complaint cleaning up stream
            tempFilePath = Files.createTempFile(fileName, null);
            classpathFileStream = PropertyImporter.class.getResourceAsStream("/" + fileOnClasspath);

            if (classpathFileStream == null) {
                //can't find on the treacherous classpath
                return; //NOSONAR - false complaint cleaning up stream
            }
            tempFileStream = new FileOutputStream(tempFilePath.toFile()); //NOSONAR - false complaint cleaning up stream
            byte[] buffer = new byte[1024];  //NOSONAR - magic number
            int n;
            while (-1 != (n = classpathFileStream.read(buffer))) {
                tempFileStream.write(buffer, 0, n);
            }

            File file = tempFilePath.toFile();

            // If can't find the file, look in the directory of the file that #include'd this one.
            if (!file.isFile() && !contextStack.isEmpty()) {
                writeDebug(DEBUG_LEVEL_2, String.format("Cannot find '%s'. Is directory executable?", file));
                file = new File(contextStack.peek().file.getParentFile(), file.getName());
            }

            writeDebug(DEBUG_LEVEL_2, COMMENT_CHAR + "loading: " + file.getCanonicalPath());

            if (!file.isFile()) {
                String notFoundMessage = COMMENT_CHAR + "cannot find file: " + file.getCanonicalPath();
                writeDebug(DEBUG_LEVEL_2, notFoundMessage);
            }

            if (!file.canRead()) {
                throw createIllegalStateException("cannot read file: " + file.getCanonicalPath());
            }

            // Start importing the file.
            contextStack.push(new Location(file));
            inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            importFromReader(inputStream);
            contextStack.pop();
        } catch (FileNotFoundException e) {
            throw createIllegalStateException("File not found: ", e);
        } catch (IOException e) {
            throw createIllegalStateException("IO exception: ", e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (tempFileStream != null) {
                    tempFileStream.close();
                }
                if (classpathFileStream != null) {
                    classpathFileStream.close();
                }
                if (tempFilePath != null && !tempFilePath.toFile().delete()) {
                    Env.consoleOut(tempFilePath.toString());
                }
            } catch (IOException e) {
                Env.consoleErr("Exception occurred cleaning up after property file import");
                Env.printStackTrace(e);
            }
        }
    }

    private enum Keyword {
        DEBUG("debug"),
        INCLUDE_IF("includeif"),
        INCLUDE("include"),
        INCLUDE_CP("includecp");
        private final String description;

        Keyword(final String description) {
            this.description = description;
        }

        public static Keyword whichDirective(final String line) {

            for (Keyword k : Keyword.values()) {
                if (isDirective(k, line)) {
                    return k;
                }
            }
            return null;
        }

        public static boolean isDirective(final Keyword k, final String line) {
            return line.matches(String.format("^%s%s\\b.*", DIRECTIVE, k.toString()));
        }

        @Override
        public String toString() {
            return this.description;
        }
    }

    private static final class Location {
        // An object to represent an import location specified by file and line number.
        private final File file;
        private int lineNumber;

        public Location(final File file) {
            this.file = file;
            lineNumber = 0;
        }

        int nextLineNum() {
            return lineNumber++;
        }

        public String toString() {
            String fileName = "unknown";
            if (file != null) {
                try {
                    fileName = file.getCanonicalPath();
                } catch (IOException e) {
                    // Something went wrong, so the message won't have a valid filename. Oh well...
                }
            }
            return fileName + ":" + lineNumber;
        }
    }
}

