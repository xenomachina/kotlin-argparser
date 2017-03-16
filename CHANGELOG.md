# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Changed

- Started using keepachangelog.com format for CHANGELOG.md

- Made minor improvements to release process

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
