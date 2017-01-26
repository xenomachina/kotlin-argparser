// Copyright © 2016 Laurence Gonsalves
//
// This file is part of kotlin-argparser, a library which can be found at
// http://github.com/xenomachina/kotlin-argparser
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by the
// Free Software Foundation; either version 2.1 of the License, or (at your
// option) any later version.
//
// This library is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
// for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this library; if not, see http://www.gnu.org/licenses/

package com.xenomachina.argparser

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.StringWriter


class ArgParserTest {
    @JvmField @Rule
    val testName: TestName = TestName()

    inline fun <reified X : Throwable> shouldThrow(f: () -> Unit): X {
        val javaClass = X::class.java
        try {
            f()
        } catch (exception: Throwable) {
            if (javaClass.isInstance(exception)) return javaClass.cast(exception)
            throw exception
        }
        throw AssertionError("Expected ${javaClass.canonicalName} to be thrown")
    }

    fun parserOf(vararg args: String, mode: ArgParser.Mode = ArgParser.Mode.GNU) =
            ArgParser(args, mode)

    @Test
    fun testArglessShortOptions() {
        class Args(parser: ArgParser) {
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z",
                    valueName = "VALUE_NAME",
                    usageArgument = "ARG_NAME") {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
        }

        assertEquals(
                listOf("-x", "-y", "-z", "-z", "-y"),
                Args(parserOf("-x", "-y", "-z", "-z", "-y")).xyz)

        assertEquals(
                listOf("-x", "-y", "-z"),
                Args(parserOf("-xyz")).xyz)
    }

