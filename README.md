Calc
====

Yet another Android unit conversion and arithmetical expression evaluator app.

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

### Getting the PGF grammar file

The PGF file is not checked in. To compile the app you have to first get the PGF and
copy it into the app's `res/raw/` directory. The grammars are developed by a separate
project (http://github.com/Kaljurand/Grammars/), and pre-compiled grammars have been
made available at http://kaljurand.github.com/Grammars/. So updating to the latest PGF
(before compiling) is as simple as:

   curl http://kaljurand.github.com/Grammars/grammars/pgf/Calc.pgf > ${APP}/res/raw/grammar.pgf


Tests
-----

Unitconv:

  * kaks sada tuhat viis teist milli kraadi kraadides
  * kaks minutit sekundites (NB! ambiguous with 2 readings)
  * kaks hektarit ruut kilo meetrites
  * kolm meetrit miilides (ERROR: internal converter does not understad `mile`)
  * viis norra krooni vanas rahas (ERROR: internal converter does not understad `NOK`)

Exp:

  * Pii korda miinus kaks jagatud pool teist

Direction:

  * Algus Sõpruse puiestee sada kakskümmend kolm Lõpp Vabaõhumuuseumi tee neli kümmend viis
