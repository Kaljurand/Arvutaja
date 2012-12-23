Arvutaja
========

__Arvutaja__ (= the Estonian word for _the one that computes_) is an Android app that converts a spoken utterance
in some natural language to an expression/command in some formal language and evaluates the formal expression
using another app on the device.

Currently supported input languages:

  - Estonian
  - English (experimental)

Currently supported expressions and commands:

  - measurement unit conversion expression
  - currency conversion expression
  - arithmetical expression
  - alarm clock / timer setting command
  - phone number
  - Estonian address query (only with Estonian language input)

The expressions/commands are evaluated using an external app, such as

  - WolframAlpha website
  - device's built-in alarm clock app
  - device's built-in phone app
  - Google Maps

The arithmetical and measurement unit conversion expressions are also evaluated by Arvutaja itself.

Arvutaja uses grammar-based speech recognition and is largely defined by the Action-grammar developed in the
separate Grammars-project (<http://kaljurand.github.com/Grammars/>). This grammar defines
which input languages and expressions are supported.

Download
--------

There are currently two versions of Arvutaja, which differ in their UIs and how they use the speech recognizer.

  - [v0.5.06](https://s3-eu-west-1.amazonaws.com/arvutaja/apk/Arvutaja-0.5.06.apk), requires Android v4+
  - [v0.4.06](https://s3-eu-west-1.amazonaws.com/arvutaja/apk/Arvutaja-0.4.06.apk)

Version v0.4.06 is also available on Google Play.


Features
--------

  * __No complex menus!__ Just a microphone button.
  * __Tap once!__ Tapping the widget launches the recognizer, which records the speech, turns it into text, which gets evaluated, with no additional taps needed to see the result.
  * The __history__ of query evaluation results is presented in a persistent list.
  * Support for __vague/ambiguous queries__ (e.g. "viis norra krooni suures valuutas").
  * Clicking on a list item sends the query to a 3rd party app. This is mostly used for
    * map queries the result of which cannot be presented in the list;
    * currency conversion (which the internal evaluator does not support yet);
    * getting a "2nd opinion" (from e.g. Google Search or WolframAlpha).


Comparison to other apps
------------------------

The main differences between Arvutaja and other intelligent assistant / speech-input based apps like Google Now
and Siri are that Arvutaja

  - supports Estonian;
  - is largely defined by a human-readable grammar, i.e. users can find out exactly which input phrases are supported;
  - is entirely open by
    - using an open source speech recognition server,
    - using open source grammars,
    - having an open source code.


Dependencies on other apps
--------------------------

__Arvutaja__ uses grammar-aware Estonian speech recognition service for Android Kõnele
which you have to install separately from either of the following URLs

  - http://recognizer-intent.googlecode.com
  - https://play.google.com/store/apps/details?id=ee.ioc.phon.android.speak


Background technologies
-----------------------

### Speech recognition

__Arvutaja__ uses an online grammar-aware Estonian-aware speech recognition server

  - http://bark.phon.ioc.ee/speech-api/v1/

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


Examples (Estonian)
-------------------

The language understood by __Arvutaja__ is described by several underlying grammars.
Following is a list of some interesting examples, more can be found at
http://kaljurand.github.com/Grammars/

### Alarm

  - ärata mind (palun) viis minutit hiljem
  - ärata mind (palun) (kell) seitse null viis

### Unitconv

  * sada meetrit sekundis kilomeetrites tunnis
  * kaks sada tuhat viis teist milli kraadi kraadides
  * kaks hektarit ruut kilo meetrites
  * kolm hektarit aakrites (ERROR: internal converter does not understad `acre`)
  * viis norra krooni vanas rahas (ERROR: internal converter does not understand `NOK`)

#### Examples of ambiguities

  * kaks minutit sekundites (ambiguous with 2 readings)
  * viis norra krooni suures valuutas (ambiguous with ~6 readings)

### Expr

  * Pii korda miinus kaks jagatud pool teist
  * miinus üks miinus miinus kaks miinus miinus kolm ... (arbitrarily long query)
  * null astmel miinus üks (= Infinity)
  * miinus üks astmel pool (= NaN, the built-in math evaluator does not support complex numbers)
    * Note: Google and WolframAlpha interpret "-1^0.5" as "-(1^0.5)"
    * Note: WolframAlpha interprets "minus one to the power of half" as "(-1)^0.5"

### Direction

  * Sõpruse puiestee sada kakskümmend kolm
  * Algus Sõpruse puiestee sada kakskümmend kolm Lõpp Vabaõhumuuseumi tee neli kümmend viis
  * Tartu

#### Examples of ambiguities

  * Roo (street in Tallinn vs village in Estonia)

### Covered by multiple grammars (i.e. ambiguous)

  * Pii (PI vs village in Estonia)
