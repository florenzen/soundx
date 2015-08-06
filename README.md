# SoundX - type-sound syntactic language extensibility

SoundX is a language independent framework for type-sound syntactic language extensions, which is currently under development. It is implemented as a language library for the [Sugar*](https://www.sugarj.org/) Eclipse plugin.

SoundX takes a base language definition, which describes the syntax and the type system of the base language, and derives a syntactically extensible dialect of that base language. In the extensible dialect, extensions are implemented by declaring their syntax, their typing rules, and the desugaring into the base language. Extensions are defined in libraries (modules of the base language) and activated by importing them. Extensions are very flexible and ll syntactic sorts of the base language like terms, types, or declarations can extended.

SoundX verifies that an extension desugars into well-typed code and type checks extended code prior to desugaring. This guarantees that there are never type errors in generated code and greatly improves the robustness of extensions.

SoundX poses only very mild requirements on the module system of the base language and no requirements on the type system at all. For the base language definition, the type system must be specified by judgements inductively defined by inference rule schema.

This repository contains the SoundX plugin including the plugin to edit base language definitions in Eclipse. Examples of base language definitions are available are available in the repository [soundx-base-language-definitions](https://github.com/florenzen/soundx-base-language-definitions) and the repository [soundx-experiments](https://github.com/florenzen/soundx-experiments) contains several case studies.

The implementation is a research prototype which realises the concepts of my upcoming PhD thesis. If you are interested in using SoundX please contact me for installation directions or any further information.
