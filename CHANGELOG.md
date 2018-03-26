# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## 2.0.6 - 2018-03-26

### Changed

- Help text formatting now treats multi-newlines as paragraph separators, while
  single newlines are still treated like spaces. Thanks @leomillon!

## 2.0.5 - 2018-03-20

### Changed

- Releasing to Maven Central in addition to Bintray. This is probably the only
  really externally visible change.

- Upgraded a bunch of dependencies, including gradlew.
    - gradle -> 4.5.1
    - dokka -> = 0.9.16
    - gradle_bintray -> = 1.8.0
    - gradle_release -> = 2.6.0
    - kotlin -> 1.2.30
    - xenocom -> 0.0.6

## 2.0.4 - 2018-01-18

### Added

- if the `programName` passed to `mainBody` is null, then the
  system property `com.xenomachina.argparser.programName` is used, if set.

- the `parseInto` method can be used as an inline alternative to `force`.

- [Issue #24](https://github.com/xenomachina/kotlin-argparser/issues/18):
 `default` can now accept a lambda, making it possible to defer computation of
  defaults until actually required.

### Changed

- all instances of `progName` have been renamed to `programName`.

- many dependencies, and the gradle wrapper, have been updated.

## 2.0.3 - 2017-06-12

### Fixed

- [Issue #18](https://github.com/xenomachina/kotlin-argparser/issues/18)

## 2.0.2 - 2017-06-12

### Fixed

- [Issue #19](https://github.com/xenomachina/kotlin-argparser/issues/19) by
  updating xenocom dependency to 0.0.5. Also updated kotlin to 1.1.2-5 for good
  measure.

## 2.0.1 - 2017-05-15

### Changed

- [Issue #14](https://github.com/xenomachina/kotlin-argparser/issues/14) —
  previously, automatic option naming would turn "camelCase" into
  "--camelCase". Now it is converted to "--camel-case".

- Likewise, positinal argument auto-naming used to convert "camelCase" into
  "CAMELCASE". Now it is converted to "CAMEL-CASE".

- Improve help formatting w/long program names

### Fixed

- [Issue #17](https://github.com/xenomachina/kotlin-argparser/issues/17) —
  specifying 0 for the columns should format help without line wrapping.
  Previously, this did not work as documented, and would instead wrap text in
  very narrow columns.

- [Issue #15](https://github.com/xenomachina/kotlin-argparser/issues/15)
  — make it possible to specify 'argName' on all variants of 'storing' and
  `adding`


## 2.0.0 - 2017-04-21

### Added

- `ArgParser.option` is now a public method, so it's possible to create many
  new option types that were not previously possible. The existing option types
  are all written in terms of `option`, so they can be used to get an idea of
  how it works.

- More tests have been added.

- Started using keepachangelog.com format for CHANGELOG.md

- Made minor improvements to release process

### Changed

- The `storing`, `adding` and `positionalList` methods of `ArgParser` have had
  their parameters slightly reordered to be consistent with the other methods.
  This is an incompatible change. The name(s) come first, if any, followed by
  `help`. Other parameters appear after `help`, with the `transform` function,
  if any, last. It is recommended that clients either pass the transform as a
  block (ie: with braces) or as a named parameter, as any future new parameters
  will necessarily change its position in the list.

- Delegate and DelegateProvider are now abstract classes with internal
  constructors. This makes it much easier for me to separate internal and
  public parts of their API. This is an incompatible change, however it
  shouldn't really affect you unless you were trying to implement `Delegate`,
  which was't supported to begin with.

- `default` methods on both `Delegate` and `DelegateProvider` are now extension
  methods.  This makes it possible to generalize the type when adding a
  default. This is most noticable when using a nullable value (or `null`
  itself) for the default, though may also be useful in other cases (eg: a
  "storing" that always produces a `Rectangle`, but you want the default to be
  a `Circle`.  The resulting delegate will be a `Delegate<Shape>`.)

- Registration of delegates now takes place at binding-time rather than
  construction time. This should be pretty indistinguishable from the old
  behavior unless you're creating delegates without binding them.

- Help formatting has been improved so that it's far less likely to wrap option
  names in the usage table.

- There have been numerous bugfixes, particularly around positionals


## 1.1.0 - 2017-03-09

### Added

- Auto-naming of options and positionals.
    - Each of the ArgParser methods that takes names and returns a Delegate<T> has
      an overload that takes no name, but returns a DelegateProvider<T>.

    - A DelegateProvider<T> has an `operator fun provideDelegate` that returns a
      Delegate<T>, using a name derived from the name of the property the
      DelegateProvider is being bound to.

### Removed

    - `addValidtator` is now deprecated. Use `addValidator` instead.

### Fixed

    - Removed documentation of `option` from `README.md`, as it is internal

    - Corrected spelling of `addValidator`. `addValidtator` is still there, but
      deprecated. It'll probably be removed in the next release, barring the
      addition of potato functionality.

## 1.0.2 - 2017-03-07

### Changed

  - Upgrade to Kotlin 1.1, extract xenocom package.

## 1.0.1 - 2017-01-30

### Fixed

  - Fix small bug where `runMain` didn't have a default value for `columns`
    parameter. (Now defaults to null.)

## 1.0.0 - 2017-01-27

### Added

  - Initial release
