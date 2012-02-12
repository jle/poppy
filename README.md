Poppy
=====

Creates Java source code from Ant properties.

What does it do?
----------------

It converts this:

    my.property=My Property

Into this:

    public static final String MY_PROPERTY = "My Property";


How to use
----------
1. Add a taskdef for poppy

    <taskdef name="poppy" classname="com.poppy.Poppy" classpath="poppy.jar"/>

2. Specify a classname, a destdir, and a path.

    <poppy classname="com.poppy.P" destdir="gen">
        <path>
            <fileset dir="config" includes="**/*.properties"/>
        </path>
    </poppy>

Author
------
[Jonathan Le][jle]

[jle]:http://twitter.com/jle
