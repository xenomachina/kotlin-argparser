# Kotlin-ArgParser

This is a library for parsing command-line arguments.  It can parse both
options and positional arguments.  It aims to be easy to use and concise yet
powerful and robust.


## Overview

The main class in this library is `ArgParser`. It handles the parsing of
command-line arguments, and also acts as a factory for creating property
delegates. These delegates help to keep client code clear and concise.

Typical usage is to create a class to represent the set of parsed arguments,
which are in turn each represented by properties that delgate to an
`ArgParser`:

    class MyArgs(parser: ArgParser) {
        val verbose by parser.flagging("-v", "--verbose")

        val name by parser.storing("-N", "--name")

        val size by parser.storing("-s", "--size") { toInt() }
    }

## Option Types

There are various types of options that can be parsed from the command line
arguments.

Boolean flags are created by asking the parser for a `flagging` delegate.  One
or more option names, either short or long style, must be provided:

    val verbose by parser.flagging("-v", "--verbose")

Here the presence of either `-v` or `--verbose` options in the
arguments will cause the `Boolean` property `verbose` to be `true`, otherwise
it will be `false`.

Options that expect a single argument are created by asking the parser for a
`storing` delegate.

    val name by parser.storing("-N", "--name")

Here either `-N` or `--name` with an argument will cause `name` to have that
argument as its value.

A function can also be supplied to transform the argument into the desired
type. Here the `size` property will be an `Int` rather than a `String`:

    val size by parser.storing("-s", "--size") { toInt() }

It's also possible to create options that add to a `Collection` each time they
appear in the arguments using the `adding` delegate. Just like `storing`
delegates, a transform function may optionally be supplied:

    val includeDirs by parser.adding("-I") { File(this) }

Now each time the `-I` option appears, its argument is appended to
`includeDirs`.

For choosing between a fixed set of values (typically, but not necessarily,
from an enum), a `mapping` delegate can be used:

    val mode by parser.mapping(
            "--fast" to Mode.FAST,
            "--small" to Mode.SMALL,
            "--quiet" to Mode.QUIET)

Here the `mode` property will be set to the corresponding `Mode` value depending
on which of `--fast`, `--small`, and `--quiet` appears (last) in the arguments.

The methods described above are convenience methods built on top of the
`option` method.  For the times when none of them do what you need, the much
more powerful `option` method can be used directly, though it is harder to use
in these common cases.

    val zaphod by parser.option("--fibonacci") {
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
            .help("collects fibonnaci sequence, remembers length")

The Delegates returned by these methods also have a few methods for setting
optional attributes.

For example, some types of options (notably `storing` and `mapping`) have no
default value, and hence will be required options unless a default
value is provided with the `default` method:

    val name by parser.storing("-N", "--name")
            .default("John Doe")

Help text can also be provided through use of the `help` method:

    val verbose by parser.flagging("-v", "--verbose")
            .help("produce verbose output")

## Positional Arguments

Positional arguments can be collected by using the `positional` and
`positionalList` methods. For a single positional argument:

    val destination by parser.positional("DEST")

The name ("DEST", here) is used in error handling and help text.

For a list of positional arguments:

    val sources by parser.positionalList("SOURCE", 1..Int.MAX_VALUE)

The range indicates how many arguments should be collected, and actually
defaults to the value shown in this example. As the name suggests, the
resulting property will be a `List`.

Both of these methods accept an optionl transform function for converting
arguments from `String` to whatever type is actually desired:

    val destination by parser.positional("DEST") { File(this) }
    val sources by parser.positionalList("SOURCE", 1..Int.MAX_VALUE) { File(this) }

## Error Handling

Exceptions caused by user error will all derive from `SystemExitException`, and
include a status code appropriate for passing to `exitProcess`.  It is
recommended that transform functions (given to `storing`, `positionalList`, etc.)
throw a `SystemExitException` when parsing fails.

Additional post-parsing validation can be performed on a delegate using
`addValidtator`:

    val sources by parser.positionalList("SOURCE", 1..Int.MAX_VALUE) { File(this) }
        .addValidtator {
            for (source in value) {
                if (!source.exists()) {
                    throw SystemExitException("Cannot find file $source", 1)
                }
            }
        }

As a convenience you can handle these exceptions by using the `runMain`
extension function:

    fun main(args: Array<String>) =
            MyArgs(ArgParser(args)).runMain {
                println("Hello, {name}!")
            }

Note that parsing does not take place until at least one delegate is read, or
`force` is called manually. It may be desirable to call `force` on the parser
in the `init` of your args object after declaring all of your parsed
properties.

## Parsing

TODO: write a brief explanation of how parsing works

## Help Formatting

TODO: write an explanation of help formatting once implemented

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
  reasonalbly easy to avoid.

## Credits

This library was created by [Laurence Gonsalves](http://laurence.gonsalv.es).

I'd also like to thank the creators of Python's
[`argparse`](https://docs.python.org/3/library/argparse.html) module, which
provided the initial inspiration for this library.
