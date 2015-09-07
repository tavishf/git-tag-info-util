Random Git Utils
================

Overview
--------

These are some toy git utilities for a couple of things I sometimes need, and also to let me experiment with Java 8 and the jgit library.

This stuff is probably easily doable in 2 lines of bash if you know git, but I'm a noob to git so this was my way of doing it :) 
Not sure if any of it is even correct (see noob status above), but it's my best first attempt.

Build
-----

Requires maven and JDK 8+.

To build:

mvn assembly:assembly

Makes a jar with all dependencies included in "target" dir

Run
---

Use Java 8+

From dir with the built jar in it:

$ java.exe -cp git-util-jar-with-dependencies.jar rocks.tboss.git.util.PrintTagHistory

