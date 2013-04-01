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
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PropertySet;

import java.io.File;

/**
 * @author Jonathan Le
 */
public class PoppyTests extends BuildFileTest {
    private static final String NO_EXCEPTION_THROWN = "No exception thrown.";
    public static final String CLASSNAME = "org.vandalsoftware.P";

    public PoppyTests() {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configureProject("build.xml");
    }

    public void testMissingClassName() {
        try {
            final Poppy p = new Poppy();
            p.execute();
            fail(NO_EXCEPTION_THROWN);
        } catch (Exception e) {
            assertThrowsException(Poppy.ERROR_CLASSNAME_NOT_SET, e);
        }
    }
    
    public void testMissingDestDir() {
        try {
            final Poppy p = new Poppy();
            p.setClassname(CLASSNAME);
            p.execute();
            fail(NO_EXCEPTION_THROWN);
        } catch (Exception e) {
            assertThrowsException(Poppy.ERROR_DESTDIR_NOT_SET, e);
        }
    }

    public void testMissingPathOrPropertySet() {
        try {
            final Poppy p = new Poppy();
            p.setClassname(CLASSNAME);
            p.setDestDir("gen");
            p.execute();
            fail(NO_EXCEPTION_THROWN);
        } catch (Exception e) {
            assertThrowsException(Poppy.PATH_OR_PROPERTYSET_NOT_ADDED, e);
        }
    }

    public void testFileGenerated() {
        final Poppy p = new Poppy();
        final Project project = getProject();
        p.setProject(project);
        p.setClassname(CLASSNAME);
        p.setDestDir("gen");
        p.addPath(new Path(project, "config/sample.properties"));
        p.execute();
        assertGeneratedFileExists();
    }

    public void testFileGeneratedWithPropertySet() {
        final Poppy p = new Poppy();
        final Project project = getProject();
        p.setProject(project);
        p.setClassname(CLASSNAME);
        p.setDestDir("gen");
        p.addPropertySet(new PropertySet());
        p.execute();
        assertGeneratedFileExists();
    }

    public void testFileGeneratedWithBothNested() {
        final Poppy p = new Poppy();
        final Project project = getProject();
        p.setProject(project);
        p.setClassname(CLASSNAME);
        p.setDestDir("gen");
        p.addPropertySet(new PropertySet());
        p.addPath(new Path(project, "config/sample.properties"));
        p.execute();
        assertGeneratedFileExists();
    }

    private void assertThrowsException(String expectedMessage, Exception e) {
        assertEquals(BuildException.class, e.getClass());
        assertEquals("Wrong exception message", expectedMessage, e.getMessage());
    }

    private void assertGeneratedFileExists() {
        final File f = new File(getProject().getBaseDir(), "gen");
        assertTrue("No generated directory", f.isDirectory());
        assertTrue("No generated file", new File(f, "org/vandalsoftware/P.java").isFile());
    }
}
