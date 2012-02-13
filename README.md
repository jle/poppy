Poppy
=====

Generates Java source code from Ant properties.

What does it do?
----------------

It converts this:

    my.property=My Property

Or this:

    <property name="debug" value="true"/>

Into this:

    public static final String MY_PROPERTY = "My Property";
    public static final boolean DEBUG = true;


How do I use it?
----------------
Add a taskdef for poppy

    <taskdef name="poppy" classname="com.poppy.Poppy" classpath="poppy.jar"/>

Specify a classname, a destdir, and a neseted path element.

    <poppy classname="com.poppy.P" destdir="gen">
        <path>
            <fileset dir="config" includes="**/*.properties"/>
        </path>
    </poppy>

Who is the author?
------------------
[Jonathan Le][jle]

[jle]:http://twitter.com/jle
