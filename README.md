# Language Detection plugin for GATE

This is a plugin for the [GATE](https://gate.ac.uk) framework that uses the [Optimaize language detector](https://github.com/optimaize/language-detector) library to identify the language in which a GATE document is written.  By default the language detection PR uses the 70 language profiles that are built in to the Optimaize library, but this list may be refined and/or additional profiles added via CREOLE parameters.

The PR can operate in two modes; by default it will classify the document as a whole and store the most probable language as a _document_ feature, but alternatively it may be configured with an annotation set name and annotation type in which case it will classify the span of text covered by each such annotation separately, and store the result as an annotation feature.

## Configuration

### Init-time parameters

- `textType` - `LONG` or `SHORT` - the default Optimaize language models are designed to classify relatively long texts (several paragraphs or longer), but the detector comes with special models more suited to shorter texts.  The short text models are only available for a subset of languages, so when the `textType` parameter is set to `SHORT` the PR will load the "short text" models for languages where such exist, but use the regular longer text models for the remaining languages.  The `LONG` mode also enables some basic pre-processing to improve the accuracy of the prediction by ignoring characters that are in a script that makes up less than 30% of the text being considered (e.g. if a document is primarily Cyrillic but has a small number of Latin alphabet words then those Latin characters would be ignored).  This pre-processing is not performed in the `SHORT` mode.
- `loadBuiltInProfiles` and `builtInLanguages` - these parameters control which of the built in language profiles will be loaded.  The `builtInLanguages` parameter may be set to a list of language codes in order to load only those languages.  If the parameter is unset or is set to an empty list, then all 70 default profiles will be loaded, _unless_ `loadBuiltInProfiles` is set to false (which disables loading of any built in profiles).  So the interaction between these parameters is as follows:
  - `loadBuiltInProfiles` = true, `builtInLanguages` is unset or empty: load _all_ built-in profiles (this is the default setting)
  - `loadBuiltInProfiles` = true, `builtInLanguages` is non-empty: load just the specified profiles
  - `loadBuiltInProfiles` = false: do not load any built in profiles at all
- `extraProfiles` provides a way to load custom profiles that you have [trained yourself](https://github.com/optimaize/language-detector/wiki/Creating-Language-Profiles) or obtained from third parties.  The parameter works in a similar way to the ANNIE gazetteer - the parameter value should point to a plain text file that lists the (relative or absolute) URLs to one or more individual profile files.  Blank lines in the list file and lines starting with a `#` character are ignored.

### Runtime parameters

- `annotationType` - if specified, the PR will treat the document as a list of segments covered by the specified annotation type (specify `annotationSetName` if the annotations are not in the default set), and will run the language detector over the text span covered by each annotation in turn, storing the detected language as a feature of the _annotation_.  If `annotationType` is not set the whole document will be classified as one unit, with the detected language stored as a feature of the _document_.
- `languageFeatureName` - the name of the feature (on the annotation or document) under which the detected language should be stored
- `unknownValue` - by default, if the detector does not find any language whose probability meets its minimum threshold then it will not set the language feature at all.  Setting this `unknownValue` parameter changes this behaviour so that the feature is _always_ added, but if the detector is unsure then it will use the specified value.  This parameter can be used in two ways; you can specify a special code like "unk" to tag the unknown instances explicitly, or you can specify one of the real language codes to act as a fallback (e.g. classify all unknown documents as if they were English).

## Licensing

This plugin itself is licensed under the GNU Lesser General Public License version 3 or later.  The underlying language detector library is Apache Licensed.