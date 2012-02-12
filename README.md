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
Add a `taskdef` for poppy

    <taskdef name="poppy" classname="com.poppy.Poppy" classpath="poppy.jar"/>

Specify a `classname`, a `destdir`, and a neseted `path` element.

    <poppy classname="com.poppy.P" destdir="gen">
        <path>
            <fileset dir="config" includes="**/*.properties"/>
        </path>
    </poppy>

Author
------
[Jonathan Le][jle]

[jle]:http://twitter.com/jle