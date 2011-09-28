Unitconv
========

Yet another Android unit conversion app.

Based on the tutorial http://www.grammaticalframework.org/android/tutorial/

Building PGF
------------

    gf -make -s -optimize-pgf -mk-index --path ../lib/:../Numerals/ Unitconv???.gf
    mv Unitconv.pgf ${APP}/res/raw/unitconv.pgf