    @Test
    fun testShortOptionsWithArgs() {
        class Args(parser: ArgParser) {
            val a by parser.flagging("-a")
            val b by parser.flagging("-b")
            val c by parser.flagging("-c")
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z",
                    valueName = "VALUE_NAME",
                    usageArgument = "ARG_NAME") {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName:${next()}")
                }
            }
        }

        // Test with value as separate arg
        assertEquals(
                listOf("-x:0", "-y:1", "-z:2", "-z:3", "-y:4"),
                Args(parserOf("-x", "0", "-y", "1", "-z", "2", "-z", "3", "-y", "4")).xyz)

        // Test with value concatenated
        assertEquals(
                listOf("-x:0", "-y:1", "-z:2", "-z:3", "-y:4"),
                Args(parserOf("-x0", "-y1", "-z2", "-z3", "-y4")).xyz)

        // Test with = between option and value. Note that the "=" is treated as part of the option value for short options.
        assertEquals(
                listOf("-x:=0", "-y:=1", "-z:=2", "-z:=3", "-y:=4"),
                Args(parserOf("-x=0", "-y=1", "-z=2", "-z=3", "-y=4")).xyz)

        // Test chained options. Note that an option with arguments must be last in the chain
        val chain1 = Args(parserOf("-abxc"))
        assertTrue(chain1.a)
        assertTrue(chain1.b)
        assertFalse(chain1.c)
        assertEquals(listOf("-x:c"), chain1.xyz)

        val chain2 = Args(parserOf("-axbc"))
        assertTrue(chain2.a)
        assertFalse(chain2.b)
        assertFalse(chain2.c)
        assertEquals(listOf("-x:bc"), chain2.xyz)
    }

    @Test
    fun testMixedShortOptions() {
        class Args(parser: ArgParser) {
            val def by parser.option<MutableList<String>>("-d", "-e", "-f",
                    valueName = "VALUE_NAME",
                    usageArgument = "ARG_NAME") {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
            val abc by parser.option<MutableList<String>>("-a", "-b", "-c",
                    valueName = "VALUE_NAME",
                    usageArgument = "ARG_NAME") {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
        }

        Args(parserOf("-adbefccbafed")).run {
            assertEquals(
                    listOf("-d", "-e", "-f", "-f", "-e", "-d"),
                    def)
            assertEquals(
                    listOf("-a", "-b", "-c", "-c", "-b", "-a"),
                    abc)
        }
    }

    @Test
    fun testMixedShortOptionsWithArgs() {
        class Args(parser: ArgParser) {
            val def by parser.option<MutableList<String>>("-d", "-e", "-f",
                    valueName = "VALUE_NAME",
                    usageArgument = "ARG_NAME") {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
            val abc by parser.option<MutableList<String>>("-a", "-b", "-c",
                    valueName = "VALUE_NAME",
                    usageArgument = "ARG_NAME") {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z",
                    valueName = "VALUE_NAME",
                    usageArgument = "ARG_NAME") {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName:${next()}")
                }
            }
        }

        Args(parserOf("-adecfy5", "-x0", "-bzxy")).run {
            assertEquals(
                    listOf("-a", "-c", "-b"),
                    abc)
            assertEquals(
                    listOf("-d", "-e", "-f"),
                    def)
            assertEquals(
                    listOf("-y:5", "-x:0", "-z:xy"),
                    xyz)
        }
    }

    @Test
    fun testArglessLongOptions() {
        class Args(parser: ArgParser) {
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zebra",
                    valueName = "ARG_NAME",
                    usageArgument = null) {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
        }

        assertEquals(
                listOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow"),
                Args(parserOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow")).xyz)

        assertEquals(
                listOf("--xray", "--yellow", "--zebra"),
                Args(parserOf("--xray", "--yellow", "--zebra")).xyz)
    }

    @Test
    fun testLongOptionsWithArgs() {
        class Args(parser: ArgParser) {
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zaphod",
                    valueName = "ARG_NAME", usageArgument = "ARG_NAME") {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName:${next()}")
                }
            }
        }

        // Test with value as separate arg
        assertEquals(
                listOf("--xray:0", "--yellow:1", "--zaphod:2", "--zaphod:3", "--yellow:4"),
                Args(parserOf("--xray", "0", "--yellow", "1", "--zaphod", "2", "--zaphod", "3", "--yellow", "4")).xyz)


        // Test with = between option and value
        assertEquals(
                listOf("--xray:0", "--yellow:1", "--zaphod:2", "--zaphod:3", "--yellow:4"),
                Args(parserOf("--xray=0", "--yellow=1", "--zaphod=2", "--zaphod=3", "--yellow=4")).xyz)

        shouldThrow<UnrecognizedOptionException> {
            Args(parserOf("--xray0", "--yellow1", "--zaphod2", "--zaphod3", "--yellow4")).xyz
        }.run {
            assertEquals("unrecognized option '--xray0'", message)
        }
    }

    @Test
    fun testDefault() {
        class Args(parser: ArgParser) {
            val x by parser.storing("-x") { toInt() }.default(5)
        }

        // Test with no value
        assertEquals(
                5,
                Args(parserOf()).x)

        // Test with value
        assertEquals(
                6,
                Args(parserOf("-x6")).x)

        // Test with value as separate arg
        assertEquals(
                7,
                Args(parserOf("-x", "7")).x)

        // Test with multiple values
        assertEquals(
                8,
                Args(parserOf("-x9", "-x8")).x)
    }

    @Test
    fun testFlag() {
        class Args(parser: ArgParser) {
            val x by parser.flagging("-x", "--ecks")
            val y by parser.flagging("-y")
            val z by parser.flagging("--zed")
        }

        Args(parserOf("-x", "-y", "--zed", "--zed", "-y")).run {
            assertTrue(x)
            assertTrue(y)
            assertTrue(z)
        }

        Args(parserOf()).run {
            assertFalse(x)
            assertFalse(y)
            assertFalse(z)
        }

        Args(parserOf("-y", "--ecks")).run {
            assertTrue(x)
            assertTrue(y)
        }

        Args(parserOf("--zed")).run {
            assertFalse(x)
            assertFalse(y)
            assertTrue(z)
        }
    }

    @Test
    fun testArgument_noParser() {
        class Args(parser: ArgParser) {
            val x by parser.storing("--ecks", "-x")
        }

        assertEquals("foo",
                Args(parserOf("-x", "foo")).x)

        assertEquals("baz",
                Args(parserOf("-x", "bar", "-x", "baz")).x)

        assertEquals("short",
                Args(parserOf("--ecks", "long", "-x", "short")).x)

        assertEquals("long",
                Args(parserOf("-x", "short", "--ecks", "long")).x)

        val args = Args(parserOf())
        shouldThrow<MissingValueException> {
            args.x
        }.run {
            assertEquals("missing ECKS", message)
        }
    }

    @Test
    fun testArgument_missing_long() {
        class Args(parser: ArgParser) {
            val x by parser.storing("--ecks")
        }

        val args = Args(parserOf())
        shouldThrow<MissingValueException> {
            args.x
        }.run {
            assertEquals("missing ECKS", message)
        }
    }

    @Test
    fun testArgument_missing_short() {
        class Args(parser: ArgParser) {
            val x by parser.storing("-x")
        }

        val args = Args(parserOf())
        shouldThrow<MissingValueException> {
            args.x
        }.run {
            assertEquals("missing X", message)
        }
    }

    @Test
    fun testArgument_withParser() {
        class Args(parser: ArgParser) {
            val x by parser.storing("-x", "--ecks") { toInt() }
        }

        val opts1 = Args(parserOf("-x", "5"))
        assertEquals(5, opts1.x)

        val opts2 = Args(parserOf("-x", "1", "-x", "2"))
        assertEquals(2, opts2.x)

        val opts3 = Args(parserOf("--ecks", "3", "-x", "4"))
        assertEquals(4, opts3.x)

        val opts4 = Args(parserOf("-x", "5", "--ecks", "6"))
        assertEquals(6, opts4.x)

        val opts6 = Args(parserOf())
        shouldThrow<MissingValueException> {
            opts6.x
        }.run {
            assertEquals("missing ECKS", message)
        }
    }

    @Test
    fun testAccumulator_noParser() {
        class Args(parser: ArgParser) {
            val x by parser.adding("-x", "--ecks")
        }

        assertEquals(
                listOf<String>(),
                Args(parserOf()).x)

        assertEquals(
                listOf("foo"),
                Args(parserOf("-x", "foo")).x)

        assertEquals(
                listOf("bar", "baz"),
                Args(parserOf("-x", "bar", "-x", "baz")).x)

        assertEquals(
                listOf("long", "short"),
                Args(parserOf("--ecks", "long", "-x", "short")).x)

        assertEquals(
                listOf("short", "long"),
                Args(parserOf("-x", "short", "--ecks", "long")).x)
    }

    @Test
    fun testAccumulator_withParser() {
        class Args(parser: ArgParser) {
            val x by parser.adding("-x", "--ecks") { toInt() }
        }

        assertEquals(listOf<Int>(), Args(parserOf()).x)
        assertEquals(listOf(5), Args(parserOf("-x", "5")).x)
        assertEquals(listOf(1, 2), Args(parserOf("-x", "1", "-x", "2")).x)
        assertEquals(listOf(3, 4), Args(parserOf("--ecks", "3", "-x", "4")).x)
        assertEquals(listOf(5, 6), Args(parserOf("-x", "5", "--ecks", "6")).x)
    }

    enum class Color { RED, GREEN, BLUE }

    class ColorArgs(parser: ArgParser) {
        val color by parser.mapping(
                "--red" to Color.RED,
                "--green" to Color.GREEN,
                "--blue" to Color.BLUE)
    }

    @Test
    fun testMapping() {
        assertEquals(Color.RED, ColorArgs(parserOf("--red")).color)
        assertEquals(Color.GREEN, ColorArgs(parserOf("--green")).color)
        assertEquals(Color.BLUE, ColorArgs(parserOf("--blue")).color)

        // Last one takes precedence
        assertEquals(Color.RED, ColorArgs(parserOf("--blue", "--red")).color)
        assertEquals(Color.GREEN, ColorArgs(parserOf("--blue", "--green")).color)
        assertEquals(Color.BLUE, ColorArgs(parserOf("--red", "--blue")).color)

        val args = ColorArgs(parserOf())
        shouldThrow<MissingValueException> {
            args.color
        }.run {
            assertEquals("missing --red|--green|--blue", message)
        }
    }

    class OptionalColorArgs(parser: ArgParser) {
        val color by parser.mapping(
                "--red" to Color.RED,
                "--green" to Color.GREEN,
                "--blue" to Color.BLUE)
                .default(Color.GREEN)
    }

    @Test
    fun testMapping_withDefault() {
        assertEquals(Color.RED, OptionalColorArgs(parserOf("--red")).color)
        assertEquals(Color.GREEN, OptionalColorArgs(parserOf("--green")).color)
        assertEquals(Color.BLUE, OptionalColorArgs(parserOf("--blue")).color)
        assertEquals(Color.GREEN, OptionalColorArgs(parserOf()).color)
    }

    @Test
    fun testUnrecognizedShortOpt() {
        shouldThrow<UnrecognizedOptionException> {
            OptionalColorArgs(parserOf("-x")).color
        }.run {
            assertEquals("unrecognized option '-x'", message)
        }
    }

    @Test
    fun testUnrecognizedLongOpt() {
        shouldThrow<UnrecognizedOptionException> {
            OptionalColorArgs(parserOf("--ecks")).color
        }.run {
            assertEquals("unrecognized option '--ecks'", message)
        }
    }

    @Test
    fun testStoringNoArg() {
        class Args(parser: ArgParser) {
            val x by parser.storing("-x", "--ecks")
        }

        // Note that name actually used for option is used in message
        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("-x")).x
        }.run {
            assertEquals("option '-x' is missing a required argument", message)
        }

        // Note that name actually used for option is used in message
        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("--ecks")).x
        }.run {
            assertEquals("option '--ecks' is missing a required argument", message)
        }
    }

    @Test
    fun testShortStoringNoArgChained() {
        class Args(parser: ArgParser) {
            val y by parser.flagging("-y")
            val x by parser.storing("-x")
        }

        // Note that despite chaining, hyphen appears in message
        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("-yx")).x
        }.run {
            assertEquals("option '-x' is missing a required argument", message)
        }
    }

    @Test
    fun testInitValidation() {
        class Args(parser: ArgParser) {
            val yDelegate = parser.storing("-y") { toInt() }
            val y by yDelegate

            val xDelegate = parser.storing("-x") { toInt() }
            val x by xDelegate

            init {
                if (y >= x)
                    throw InvalidArgumentException("${yDelegate.valueName} must be less than ${xDelegate.valueName}")

                // A better way to accomplish validation that only depends on one Delegate is to use
                // Delegate.addValidator. See testAddValidator for an example of this.
                if (x.mod(2) != 0)
                    throw InvalidArgumentException("${xDelegate.valueName} must be even, $x is odd")
            }
        }

        // This should pass validation
        val opts0 = Args(parserOf("-y1", "-x10"))
        assertEquals(1, opts0.y)
        assertEquals(10, opts0.x)

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("-y20", "-x10")).x
        }.run {
            assertEquals("Y must be less than X", message)
        }

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("-y10", "-x15")).x
        }.run {
            assertEquals("X must be even, 15 is odd", message)
        }

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("-y10", "-x15")).x
        }.run {
            assertEquals("X must be even, 15 is odd", message)
        }
    }

    @Test
    fun testAddValidator() {
        class Args(parser: ArgParser) {
            val yDelegate = parser.storing("-y") { toInt() }
            val y by yDelegate

            val xDelegate = parser.storing("-x") { toInt() }
                    .addValidtator {
                        if (value.mod(2) != 0)
                            throw InvalidArgumentException("$valueName must be even, $value is odd")
                    }
            val x by xDelegate

            init {
                if (y >= x)
                    throw InvalidArgumentException("${yDelegate.valueName} must be less than ${xDelegate.valueName}")

                // A better way to accomplish validation that only depends on one Delegate is to use Delegate.addValidator
            }
        }

        // This should pass validation
        val opts0 = Args(parserOf("-y1", "-x10"))
        assertEquals(1, opts0.y)
        assertEquals(10, opts0.x)

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("-y20", "-x10")).x
        }.run {
            assertEquals("Y must be less than X", message)
        }

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("-y10", "-x15")).x
        }.run {
            assertEquals("X must be even, 15 is odd", message)
        }

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("-y10", "-x15")).x
        }.run {
            assertEquals("X must be even, 15 is odd", message)
        }
    }

    @Test
    fun testUnconsumed() {
        class Args(parser: ArgParser) {
            val y by parser.flagging("-y", "--why")
            val x by parser.flagging("-x", "--ecks")
        }

        // No problem.
        Args(parserOf("-yx")).run {
            assertTrue(x)
            assertTrue(y)
        }

        // Attempting to give -y a parameter, "z", is treated as unrecognized option.
        shouldThrow<UnrecognizedOptionException> {
            Args(parserOf("-yz")).y
        }.run {
            assertEquals("unrecognized option '-z'", message)
        }

        // Unconsumed "z" again, but note that it triggers even if we don't look at y.
        shouldThrow<UnrecognizedOptionException> {
            Args(parserOf("-yz")).x
        }.run {
            assertEquals("unrecognized option '-z'", message)
        }

        // No problem again, this time with long opts.
        Args(parserOf("--why", "--ecks")).run {
            assertTrue(x)
            assertTrue(y)
        }

        // Attempting to give --why a parameter, "z" causes an error.
        shouldThrow<UnexpectedOptionArgumentException> {
            Args(parserOf("--why=z")).y
        }.run {
            assertEquals("option '--why' doesn't allow an argument", message)
        }

        // Unconsumed "z" again, but note that it triggers even if we don't look at y.
        shouldThrow<UnexpectedOptionArgumentException> {
            Args(parserOf("--why=z")).x
        }.run {
            assertEquals("option '--why' doesn't allow an argument", message)
        }
    }

    @Test
    fun testPositional_basic() {
        class Args(parser: ArgParser) {
            val flag by parser.flagging("-f", "--flag")
            val store by parser.storing("-s", "--store").default("DEFAULT")
            val sources by parser.positionalList("SOURCE")
            val destination by parser.positional("DEST")
        }

        Args(parserOf("foo", "bar", "baz", "quux")).run {
            assertFalse(flag)
            assertEquals("DEFAULT", store)
            assertEquals(listOf("foo", "bar", "baz"), sources)
            assertEquals("quux", destination)
        }

        Args(parserOf("-f", "foo", "bar", "baz", "quux")).run {
            assertTrue(flag)
            assertEquals("DEFAULT", store)
            assertEquals(listOf("foo", "bar", "baz"), sources)
            assertEquals("quux", destination)
        }

        Args(parserOf("-s", "foo", "bar", "baz", "quux")).run {
            assertFalse(flag)
            assertEquals("foo", store)
            assertEquals(listOf("bar", "baz"), sources)
            assertEquals("quux", destination)
        }

        Args(parserOf("-s", "foo", "bar", "-f", "baz", "quux")).run {
            assertTrue(flag)
            assertEquals("foo", store)
            assertEquals(listOf("bar", "baz"), sources)
            assertEquals("quux", destination)
        }

        // "--" disables option processing for all further arguments.
        // Note that "-f" is now considered a positional argument.
        Args(parserOf("-s", "foo", "--", "bar", "-f", "baz", "quux")).run {
            assertFalse(flag)
            assertEquals("foo", store)
            assertEquals(listOf("bar", "-f", "baz"), sources)
            assertEquals("quux", destination)
        }

        // "--" disables option processing for all further arguments.
        // Note that the second "--" is also considered a positional argument.
        Args(parserOf("-s", "foo", "--", "bar", "--", "-f", "baz", "quux")).run {
            assertFalse(flag)
            assertEquals("foo", store)
            assertEquals(listOf("bar", "--", "-f", "baz"), sources)
            assertEquals("quux", destination)
        }

        Args(parserOf("-s", "foo", "bar", "-f", "baz", "quux", mode = ArgParser.Mode.POSIX)).run {
            assertFalse(flag)
            assertEquals("foo", store)
            assertEquals(listOf("bar", "-f", "baz"), sources)
            assertEquals("quux", destination)
        }
    }

    @Test
    fun testPositional_withParser() {
        class Args(parser: ArgParser) {
            val flag by parser.flagging("-f", "--flag")
            val store by parser.storing("-s", "--store").default("DEFAULT")
            val start by parser.positionalList("START", 3..4) { toInt() }
            val end by parser.positionalList("END", 3..5) { toInt() }
        }

        shouldThrow<MissingRequiredPositionalArgumentException> {
            Args(parserOf("1", "2")).flag
        }.run {
            assertEquals("missing START operand", message)
        }

        shouldThrow<MissingRequiredPositionalArgumentException> {
            Args(parserOf("1", "2", "3", "4", "5")).flag
        }.run {
            assertEquals("missing END operand", message)
        }

        Args(parserOf("1", "2", "3", "4", "5", "6")).run {
            assertFalse(flag)
            assertEquals("DEFAULT", store)

            // end needs at least 3 args, so start only consumes 3
            assertEquals(listOf(1, 2, 3), start)
            assertEquals(listOf(4, 5, 6), end)
        }

        Args(parserOf("1", "2", "3", "4", "5", "6", "7")).run {
            assertFalse(flag)
            assertEquals("DEFAULT", store)

            // end only needs at 3 args, so start can consume 4
            assertEquals(listOf(1, 2, 3, 4), start)
            assertEquals(listOf(5, 6, 7), end)
        }

        Args(parserOf("1", "2", "3", "4", "5", "6", "7", "8")).run {
            assertFalse(flag)
            assertEquals("DEFAULT", store)

            // start can't consume more than 4, so end gets the rest.
            assertEquals(listOf(1, 2, 3, 4), start)
            assertEquals(listOf(5, 6, 7, 8), end)
        }

        Args(parserOf("1", "2", "3", "4", "5", "6", "7", "8", "9")).run {
            assertFalse(flag)
            assertEquals("DEFAULT", store)

            // once again, start can't consume more than 4, so end gets the rest.
            assertEquals(listOf(1, 2, 3, 4), start)
            assertEquals(listOf(5, 6, 7, 8, 9), end)
        }

        shouldThrow<UnexpectedPositionalArgumentException> {
            Args(parserOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")).flag
        }.run {
            assertEquals("unexpected argument after END", message)
        }
    }

    @Test
    fun testCounting() {
        class Args(parser: ArgParser) {
            val verbosity by parser.counting("-v", "--verbose")
        }

        Args(parserOf()).run {
            assertEquals(0, verbosity)
        }

        Args(parserOf("-v")).run {
            assertEquals(1, verbosity)
        }

        Args(parserOf("-v", "-v")).run {
            assertEquals(2, verbosity)
        }
    }

    @Test
    fun testHelp() {
        class Args(parser: ArgParser) {
            val dryRun by parser.flagging("-n", "--dry-run").help("don't do anything")
            val includes by parser.adding("-I", "--include").help("search in this directory for header files")
            val outDir by parser.storing("-o", "--output").help("directory in which all output should be generated")
            val verbosity by parser.counting("-v", "--verbose").help("increase verbosity")
            val sources by parser.positionalList("SOURCE").help("source file")
            val destination by parser.positional("DEST").help("destination file")
        }

        shouldThrow<ArgParser.ShowHelpException> {
            Args(parserOf("--help")).dryRun
        }.run {
            val writer = StringWriter()
            printUserMessage(writer, "program_name", 60)
            val help = writer.toString()
            assertEquals(
                    """
usage: program_name [-h] [-n] [-I INCLUDE]... -o OUTPUT
                    [-v]... SOURCE... DEST

required arguments:
  -o OUTPUT,        directory in which all output should
  --output OUTPUT   be generated


optional arguments:
  -h, --help        show this help message and exit

  -n, --dry-run     don't do anything

  -I INCLUDE,       search in this directory for header
  --include INCLU   files
  DE

  -v, --verbose     increase verbosity


positional arguments:
  SOURCE            source file

  DEST              destination file

""".trimStart(), help)
        }
    }

    // TODO: test default on argument
    // TODO: test default on argumentList
    // TODO: test addValidator on argument
    // TODO: test addValidator on argumentList
}
