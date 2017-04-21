# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

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
