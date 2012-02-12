package com.poppy;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

import java.io.File;

/**
 * @author Jonathan Le
 */
public class PoppyTests extends BuildFileTest {
    private static final String NO_EXCEPTION_THROWN = "No exception thrown.";
    public static final String CLASSNAME = "com.poppy.P";

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

    public void testMissingPath() {
        try {
            final Poppy p = new Poppy();
            p.setClassname(CLASSNAME);
            p.setDestDir("gen");
            p.execute();
            fail(NO_EXCEPTION_THROWN);
        } catch (Exception e) {
            assertThrowsException(Poppy.ERROR_PATH_NOT_ADDED, e);
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

        final File f = new File(project.getBaseDir(), "gen");
        assertTrue("No generated directory", f.isDirectory());
        assertTrue("No generated file", new File(f, "com/poppy/P.java").isFile());
    }

    private void assertThrowsException(String expectedMessage, Exception e) {
        assertEquals(BuildException.class, e.getClass());
        assertEquals("Wrong exception message", expectedMessage, e.getMessage());
    }
}
