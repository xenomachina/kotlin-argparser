# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Changed

- Improve help formatting w/long program names

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

* [31m9978a26[m -[33m[m Remove defunct TODO [32m(4 hours ago)[1;34m<Laurence Gonsalves>[m
* [31m83976d8[m -[33m[m Upgrade kotlintest to 2.0.1 [32m(4 hours ago)[1;34m<Laurence Gonsalves>[m
* [31md45facd[m -[33m (origin/master)[m [Gradle Release Plugin] - new version commit:  '2.0.1-SNAPSHOT'. [32m(3 days ago)[1;34m<Laurence Gonsalves>[m
* [31m81255f1[m -[33m (tag: 2.0.0)[m Release 2.0.0 [32m(3 days ago)[1;34m<Laurence Gonsalves>[m
* [31m3016283[m -[33m[m Improve README.md [32m(3 days ago)[1;34m<Laurence Gonsalves>[m
* [31m40202f1[m -[33m[m Improve epilogue formatting [32m(3 days ago)[1;34m<Laurence Gonsalves>[m
* [31m8e91c69[m -[33m[m Remove unnecessary errorName args in tests [32m(3 days ago)[1;34m<Laurence Gonsalves>[m
* [31mf684782[m -[33m[m Make option's erroName nullable, add argName param [32m(3 days ago)[1;34m<Laurence Gonsalves>[m
* [31mc65cbca[m -[33m[m Remove completed TODO comments [32m(3 days ago)[1;34m<Laurence Gonsalves>[m
* [31med620c7[m -[33m[m Clean up DelegateProvider's API and add test [32m(3 days ago)[1;34m<Laurence Gonsalves>[m
* [31m39edfe8[m -[33m[m Reorder args in option method, rem auto-naming [32m(4 days ago)[1;34m<Laurence Gonsalves>[m
* [31m7f4c2af[m -[33m[m Add option and argument name validation [32m(4 days ago)[1;34m<Laurence Gonsalves>[m
* [31m096f3e3[m -[33m[m Update CHANGELOG in preparation for 2.0.0 release [32m(4 days ago)[1;34m<Laurence Gonsalves>[m
* [31ma0e384b[m -[33m (dev)[m Improve help DefaultHelpFormatter [32m(4 days ago)[1;34m<Laurence Gonsalves>[m
* [31m6519b75[m -[33m[m Add some TODO comments [32m(4 days ago)[1;34m<Laurence Gonsalves>[m
* [31m37355c4[m -[33m[m Replace runMain with mainBody [32m(4 days ago)[1;34m<Laurence Gonsalves>[m
* [31mea6a2e2[m -[33m[m Make option method public [32m(4 days ago)[1;34m<Laurence Gonsalves>[m
* [31m5311ab9[m -[33m[m Port tests to kotlintest [32m(6 days ago)[1;34m<Laurence Gonsalves>[m
* [31m1bc1474[m -[33m[m Change Delegate into abstract class [32m(3 weeks ago)[1;34m<Laurence Gonsalves>[m
* [31m1421ca3[m -[33m[m Use keepachangelog.com format for CHANGELOG.md [32m(6 weeks ago)[1;34m<Laurence Gonsalves>[m
* [31m3d83aea[m -[33m[m Set githubReleaseNotesFile in bintrayUpload target [32m(6 weeks ago)[1;34m<Laurence Gonsalves>[m
* [31mc8b78d5[m -[33m[m Upgrade dependencies to latest versions [32m(6 weeks ago)[1;34m<Laurence Gonsalves>[m
* [31md7bd412[m -[33m[m [Gradle Release Plugin] - new version commit:  '1.1.1-SNAPSHOT'. [32m(7 weeks ago)[1;34m<Laurence Gonsalves>[m
* [31mcff39a1[m -[33m (tag: 1.1.0)[m [Gradle Release Plugin] - pre tag commit:  '1.1.0'. [32m(7 weeks ago)[1;34m<Laurence Gonsalves>[m
* [31ma5fc265[m -[33m[m Update CHANGELOG.md for 1.1.0 [32m(7 weeks ago)[1;34m<Laurence Gonsalves>[m
* [31mc5b10bf[m -[33m[m Implement auto-naming of options and positionals [32m(7 weeks ago)[1;34m<Laurence Gonsalves>[m
* [31m5d0b6c7[m -[33m[m Update CHANGELOG.md [32m(7 weeks ago)[1;34m<Laurence Gonsalves>[m
* [31ma895f67[m -[33m[m [Gradle Release Plugin] - new version commit:  '1.0.3-SNAPSHOT'. [32m(7 weeks ago)[1;34m<Laurence Gonsalves>[m
* [31me6bf3f2[m -[33m (tag: 1.0.2)[m [Gradle Release Plugin] - pre tag commit:  '1.0.2'. [32m(7 weeks ago)[1;34m<Laurence Gonsalves>[m
* [31m17b5afc[m -[33m[m Upgrade to Kotlin 1.1, extract xenocom [32m(7 weeks ago)[1;34m<Laurence Gonsalves>[m
* [31m8c2b630[m -[33m[m Change bullets->subheads in Parsing README [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31mbb8d326[m -[33m[m Document parsing in README.md [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m2f309f2[m -[33m[m Don't use shields.io for Travis or bintray badges [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m5ebae7f[m -[33m[m Adjust logo page margins [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m3a5f698[m -[33m[m Fix rawgit logo link in README [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m373deeb[m -[33m[m Use 3-backticks for code blocks in README [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m77c408a[m -[33m[m Use Commodore 64 font instead of Teko in logo [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31meafa449[m -[33m[m Cleanup README.md [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m12fd108[m -[33m[m Work around Github bug 316, attempt #2 [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m3f395e6[m -[33m[m Work around github's local SVG in README bug [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m8056d76[m -[33m[m Tweak README images [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m953d7d0[m -[33m[m Add .travis.yml as per https://docs.travis-ci.com/ [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m6da5957[m -[33m[m Add CHANGELOG.md [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31mca771ac[m -[33m[m [Gradle Release Plugin] - new version commit:  '1.0.2-SNAPSHOT'. [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m0e101f2[m -[33m (tag: 1.0.1)[m [Gradle Release Plugin] - pre tag commit:  '1.0.1'. [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m568ceb8[m -[33m[m Make columns param of runMain default to null [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m119a7e4[m -[33m[m Bump kotlin_version to latest stable: 1.0.6 [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m0a91398[m -[33m[m Enable `gradle install` for local maven repo [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m2d2a874[m -[33m[m Add download and license badges to README.md [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m86e5e26[m -[33m[m [Gradle Release Plugin] - new version commit:  '1.0.1-SNAPSHOT'. [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m33c3fd9[m -[33m (tag: 1.0.0)[m [Gradle Release Plugin] - pre tag commit:  '1.0.0'. [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m1b44294[m -[33m[m Add ktlint to gradle check rule [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m67f2b93[m -[33m[m Fix ktlint errors [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31mfd8c6d8[m -[33m[m Add KDoc comments wherever missing [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m1abd970[m -[33m[m Generate dokka-style docs in addition to Javadocs [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m14667a6[m -[33m[m Add logo to README [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m5d17851[m -[33m[m Test prologue and epilogue of DefaultHelpFormatter [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31mfced0cc[m -[33m[m Move arg parsing exceptions to ArgParser.kt [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31md36ddd3[m -[33m[m Rename valueName -> errorName, hide some internals [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31mbb291a4[m -[33m[m Reformat code (automated) [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31ma31dbdb[m -[33m[m Update README to reflect mandatory help argument [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m4841c6f[m -[33m[m Add comments explaining all uses of !! [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31mb78df45[m -[33m[m Make help mandatory for all delegates [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m1764842[m -[33m (help-formatting)[m Implement word-wrapping and columnizing of --help [32m(3 months ago)[1;34m<Laurence Gonsalves>[m
* [31m006db50[m -[33m[m Clean up dokka gradle tasks [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mdb1e312[m -[33m[m Nest ShowHelpException in ArgParser [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mf1b7332[m -[33m[m Factor out printUserMessage in SystemExitException [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m10715fc[m -[33m[m Rename toValueHelp to toHelpFormatterValue [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m33b3007[m -[33m[m Pass columns down to HelpFormatter [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m110de31[m -[33m[m Use .mod instead of % to avoid warnings [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mbaf39ad[m -[33m[m Remove TODOs that now have issues [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mc22a8c7[m -[33m[m Remove unused dependency on org.apache.commons [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m726d154[m -[33m[m Remove debugging println from build.gradle [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m940f208[m -[33m[m Fix bugs in bintrayUpload gradle task [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m8d5fd21[m -[33m[m Add dokka, bintray, and gradle-release support [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m54a501e[m -[33m[m Fix some missed renamings in header comments [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m1d699fd[m -[33m[m Clean up README [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m5022fad[m -[33m[m Make ArgParser.force public [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m4e45fab[m -[33m[m Rename OptionParser -> ArgParser [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mea38d8e[m -[33m[m Clean up TODO comments [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m9a57e78[m -[33m[m Clean up HelpFormatter API [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m24e2134[m -[33m[m Implement --help support [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m561f671[m -[33m[m Remove bogus import [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31md0b7000[m -[33m[m Change shouldThrow to use a reified type parameter [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31meb6a106[m -[33m[m Change shouldThrow to use a KClass [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mfcd2800[m -[33m[m Add some testing TODOs [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m9712722[m -[33m[m Rewrite the docs for the Delegate factory methods [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mc25bc9e[m -[33m[m Add OptionParser.counting [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m7196f59[m -[33m[m Check sizeRange for validity [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m2a06173[m -[33m (simple-positional)[m Reformat code (mostly automated) [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m5b6b936[m -[33m[m Add POSIX/GNU modes [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m8134eee[m -[33m[m Add support for "--" [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mf08dd49[m -[33m[m Finish implementing positional arguments [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m51b0d22[m -[33m[m Start tests of argument[List], add overloads [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mf4f06ab[m -[33m[m Add OptionParser.argument & argumentList [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m37b3e82[m -[33m[m Split Delegate into interface and subclasses [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m251d609[m -[33m[m Move Delegate.Input out to OptionArgumentIterator [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mc98130e[m -[33m[m Add Delegate.addValidator [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m74084f4[m -[33m[m Implement Input.peek and Input.hasNext [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m582ff87[m -[33m[m Clean up OptionParser.Delegate.Input properties [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m9661b66[m -[33m[m Throw UnrecognizedOptionException when appropriate [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m27ceeca[m -[33m[m Disallow creation/mutation of Delegates post-parse [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m7325492[m -[33m[m Clean up TODOs [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m4285ae3[m -[33m[m Change shouldThrow usages to use run [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mee2a566[m -[33m[m Use shouldThrow in place of JUnit's "thrown" [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m78864b2[m -[33m[m Import assertEquals/True/False directly [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m13f51fa[m -[33m[m Add testInitValidation and shouldThrow [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m1bace37[m -[33m[m Add OptionParser.force, move checking out of lazy [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m12fd091[m -[33m[m Add OptionParserTest.parserOf [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mb6f172e[m -[33m[m Correct variance of args arrays [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m4061152[m -[33m[m Remove progName from SystemExitException + parser [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m41d9f1d[m -[33m[m Make SystemExitExceptions all open classes [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m6d6d273[m -[33m[m Rename inline test option classes to "Opts" [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m0e7f942[m -[33m[m Add OptionMissingRequiredArgumentException [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m81e5012[m -[33m[m Test unrecognized options [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31me944053[m -[33m[m Test mapping missing with and without default set [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mf21149d[m -[33m[m Add testArgument_missing_withParser [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m9f8b3ee[m -[33m[m Test chained options [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31me179d34[m -[33m[m Rename arg(ument) to value in a few places [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m5096439[m -[33m[m Throw MissingArgumentException when appropriate [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31md108c3f[m -[33m[m Add testLongOptionsWithConcatenatedArgs [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mf3ab7f2[m -[33m[m Add README.md [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mab845dc[m -[33m[m Cleaned up docs of OptionParser methods [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mb20ec88[m -[33m[m Remove `help` param to delegate factory methods [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mdf8c7a3[m -[33m[m Rename UserErrorException to SystemExitException [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31me3fe674[m -[33m[m Close OptionParser class [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m57417a9[m -[33m[m Inline some OptionParser methods [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m1436cb8[m -[33m[m Implement OptionParser.mapping [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mcd84906[m -[33m[m Stop saying "this" in parser funcs [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m236d3df[m -[33m[m Clean up some exception messages [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31mea536a8[m -[33m[m Begin cleanup of UserErrorExceptions [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m9e63cdd[m -[33m[m Include hyphen in name of short options [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m095c08d[m -[33m[m Include gradle sources in wrapper [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m193d102[m -[33m[m License everything under LGPL v2.1 [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m7b64264[m -[33m[m Add progName to OptionParser & UserErrorException [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m411d092[m -[33m[m Rename action->option, Action->Delegate [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31me0f8894[m -[33m[m Fix indentation of commented-out adding overload [32m(4 months ago)[1;34m<Laurence Gonsalves>[m
* [31m9c57af5[m -[33m[m Move OptionParser.Exception -> UserErrorException [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m414ddc1[m -[33m[m Move NAME_EQUALS_VALUE_REGEX into static field [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m6d1c3d0[m -[33m[m Use "when" for parseOption dispatching [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m6c35094[m -[33m[m Clean up parseShortOptions and parseLongOption [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m274fa25[m -[33m[m Rename {short,long}Flags to {short,Long}Options [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m6cd5275[m -[33m[m Change Input to work like an iterator [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m74c56f8[m -[33m[m Clean up docs for OptionParser.default() [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31maef9ec7[m -[33m[m Rename parser methods to flagging/storing/adding [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m0b160ed[m -[33m[m Implement runMain, clean up main example docs [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m0835fa8[m -[33m[m Add some TODOs [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m179b05e[m -[33m[m Implement OptionParser.accumulator [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31mf77b45d[m -[33m[m Test OptionParser.argument() [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31mf5ce626[m -[33m[m Test OptionParser.flag() [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m55d9b66[m -[33m[m Fix test names: Values -> Args [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m088ef28[m -[33m[m Fix test names: Flags -> Options [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m211b9dc[m -[33m[m Implement flag, argument, accumulator, printAndExit [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m3981997[m -[33m[m Improve docs; use composition not inheritance [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31me7dbe00[m -[33m[m Rename arg(ument)->opt(ion) and value->argument [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m4b1e630[m -[33m[m Do a bunch of cleaning up and rearranging [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m3d4d43e[m -[33m[m Make passing in of option arguments null safe [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31me5286a0[m -[33m[m Add Action.default for setting default values [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31maa24884[m -[33m[m Add messages to TODO() calls [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m962d86e[m -[33m[m Test (and fix) long flags with values [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31mb8e3685[m -[33m[m Test valueless long flags [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m83b56ed[m -[33m[m Test mixed short flags with values [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31md5dbfa8[m -[33m[m Test (and fix) parsing of mixed short flags [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31mfb6573f[m -[33m[m Get short flags with values working [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m88915bc[m -[33m[m Test parsing of chained (valueless) short flags [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m8d229b3[m -[33m[m Implement parsing of valueless short flags [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m744649f[m -[33m[m Create signatures for core plumbing [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
* [31m08a8b8f[m -[33m[m Start creating ArgParser [32m(5 months ago)[1;34m<Laurence Gonsalves>[m
