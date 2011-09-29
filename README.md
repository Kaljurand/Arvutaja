Unitconv
========

Yet another Android unit conversion app.

_Work in progress_

Features:

  * No menus! The user is expected to enter the unit conversion expression by Estonian speech
  * Unit conversion expression conversion powered by GF
  * Unit conversion powered by jscience-4.3.jar (unit-api-0.6.0.jar)
  * Uses grammar-aware Estonian speech recognition service for Android (install separately: http://recognizer-intent.googlecode.com)


Background
----------

### GF

  * GF on Android tutorial: http://www.grammaticalframework.org/android/tutorial/


Other
-----

### Building PGF

The PGF file is not checked in. To compile the app you have to build the PGF and
copy it into the app's `res/raw/` directory. The relevant GF-files are currently
part of the net-speech-api project's gf-branch (http://net-speech-api.googlecode.com).

    gf -make -s -optimize-pgf -mk-index --path ../lib/:../Numerals/ Unitconv???.gf
    mv Unitconv.pgf ${APP}/res/raw/unitconv.pgf


Tests
-----

  * kaks sada tuhat viis teist milli kraadi kraadides
  * kaks minutit sekundites (2 readings, 2nd currently gets a Java parse error)
  * kaks hektarit ruut kilo meetrites
