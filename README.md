Arvutaja
========

[![Codacy Badge](https://api.codacy.com/project/badge/grade/901a08be062d4d32b504a6c7f3905b95)](https://www.codacy.com/app/kaljurand/Arvutaja)

__Arvutaja__ (= the Estonian word for _the one that computes_) is a voice actions app for Android,
i.e. it converts a spoken utterance
in some natural language to an expression/command in some formal language and then
evaluates/executes the formal expression.

Currently supported input languages:

  - Estonian
  - English

Currently supported expressions and commands:

  - measurement unit conversion expression
  - currency conversion expression
  - arithmetical expression
  - alarm clock / timer setting command
  - phone number
  - Estonian address query (only with Estonian language input)
  - Estonian weather query (only with Estonian language input)

The expressions/commands are evaluated using an external app, such as

  - device's built-in alarm clock app
  - device's built-in phone app
  - a maps app
  - a browser loading the WolframAlpha website

The arithmetical and measurement unit conversion expressions are also evaluated by Arvutaja itself.

Arvutaja uses grammar-based speech recognition and is largely defined by the Action-grammar developed in the
separate Grammars-project (<http://kaljurand.github.io/Grammars/>). This grammar defines
which input languages and expressions are supported.

Download
--------

There are currently two versions of Arvutaja. They use the same underlying grammar but
differ in their UIs and how they use the speech recognizer.

  - v0.5+, requires Android v4+
  - v0.4, requires Android v1.6+ (not developed anymore)

APK-packages for both versions are available via [Google Play](https://play.google.com/store/apps/details?id=ee.ioc.phon.android.arvutaja)
and [GitHub Releases](https://github.com/Kaljurand/Arvutaja/releases).


Building from source
--------------------

    git clone --recursive git@github.com:Kaljurand/Arvutaja.git
    cd Arvutaja
    gradle assembleRelease

Features
--------

  * __No complex menus!__ Just a microphone button.
  * The __history__ of query evaluation results is presented in a persistent list.
  * Support for __vague/ambiguous queries__ (e.g. "viis norra krooni suures valuutas").
  * Clicking on a list item sends the query to a 3rd party app. This is mostly used for
    * map queries the result of which cannot be presented in the list;
    * currency conversion (which the internal evaluator does not support yet);
    * getting a "2nd opinion" (from e.g. Google Search or WolframAlpha).

The main difference between Arvutaja and other intelligent assistant / voice actions apps like Google Now
and Siri is that Arvutaja is largely defined by a human-readable multilingual grammar.
Also, Arvutaja is open and modular by

  - using open source grammars (i.e. users can find out exactly which input phrases are supported),
  - not depending on any particular speech recognition server,
  - having an open source code.

Dependencies on other apps
--------------------------

For speech recognition, Arvutaja can technically use any Android speech recognizer (chosen via the Arvutaja settings).
However, it is recommended to use Kõnele, a grammar-aware speech recognition service for Android.
You have to install it separately from <http://kaljurand.github.io/K6nele/>, or from:

  - https://f-droid.org/repository/browse/?fdid=ee.ioc.phon.android.speak
  - https://play.google.com/store/apps/details?id=ee.ioc.phon.android.speak

In order to execute some actions, Arvutaja expects the device to contain

  - a web browser;
  - an app that understands Google Maps' URLs (`http://maps.google.com/maps?...`);
  - an app that responds to the standard Android alarm clock intent (`android.intent.action.SET_ALARM`).

In order to read back the input query, Arvutaja uses the system default text-to-speech (TTS) engine, setting it to the same language as the input query. Many TTS engines with support for different languages are available for Android. They can be installed e.g. via Google Play and set as system default in the Android language settings.
An Estonian TTS engine for Android is available e.g. on <http://heli.eki.ee/koduleht/index.php/rakendused> ("HTS-sünteeshääl Androidile EKISpeak.apk"). (Note that EKISpeak.apk v1.0 does not support numbers in the digit form, e.g. "üks pluss kaks on 3" is not rendered to speech at all.) Two modifications (which do not fix the "digit" problem) of this app are:

  - https://github.com/Kaljurand/EKISpeak
  - https://play.google.com/store/apps/details?id=ee.eki.heli.EKISpeak

Used libraries
--------------

  - Unit conversion powered by <http://jscience.org> (`jscience-4.3.jar` and `unit-api-0.6.0.jar`)
  - Evaluation of arithmetical expressions powered by <http://www.softwaremonkey.org/Code/MathEval>

Examples (Estonian)
-------------------

Following is a list of some input examples, more can be found at
<http://kaljurand.github.io/Grammars/>.

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
