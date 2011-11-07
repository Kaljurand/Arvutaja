Calculator
==========

_Work in progress_

*Calculator* is a novel
unit/currency conversion and arithmetical expression evaluator app for Android
(that also does Estonian address queries).

Features:

  * No menus! The query is entered via *Estonian speech*
  * The *history* of query evaluation results is presented in a persistent list
  * Support for *vague/ambiguous queries* (e.g. "viis norra krooni suures valuutas")
  * Clicking on a list item opens a 3rd party app for a 2nd opinion
    * (this is mostly used for map queries the result of which cannot be presened in the list)


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

The language understood by *Calculator* is composed of 3 underlying grammars.

Unitconv:

  * kaks sada tuhat viis teist milli kraadi kraadides
  * kaks minutit sekundites (NB! ambiguous with 2 readings)
  * kaks hektarit ruut kilo meetrites
  * kolm hektarit aakrites (ERROR: internal converter does not understad `acre`)
  * viis norra krooni vanas rahas (ERROR: internal converter does not understad `NOK`)
  * viis norra krooni suures valuutas (NB! ambiguous with ~6 readings)

Exp:

  * Pii korda miinus kaks jagatud pool teist
  * miinus üks miinus miinus kaks miinus miinus kolm ... (arbitrarily long query)
  * null astmel miinus üks

Direction:

  * Algus Sõpruse puiestee sada kakskümmend kolm Lõpp Vabaõhumuuseumi tee neli kümmend viis
  * Roo (Ambiguous: street in Tallinn vs village in Estonia)
