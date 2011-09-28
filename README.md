Unitconv
========

Yet another Android unit conversion app.

_Work in progress_

Features:

  * Speech input
  * GF grammars


Background
----------

### GF

  * GF on Android tutorial: http://www.grammaticalframework.org/android/tutorial/


Other
-----

### Building PGF

    gf -make -s -optimize-pgf -mk-index --path ../lib/:../Numerals/ Unitconv???.gf
    mv Unitconv.pgf ${APP}/res/raw/unitconv.pgf
