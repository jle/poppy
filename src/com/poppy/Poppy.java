package com.poppy;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

/**
 * A task that generates a Java source file from Ant properties.
 *
 * @author Jonathan Le
 */
public class Poppy extends Task {
    public static final String ERROR_CLASSNAME_NOT_SET = "classname not set";
    public static final String ERROR_DESTDIR_NOT_SET = "destdir not set";
    public static final String ERROR_PATH_NOT_ADDED = "path not added";
    private static final String ERROR_COULD_NOT_CREATE_FILE = "Could not create file";

    private static final String JAVA = ".java";
    private static final String TAB_SPACE = "    ";

    private final ArrayList<Path> paths;
    private String className;
    private String destDir;

    public Poppy() {
        paths = new ArrayList<Path>();
    }

    /**
     * Sets a classname for the Java class that will be generated.
     * @param className a fully qualified classname. e.g. com.poppy.P
     */
    public void setClassname(String className) {
        this.className = className;
    }

    /**
     * Sets a destDir to write the Java file.
     * @param destDir the directory relative to the project
     */
    public void setDestDir(String destDir) {
        this.destDir = destDir;
    }

    /**
     * Add a path to properties files.
     * @param path a path
     */
    public void addPath(Path path) {
        paths.add(path);
    }

    /**
     * Processes properties and writes them out into a Java file.
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        validate();
        process();
    }

    private void process() throws BuildException {
        final ClassInfo classInfo = prepareClassInfo();
        FileOutputStream fos = null;
        try {
            final File file = classInfo.file;
            log("Opening file " + file, Project.MSG_VERBOSE);
            fos = new FileOutputStream(file);
            if (classInfo.packageName != null) {
                fos.write(String.format("package %s;\n\n", classInfo.packageName).getBytes());
            }

            fos.write(String.format("public class %s {\n", classInfo.simpleName).getBytes());
            for (Path path : paths) {
                final String[] includedFiles = path.list();
                for (String includedFile : includedFiles) {
                    log("Reading property file " + includedFile, Project.MSG_VERBOSE);
                    readWrite(includedFile, fos);
                }
            }
            fos.write("}\n".getBytes());
            fos.flush();
        } catch (FileNotFoundException e) {
            throw new BuildException(ERROR_COULD_NOT_CREATE_FILE);
        } catch (IOException e) {
            throw new BuildException(ERROR_COULD_NOT_CREATE_FILE);
        } finally {
            close(fos);
        }
    }

    private ClassInfo prepareClassInfo() {
        final File destDir = new File(getProject().getBaseDir(), this.destDir);
        final File genFile;
        final int i = this.className.lastIndexOf('.');
        if (i != -1) {
            final String packageName = this.className.substring(0, i);
            final String simpleName = this.className.substring(i + 1);
            genFile = new File(destDir, packageName.replace('.', '/'));
            if (genFile.mkdirs()) {
                log("Created directory " + genFile, Project.MSG_VERBOSE);
            }
            return new ClassInfo(packageName, simpleName, new File(genFile, simpleName + JAVA));
        } else {
            return new ClassInfo(null, this.className, new File(destDir, this.className + JAVA));
        }
    }

    private void readWrite(String filePath, FileOutputStream fos) {
        FileInputStream fis = null;
        try {
            File f = new File(filePath);
            String fName = f.getName();
            log("Processing " + fName);
            fos.write(String.format(TAB_SPACE + "// %s\n", fName).getBytes());
            fis = new FileInputStream(f);
            final BufferedInputStream bis = new BufferedInputStream(fis);
            final Properties props = new Properties();
            props.load(bis);
            for (Map.Entry e : props.entrySet()) {
                final String name = (String) e.getKey();
                final String value = (String) e.getValue();
                fos.write(String.format(DataType.evaluateType(name, value).getFormat(),
                        name.replace('.', '_').toUpperCase(), value).getBytes());
            }
        } catch (FileNotFoundException ignore) {
            log("Not found: " + filePath, Project.MSG_WARN);
        } catch (IOException e) {
            log("Load error: " + filePath, Project.MSG_WARN);
        } finally {
            close(fis);
        }
    }

    private void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignore) {
            }
        }
    }

    private void validate() throws BuildException {
        if (isEmpty(this.className)) {
            throw new BuildException(ERROR_CLASSNAME_NOT_SET);
        }
        if (isEmpty(this.destDir)) {
            throw new BuildException(ERROR_DESTDIR_NOT_SET);
        }
        if (this.paths.isEmpty()) {
            throw new BuildException(ERROR_PATH_NOT_ADDED);
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * A helper class.
     */
    public static class ClassInfo {
        final String packageName;
        final String simpleName;
        final File file;

        public ClassInfo(String packageName, String simpleName, File file) {
            this.packageName = packageName;
            this.simpleName = simpleName;
            this.file = file;
        }
    }

    /**
     * An enum that describes data types.
     */
    public enum DataType {
        INT("public static final int %1$s = %2$s;\n"),
        BOOLEAN("public static final boolean %1$s = %2$s;\n"),
        SHORT("public static final short %1$s = %2$s;\n"),
        LONG("public static final long %1$s = %2$sL;\n"),
        DOUBLE("public static final double %1$s = %2$s;\n"),
        FLOAT("public static final float %1$s = %2$sF;\n"),
        CHAR("public static final char %1$s = \"%2$s\";\n"),
        STRING("public static final String %1$s = \"%2$s\";\n");

        private final String format;

        private DataType(String format) {
            this.format = TAB_SPACE + format;
        }

        public String getFormat() {
            return format;
        }

        public static DataType evaluateType(String name, String value) {
            if (name.startsWith("int.")) {
                return INT;
            } else if (name.startsWith("bool.") || "true".equals(value) || "false".equals(value)) {
                return BOOLEAN;
            } else if (name.startsWith("short.")) {
                return SHORT;
            } else if (name.startsWith("long.")) {
                return LONG;
            } else if (name.startsWith("double.")) {
                return DOUBLE;
            } else if (name.startsWith("float.")) {
                return FLOAT;
            } else if (name.startsWith("char.")) {
                return CHAR;
            } else {
                return STRING;
            }
        }
    }
}