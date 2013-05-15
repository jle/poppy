/*
 * Copyright (c) 2012, Jonathan Le
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the FreeBSD Project.
 */

package com.vandalsoftware;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PropertySet;

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
    public static final String PATH_OR_PROPERTYSET_NOT_ADDED = "path or propertyset not added";
    private static final String ERROR_COULD_NOT_CREATE_FILE = "Could not create file";

    private static final String JAVA = ".java";
    private static final String TAB_SPACE = "    ";

    private final ArrayList<Path> paths;
    private final ArrayList<PropertySet> propertySets;
    private String className;
    private String destDir;

    public Poppy() {
        paths = new ArrayList<Path>();
        propertySets = new ArrayList<PropertySet>();
    }

    /**
     * Sets a classname for the Java class that will be generated.
     * @param className a fully qualified classname. e.g. org.vandalsoftware.P
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

    public void addPropertySet(PropertySet propertySet) {
        propertySets.add(propertySet);
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
            // Write a private constructor
            fos.write((TAB_SPACE + String.format("private %s() {}\n", classInfo.simpleName))
                    .getBytes());
            processPropertySets(fos);
            processPaths(fos);
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

    private void processPropertySets(FileOutputStream fos) {
        for (PropertySet propertySet : propertySets) {
            log("Processing propertyset");
            final Properties properties = propertySet.getProperties();
            try {
                writeConstant(fos, properties);
            } catch (IOException e) {
                log("Write error", Project.MSG_WARN);
            }
        }
    }

    private void processPaths(FileOutputStream fos) {
        for (Path path : paths) {
            final String[] includedFiles = path.list();
            for (String includedFile : includedFiles) {
                log("Reading property file " + includedFile, Project.MSG_VERBOSE);
                FileInputStream fis = null;
                try {
                    File f = new File(includedFile);
                    String fName = f.getName();
                    log("Processing " + fName);
                    fos.write(String.format(TAB_SPACE + "// %s\n", fName).getBytes());
                    fis = new FileInputStream(f);
                    final BufferedInputStream bis = new BufferedInputStream(fis);
                    final Properties props = new Properties();
                    props.load(bis);
                    writeConstant(fos, props);
                } catch (FileNotFoundException ignore) {
                    log("Not found: " + includedFile, Project.MSG_WARN);
                } catch (IOException e) {
                    log("Load error: " + includedFile, Project.MSG_WARN);
                } finally {
                    close(fis);
                }
            }
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

    private void writeConstant(FileOutputStream fos, Properties props) throws IOException {
        for (Map.Entry e : props.entrySet()) {
            final String name = (String) e.getKey();
            final String value = (String) e.getValue();
            DataType.write(fos, name, value);
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
        if (this.paths.isEmpty() && this.propertySets.isEmpty()) {
            throw new BuildException(PATH_OR_PROPERTYSET_NOT_ADDED);
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
        CHAR("public static final char %1$s = \'%2$s\';\n"),
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

        public static void write(FileOutputStream fos, String name, String value)
                throws IOException {
            final DataType type = DataType.evaluateType(name, value);
            if (!type.equals(DataType.STRING)) {
                final int i = name.indexOf('.');
                if (i < name.length() - 1) {
                    name = name.substring(i + 1);
                }
            }
            fos.write(String.format(type.getFormat(),
                    name.replace('.', '_').toUpperCase(), value).getBytes());
        }
    }
}
