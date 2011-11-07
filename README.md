Calculator
==========

*Calculator* is a novel
unit/currency conversion and arithmetical expression evaluator app for Android
(that also does Estonian address queries).

Features
--------

  * No menus! The query is entered via _Estonian speech_.
  * The _history_ of query evaluation results is presented in a persistent list.
  * Support for _vague/ambiguous queries_ (e.g. "viis norra krooni suures valuutas").
  * Clicking on a list item sends the query to a 3rd party app. This is mostly used for
    * map queries the result of which cannot be presened in the list;
    * currency conversion (which the internal evaluator does not support yet);
    * getting a "2nd opinion" (from e.g. Google Search or WolframAlpha).


Dependencies
------------

Calculator uses grammar-aware Estonian speech recognition service for Android
which you have to install separately from
http://recognizer-intent.googlecode.com


Background technologies
-----------------------

### Estonian speech recognition

Calculator uses an online grammar-aware Estonian speech recognition server

  * http://bark.phon.ioc.ee/

### GF-based speech recognition

GF is used by the server to guide speech recognition and to transform the
raw recognition result into an evaluatable form.

  * GF: http://www.grammaticalframework.org/
  * Estonian grammars: http://kaljurand.github.com/Grammars/
  * GF on Android tutorial: http://www.grammaticalframework.org/android/tutorial/

### Unit conversion

  * Unit conversion powered by jscience-4.3.jar (unit-api-0.6.0.jar)

### Arithmetical expressions

  * http://www.softwaremonkey.org/Code/MathEval


Examples
--------

The language understood by *Calculator* is described by 3 underlying grammars.
Following is a list of some interesting examples, more can be found at
http://kaljurand.github.com/Grammars/

### Unitconv

  * kaks sada tuhat viis teist milli kraadi kraadides
  * kaks hektarit ruut kilo meetrites
  * kolm hektarit aakrites (ERROR: internal converter does not understad `acre`)
  * viis norra krooni vanas rahas (ERROR: internal converter does not understand `NOK`)

#### Examples of ambiguities

  * kaks minutit sekundites (ambiguous with 2 readings)
  * viis norra krooni suures valuutas (ambiguous with ~6 readings)

### Exp

  * Pii korda miinus kaks jagatud pool teist
  * miinus üks miinus miinus kaks miinus miinus kolm ... (arbitrarily long query)
  * null astmel miinus üks (= Infinity)
  * miinus üks astmel pool (= NaN, the built-in math evaluator does not support complex numbers)

### Direction

  * Sõpruse puiestee sada kakskümmend kolm
  * Algus Sõpruse puiestee sada kakskümmend kolm Lõpp Vabaõhumuuseumi tee neli kümmend viis
  * Tartu

#### Examples of ambiguities

  * Roo (street in Tallinn vs village in Estonia)

### Covered by multiple grammars (i.e. ambiguous)

  * Pii (PI vs village in Estonia)
