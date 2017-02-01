# ![Kotlin --argparser](https://rawgit.com/xenomachina/kotlin-argparser/master/logo.svg)

[![Download](https://api.bintray.com/packages/xenomachina/maven/kotlin-argparser/images/download.svg) ](https://bintray.com/xenomachina/maven/kotlin-argparser/%5FlatestVersion)
[![Build Status](https://travis-ci.org/xenomachina/kotlin-argparser.svg?branch=master)](https://travis-ci.org/xenomachina/kotlin-argparser)
[![License: LGPL 2.1](https://img.shields.io/badge/license-LGPL--2.1-blue.svg) ](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html)

This is a library for parsing command-line arguments.  It can parse both
options and positional arguments.  It aims to be easy to use and concise yet
powerful and robust.


## Overview

The main class in this library is `ArgParser`. It handles the parsing of
command-line arguments, and also acts as a factory for creating property
delegates. These delegates help to keep client code clear and concise.

Typical usage is to create a class to represent the set of parsed arguments,
which are in turn each represented by properties that delegate to an
`ArgParser`:

```kotlin
class MyArgs(parser: ArgParser) {
    val verbose by parser.flagging("-v", "--verbose",
                                   help="enable verbose mode")

    val name by parser.storing("-N", "--name",
                               help = "name of the widget")

    val size by parser.storing("-s", "--size",
                               help = "size of the plumbus") { toInt() }
}
```


## Option Types

Various types of options can be parsed from the command line arguments:

- Boolean flags are created by asking the parser for a `flagging` delegate.  One
  or more option names, either short or long style, must be provided:

  ```kotlin
  val verbose by parser.flagging("-v", "--verbose",
                                 help = "enable verbose mode")
  ```

  Here the presence of either `-v` or `--verbose` options in the
  arguments will cause the `Boolean` property `verbose` to be `true`, otherwise
  it will be `false`.

- Single argument options are created by asking the parser for a
  `storing` delegate.

  ```kotlin
  val name by parser.storing("-N", "--name",
                             help = "name of the widget")
  ```

  Here either `-N` or `--name` with an argument will cause `name` to have that
  argument as its value.

  A function can also be supplied to transform the argument into the desired
  type. Here the `size` property will be an `Int` rather than a `String`:

  ```kotlin
  val size by parser.storing("-s", "--size",
                             help = "size of the plumbus") { toInt() }
  ```

- Options that add to a `Collection` each time they
  appear in the arguments are created with using the `adding` delegate. Just like `storing`
  delegates, a transform function may optionally be supplied:

  ```kotlin
  val includeDirs by parser.adding(
          "-I", help = "directory to search for header files") { File(this) }
  ```

  Now each time the `-I` option appears, its argument is appended to
  `includeDirs`.

- For choosing between a fixed set of values (typically, but not necessarily,
  from an enum), a `mapping` delegate can be used:

  ```kotlin
  val mode by parser.mapping(
          "--fast" to Mode.FAST,
          "--small" to Mode.SMALL,
          "--quiet" to Mode.QUIET,
          help = "mode of operation")
  ```

  Here the `mode` property will be set to the corresponding `Mode` value depending
  on which of `--fast`, `--small`, and `--quiet` appears (last) in the arguments.

- For the times when none of the above do what you need, the much more powerful
  `option` method can be used.  (The methods described above are convenience
  methods built on top of `option`.)

  ```kotlin
  val zaphod by parser.option(
          "--fibonacci",
          help = "collects fibonnaci sequence, remembers length") {
      var prev = 0
      var current = 1
      var result = 0
      while (peek() == current) {
          result++
          prev, current = current, current+prev
          next()
      }
      return result
  }
  ```

The delegates returned by any of these methods also have a few methods for setting
optional attributes:

- Some types of options (notably `storing` and `mapping`) have no
  default value, and hence will be required options unless a default
  value is provided. This is done with the `default` method:

  ```kotlin
  val name by parser.storing("-N", "--name", help="...")
          .default("John Doe")
  ```

- Sometimes it's easier to validate an option at the end pf parsing, in which
  case the `addValidator` method can be used.

  ```kotlin
  val percentages by parser.adding("--percentages", help="...") { toInt() }
          .addValidator {
                if (sum() != 100)
                    throw InvalidArgumentException(
                            "Percentages must add up to 100%")
          }
  ```


## Positional Arguments

Positional arguments are collected by using the `positional` and
`positionalList` methods.

For a single positional argument:

```kotlin
val destination by parser.positional("DEST",
                                     help="destination filename")
```

The name ("DEST", here) is used in error handling and help text.

For a list of positional arguments:

```kotlin
val sources by parser.positionalList("SOURCE", 1..Int.MAX_VALUE,
                                     help="source filename")
```

The range indicates how many arguments should be collected, and actually
defaults to the value shown in this example. As the name suggests, the
resulting property will be a `List`.

Both of these methods accept an optional transform function for converting
arguments from `String` to whatever type is actually desired:

```kotlin
val destination by parser.positional("DEST",
                                     help="...") { File(this) }

val sources by parser.positionalList("SOURCE", 1..Int.MAX_VALUE,
                                     help="...") { File(this) }
```


## Error Handling

Exceptions caused by user error will all derive from `SystemExitException`, and
include a status code appropriate for passing to `exitProcess`.  It is
recommended that transform functions (given to `storing`, `positionalList`, etc.)
throw a `SystemExitException` when parsing fails.

Additional post-parsing validation can be performed on a delegate using
`addValidtator`.

As a convenience, these exceptions can be handled by using the `runMain`
extension function:

```kotlin
fun main(args: Array<String>) =
        MyArgs(ArgParser(args)).runMain(PROGRAM_NAME) {
            println("Hello, {name}!")
        }
```

Note that parsing does not take place until at least one delegate is read, or
`force` is called manually. It may be desirable to call `force` on the parser
in the `init` of your args object after declaring all of your parsed
properties.


<!--
## Parsing

TODO: write a brief explanation of how parsing works


## Help Formatting

TODO: write an explanation of help formatting once implemented
-->


## Caveats

- This library should be considered to be *very beta*. While there are no plans
  to make any breaking changes to the API, it's possible that there may be some
  until it is mature.

- Upon reading the value any of the delegated properties created by an
  `ArgParser`, the arguments used to construct that `ArgParser` will be
  parsed. This means it's important that you don't attempt to create delegates
  on an `ArgParser` after any of its existing delegated properties have been
  read. Attempting to do so will cause an `IllegalStateException`. It would be
  nice if Kotlin has facilities for doing some of the work of `ArgParser` at
  compile time rather than run time, but so far the run time errors seem to be
  reasonably easy to avoid.


## Credits

This library was created by [Laurence Gonsalves](http://laurence.gonsalv.es).

I'd also like to thank the creators of Python's
[`argparse`](https://docs.python.org/3/library/argparse.html) module, which
provided the initial inspiration for this library.
