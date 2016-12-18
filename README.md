# Kotlin-OptionParser

This is a library for parsing command-line options/arguments. It aims to be
easy to use and concise yet powerful and robust.

In addition to parsing arguments, it can also help with parsing non-option
arguments (that is, positional arguments).

## Overview

The main class in this library is `OptionParser`. It handles the parsing of
command-line arguments, and also acts as a factory for creating property
delegates. These delegates help to keep client code clear and concise.

Typical usage is to create a class to represent the set of parsed arguments,
which are in turn each represented by properties that delgate to an
`OptionParser`:

    class MyOptions(parser: OptionParser) {
        val verbose by parser.flagging("-v", "--verbose")

        val name by parser.storing("-N", "--name")

        val size by parser.storing("-s", "--size") { toInt() }
    }

## Delegate Types

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

A parsing function can also be supplied. Here the `size` property will be an
`Int` rather than a `String`:

    val size by parser.storing("-s", "--size") { toInt() }

It's also possible to create options that add to a `Collection` each time they
appear in the arguments using the `adding` delegate. Just like `storing`
delegates, a parsing function may optionally be supplied:

    val includeDirs by parser.adding("-I") { File(this) }

Now each time the `-I` option appears, its argument is appended to `includeDirs`.

For choosing between a fixed set of values (typically, but not necessarily, from an
enum), a `mapping` delegate can be used:

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

Delegates also have a few methods for setting optional attributes.

For example, some types of options (notably `storing` and `mapping`) have no
default value, and hence will be required options. To make them optional, a default
value can be provided with the `default` method:

    val name by parser.storing("-N", "--name")
            .default("John Doe")

Help text can also be provided through use of the `help` method:

    val verbose by parser.flagging("-v", "--verbose")
            .help("produce verbose output")

TODO: positional parameters

Exceptions caused by user error will all derive from `UserErrorException`, and
include a status code appropriate for passing to `exitProcess`. As a
convenience you can handle these exceptions by using the `runMain` extension
function:

    fun main(args: Array<String>) =
            MyOptions(OptionParser(args)).runMain {
                println("Hello, {name}!")
            }


## Parsing

TODO: write a brief explanation of how parsing works

## Help Formatting

TODO: write an explanation of help formatting once implemented

## Caveats

- This library should be considered to be *very beta*. While there are no plans
  to make any breaking changes to the API, it's possible that there may be some
  until it is mature.

- Upon reading the value any of the delegated properties created by an
  `OptionParser`, the arguments used to construct that `OptionParser` will be
  parsed. This means it's important that you don't attempt to create delegates
  on an `OptionParser` after any of its existing delegated properties have been
  read. Attempting to do so will cause an `IllegalStateException`. It would be
  nice if Kotlin has facilities for doing some of the work of `OptionParser` at
  compile time rather than run time, but so far the run time errors seem to be
  reasonalbly easy to avoid.

## Credits

This library was created by [Laurence Gonsalves](http://laurence.gonsalv.es),
aka [xenomachina](http://xenomachina.com/).

I'd also like to thank the creators of Python's
[`argparse`](https://docs.python.org/3/library/argparse.html) module, which
provided the initial inspiration for this library.
