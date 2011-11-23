Arvutaja
========

__Arvutaja__ (= the Estonian word for _Calculator_) is an Android app that
offers a new way for:

  * converting units and currencies,
  * evaluating arithmetical expressions,
  * performing Estonian address queries,

where the query is input via __Estonian speech__.

Features
--------

  * __No complex menus!__ Just a microphone button.
  * __Tap once!__ Tapping the widget launches the recognizer, which records the speech, turns it into text, which gets evaluated, with no additional taps needed to see the result.
  * The __history__ of query evaluation results is presented in a persistent list.
  * Support for __vague/ambiguous queries__ (e.g. "viis norra krooni suures valuutas").
  * Clicking on a list item sends the query to a 3rd party app. This is mostly used for
    * map queries the result of which cannot be presened in the list;
    * currency conversion (which the internal evaluator does not support yet);
    * getting a "2nd opinion" (from e.g. Google Search or WolframAlpha).


Dependencies
------------

__Arvutaja__ uses grammar-aware Estonian speech recognition service for Android
which you have to install separately from
http://recognizer-intent.googlecode.com


Background technologies
-----------------------

### Estonian speech recognition

__Arvutaja__ uses an online grammar-aware Estonian speech recognition server

  * http://bark.phon.ioc.ee/

### GF-based speech recognition

Grammatical Framework (GF) is used by the server to guide speech recognition
and to transform the
raw recognition result into an evaluatable form.

  * GF: http://www.grammaticalframework.org/
  * Estonian GF grammars: http://kaljurand.github.com/Grammars/

### Unit conversion

  * Unit conversion powered by jscience-4.3.jar (unit-api-0.6.0.jar)

### Arithmetical expressions

  * http://www.softwaremonkey.org/Code/MathEval


Examples
--------

The language understood by __Arvutaja__ is described by 3 underlying grammars.
Following is a list of some interesting examples, more can be found at
http://kaljurand.github.com/Grammars/

### Unitconv

  * sada meetrit sekundis kilomeetrites tunnis
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
