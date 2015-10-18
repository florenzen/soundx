# SoundX - type-sound syntactic language extensibility

SoundX is a language independent framework for type-sound syntactic language extensions, which is currently under development. It is implemented as a language library for the [Sugar*](https://www.sugarj.org/) Eclipse plugin.

SoundX takes a base language definition, which describes the syntax and the type system of the base language, and derives a syntactically extensible dialect of that base language. In the extensible dialect, extensions are implemented by declaring their syntax, their typing rules, and the desugaring into the base language. Extensions are defined in libraries (modules of the base language) and activated by importing them. Extensions are very flexible and all syntactic sorts of the base language like terms, types, or declarations can extended.

SoundX verifies that an extension desugars into well-typed code and type checks extended code prior to desugaring. This guarantees that there are never type errors in generated code and greatly improves the robustness of extensions.

SoundX poses only very mild requirements on the module system of the base language and no requirements on the type system at all. For the base language definition, the type system must be specified by judgements inductively defined by inference rule schema.

This repository contains the SoundX plugin including the plugin to edit base language definitions in Eclipse. SoundX has been applied to two base languages with several extensions each:

- [Simply typed lambda calculus](https://github.com/florenzen/soundx-base-language-definitions/tree/master/lang-stlcweak/src/STLCWeak.sxbld)

  Extensions:
  - [Let-expressions](https://github.com/florenzen/soundx-experiments/blob/master/TestSTLCWeak/src/LetSeqExtension.xstw)
  - [Pairs](https://github.com/florenzen/soundx-experiments/blob/master/TestSTLCWeak/src/PairExtension.xstw)
  - [Pattern matching for pairs](https://github.com/florenzen/soundx-experiments/blob/master/TestSTLCWeak/src/LetPairExtension.xstw)

- [JavaLight](https://github.com/florenzen/soundx-base-language-definitions/tree/master/lang-javalight/src/JavaLight.sxbld)

  Extensions:
  - [Enhanced for-loops](https://github.com/florenzen/soundx-experiments/blob/master/TestJavaLight/ForComprehensions/src/extensions/EnhancedForStatement.xjl)
  - [For-comprehensions](https://github.com/florenzen/soundx-experiments/blob/master/TestJavaLight/ForComprehensions/src/extensions/ForComprehensions.xjl)
