Poppy
=====

Generates Java source code from Ant properties. Developed by [Jonathan Le][jle].

What does it do?
----------------

It converts this:

    my.property=My Property

Or this:

    <property name="debug" value="true"/>

Into this:

    public static final String MY_PROPERTY = "My Property";
    public static final boolean DEBUG = true;

Why would I need that?
----------------------

There are many uses. For example, you might want to conditionally compile some code based on a property set at build
time.

    if (DEBUG) {
        ...
    }

But I'm sure you'll be able to come up with more interesting uses than that.

More Info
---------

Please see the [wiki][wiki] for instructions on how to use poppy.

[jle]:http://twitter.com/jle
[wiki]:https://github.com/jonathanle/poppy/wiki
