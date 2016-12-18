// Copyright © 2016 Laurence Gonsalves
//
// This file is part of kotlin-optionparser, a library which can be found at
// http://github.com/xenomachina/kotlin-optionparser
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

package com.xenomachina.optionparser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TestName

class OptionParserTest {
    @JvmField @Rule
    val testName: TestName = TestName()

    fun <X> shouldThrow(exceptionClass: Class<X>, f: () -> Unit): X {
        try {
            f()
        } catch (exception: Exception) {
            if (exceptionClass.isInstance(exception)) return exceptionClass.cast(exception)
            throw exception
        }
        throw AssertionError("Expected ${exceptionClass.canonicalName} to be thrown")
    }

    fun parserOf(vararg args: String) = OptionParser(args)

    @Test
    fun testArglessShortOptions() {
        class Opts(parser: OptionParser) {
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$optionName")
                }
            }
        }

        assertEquals(
                listOf("-x", "-y", "-z", "-z", "-y"),
                Opts(parserOf("-x", "-y", "-z", "-z", "-y")).xyz)

        assertEquals(
                listOf("-x", "-y", "-z"),
                Opts(parserOf("-xyz")).xyz)
    }

    @Test
    fun testShortOptionsWithArgs() {
        class Opts(parser: OptionParser) {
            val a by parser.flagging("-a")
            val b by parser.flagging("-b")
            val c by parser.flagging("-c")
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$optionName:${next()}")
                }
            }
        }

        // Test with value as separate arg
        assertEquals(
                listOf("-x:0", "-y:1", "-z:2", "-z:3", "-y:4"),
                Opts(parserOf("-x", "0", "-y", "1", "-z", "2", "-z", "3", "-y", "4")).xyz)

        // Test with value concatenated
        assertEquals(
                listOf("-x:0", "-y:1", "-z:2", "-z:3", "-y:4"),
                Opts(parserOf("-x0", "-y1", "-z2", "-z3", "-y4")).xyz)

        // Test with = between option and value. Note that the "=" is treated as part of the option value for short options.
        assertEquals(
                listOf("-x:=0", "-y:=1", "-z:=2", "-z:=3", "-y:=4"),
                Opts(parserOf("-x=0", "-y=1", "-z=2", "-z=3", "-y=4")).xyz)

        // Test chained options. Note that an option with arguments must be last in the chain
        val chain1 = Opts(parserOf("-abxc"))
        assertTrue(chain1.a)
        assertTrue(chain1.b)
        assertFalse(chain1.c)
        assertEquals(listOf("-x:c"), chain1.xyz)

        val chain2 = Opts(parserOf("-axbc"))
        assertTrue(chain2.a)
        assertFalse(chain2.b)
        assertFalse(chain2.c)
        assertEquals(listOf("-x:bc"), chain2.xyz)
    }

    @Test
    fun testMixedShortOptions() {
        class Opts(parser: OptionParser) {
            val def by parser.option<MutableList<String>>("-d", "-e", "-f", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$optionName")
                }
            }
            val abc by parser.option<MutableList<String>>("-a", "-b", "-c", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$optionName")
                }
            }
        }

        val myOpts = Opts(parserOf("-adbefccbafed"))

        assertEquals(
                listOf("-d", "-e", "-f", "-f", "-e", "-d"),
                myOpts.def)
        assertEquals(
                listOf("-a", "-b", "-c", "-c", "-b", "-a"),
                myOpts.abc)
    }

    @Test
    fun testMixedShortOptionsWithArgs() {
        class Opts(parser: OptionParser) {
            val def by parser.option<MutableList<String>>("-d", "-e", "-f", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$optionName")
                }
            }
            val abc by parser.option<MutableList<String>>("-a", "-b", "-c", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$optionName")
                }
            }
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$optionName:${next()}")
                }
            }
        }

        val myOpts = Opts(parserOf("-adecfy5", "-x0", "-bzxy"))

        assertEquals(
                listOf("-a", "-c", "-b"),
                myOpts.abc)
        assertEquals(
                listOf("-d", "-e", "-f"),
                myOpts.def)
        assertEquals(
                listOf("-y:5", "-x:0", "-z:xy"),
                myOpts.xyz)
    }

    @Test
    fun testArglessLongOptions() {
        class Opts(parser: OptionParser) {
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zebra", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$optionName")
                }
            }
        }

        assertEquals(
                listOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow"),
                Opts(parserOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow")).xyz)

        assertEquals(
                listOf("--xray", "--yellow", "--zebra"),
                Opts(parserOf("--xray", "--yellow", "--zebra")).xyz)
    }

    @Test
    fun testLongOptionsWithArgs() {
        class Opts(parser: OptionParser) {
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zaphod", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$optionName:${next()}")
                }
            }
        }

        // Test with value as separate arg
        assertEquals(
                listOf("--xray:0", "--yellow:1", "--zaphod:2", "--zaphod:3", "--yellow:4"),
                Opts(parserOf("--xray", "0", "--yellow", "1", "--zaphod", "2", "--zaphod", "3", "--yellow", "4")).xyz)


        // Test with = between option and value
        assertEquals(
                listOf("--xray:0", "--yellow:1", "--zaphod:2", "--zaphod:3", "--yellow:4"),
                Opts(parserOf("--xray=0", "--yellow=1", "--zaphod=2", "--zaphod=3", "--yellow=4")).xyz)

        shouldThrow(UnrecognizedOptionException::class.java) {
            Opts(parserOf("--xray0", "--yellow1", "--zaphod2", "--zaphod3", "--yellow4")).xyz
        }.run {
            assertEquals("unrecognized option '--xray0'", message)
        }
    }

    @Test
    fun testDefault() {
        class Opts(parser: OptionParser) {
            val x by parser.option<Int>("-x", valueName = "ARG_NAME"){
                next().toInt()
            }.default(5)
        }

        // Test with no value
        assertEquals(
                5,
                Opts(parserOf()).x)

        // Test with value
        assertEquals(
                6,
                Opts(parserOf("-x6")).x)

        // Test with value as separate arg
        assertEquals(
                7,
                Opts(parserOf("-x", "7")).x)

        // Test with multiple values
        assertEquals(
                8,
                Opts(parserOf("-x9", "-x8")).x)
    }

    @Test
    fun testFlag() {
        class Opts(parser: OptionParser) {
            val x by parser.flagging("-x", "--ecks")
            val y by parser.flagging("-y")
            val z by parser.flagging("--zed")
        }

        val opts1 = Opts(parserOf("-x", "-y", "--zed", "--zed", "-y"))
        assertTrue(opts1.x)
        assertTrue(opts1.y)
        assertTrue(opts1.z)

        val opts2 = Opts(parserOf())
        assertFalse(opts2.x)
        assertFalse(opts2.y)
        assertFalse(opts2.z)

        val opts3 = Opts(parserOf("-y", "--ecks"))
        assertTrue(opts3.x)
        assertTrue(opts3.y)

        val opts4 = Opts(parserOf("--zed"))
        assertFalse(opts4.x)
        assertFalse(opts4.y)
        assertTrue(opts4.z)
    }

    @Test
    fun testArgument_noParser() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("--ecks", "-x")
        }

        assertEquals("foo",
                Opts(parserOf("-x", "foo")).x)

        assertEquals("baz",
                Opts(parserOf("-x", "bar", "-x", "baz")).x)

        assertEquals("short",
                Opts(parserOf("--ecks", "long", "-x", "short")).x)

        assertEquals("long",
                Opts(parserOf("-x", "short", "--ecks", "long")).x)

        val opts = Opts(parserOf())
        shouldThrow(MissingValueException::class.java) {
            opts.x
        }.run {
            assertEquals("missing ECKS", message)
        }
    }

    @Test
    fun testArgument_missing_long() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("--ecks")
        }

        val opts = Opts(parserOf())
        shouldThrow(MissingValueException::class.java) {
            opts.x
        }.run {
            assertEquals("missing ECKS", message)
        }
    }

    @Test
    fun testArgument_missing_short() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("-x")
        }

        val opts = Opts(parserOf())
        shouldThrow(MissingValueException::class.java) {
            opts.x
        }.run {
            assertEquals("missing X", message)
        }
    }

    @Test
    fun testArgument_withParser() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("-x", "--ecks"){toInt()}
        }

        val opts1 = Opts(parserOf("-x", "5"))
        assertEquals(5, opts1.x)

        val opts2 = Opts(parserOf("-x", "1", "-x", "2"))
        assertEquals(2, opts2.x)

        val opts3 = Opts(parserOf("--ecks", "3", "-x", "4"))
        assertEquals(4, opts3.x)

        val opts4 = Opts(parserOf("-x", "5", "--ecks", "6"))
        assertEquals(6, opts4.x)

        val opts6 = Opts(parserOf())
        shouldThrow(MissingValueException::class.java) {
            opts6.x
        }.run {
            assertEquals("missing ECKS", message)
        }
    }

    @Test
    fun testAccumulator_noParser() {
        class Opts(parser: OptionParser) {
            val x by parser.adding("-x", "--ecks")
        }

        assertEquals(
                listOf<String>(),
                Opts(parserOf()).x)

        assertEquals(
                listOf("foo"),
                Opts(parserOf("-x", "foo")).x)

        assertEquals(
                listOf("bar", "baz"),
                Opts(parserOf("-x", "bar", "-x", "baz")).x)

        assertEquals(
                listOf("long", "short"),
                Opts(parserOf("--ecks", "long", "-x", "short")).x)

        assertEquals(
                listOf("short", "long"),
                Opts(parserOf("-x", "short", "--ecks", "long")).x)
    }

    @Test
    fun testAccumulator_withParser() {
        class Opts(parser: OptionParser) {
            val x by parser.adding("-x", "--ecks"){toInt()}
        }

        assertEquals(listOf<Int>(), Opts(parserOf()).x)
        assertEquals(listOf(5), Opts(parserOf("-x", "5")).x)
        assertEquals(listOf(1, 2), Opts(parserOf("-x", "1", "-x", "2")).x)
        assertEquals(listOf(3, 4), Opts(parserOf("--ecks", "3", "-x", "4")).x)
        assertEquals(listOf(5, 6), Opts(parserOf("-x", "5", "--ecks", "6")).x)
    }

    enum class Color { RED, GREEN, BLUE }

    class ColorOpts(parser: OptionParser) {
        val color by parser.mapping(
                "--red" to Color.RED,
                "--green" to Color.GREEN,
                "--blue" to Color.BLUE)
    }

    @Test
    fun testMapping() {
        assertEquals(Color.RED,   ColorOpts(parserOf("--red")).color)
        assertEquals(Color.GREEN, ColorOpts(parserOf("--green")).color)
        assertEquals(Color.BLUE,  ColorOpts(parserOf("--blue")).color)

        // Last one takes precedence
        assertEquals(Color.RED,   ColorOpts(parserOf("--blue", "--red")).color)
        assertEquals(Color.GREEN, ColorOpts(parserOf("--blue", "--green")).color)
        assertEquals(Color.BLUE,  ColorOpts(parserOf("--red", "--blue")).color)

        val opts = ColorOpts(parserOf())
        shouldThrow(MissingValueException::class.java) {
            opts.color
        }.run {
            assertEquals("missing --red|--green|--blue", message)
        }
    }

    class OptionalColorOpts(parser: OptionParser) {
        val color by parser.mapping(
                "--red" to Color.RED,
                "--green" to Color.GREEN,
                "--blue" to Color.BLUE)
                .default(Color.GREEN)
    }

    @Test
    fun testMapping_withDefault() {
        assertEquals(Color.RED,   OptionalColorOpts(parserOf("--red")).color)
        assertEquals(Color.GREEN, OptionalColorOpts(parserOf("--green")).color)
        assertEquals(Color.BLUE,  OptionalColorOpts(parserOf("--blue")).color)
        assertEquals(Color.GREEN, OptionalColorOpts(parserOf()).color)
    }

    @Test
    fun testUnrecognizedShortOpt() {
        shouldThrow(UnrecognizedOptionException::class.java) {
            OptionalColorOpts(parserOf("-x")).color
        }.run {
            assertEquals("unrecognized option '-x'", message)
        }
    }

    @Test
    fun testUnrecognizedLongOpt() {
        shouldThrow(UnrecognizedOptionException::class.java) {
            OptionalColorOpts(parserOf("--ecks")).color
        }.run {
            assertEquals("unrecognized option '--ecks'", message)
        }
    }

    @Test
    fun testStoringNoArg() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("-x", "--ecks")
        }

        // Note that name actually used for option is used in message
        shouldThrow(OptionMissingRequiredArgumentException::class.java) {
            Opts(parserOf("-x")).x
        }.run {
            assertEquals("option '-x' is missing a required argument", message)
        }

        // Note that name actually used for option is used in message
        shouldThrow(OptionMissingRequiredArgumentException::class.java) {
            Opts(parserOf("--ecks")).x
        }.run {
            assertEquals("option '--ecks' is missing a required argument", message)
        }
    }

    @Test
    fun testShortStoringNoArgChained() {
        class Opts(parser: OptionParser) {
            val y by parser.flagging("-y")
            val x by parser.storing("-x")
        }

        // Note that despite chaining, hyphen appears in message
        shouldThrow(OptionMissingRequiredArgumentException::class.java) {
            Opts(parserOf("-yx")).x
        }.run {
            assertEquals("option '-x' is missing a required argument", message)
        }
    }

    @Test
    fun testInitValidation() {
        class Opts(parser: OptionParser) {
            val yDelegate = parser.storing("-y"){toInt()}
            val y by yDelegate

            val xDelegate = parser.storing("-x"){toInt()}
            val x by xDelegate

            init {
                if (y >= x)
                    throw InvalidArgumentException("${yDelegate.valueName} must be less than ${xDelegate.valueName}")

                // A better way to accomplish validation that only depends on one Delegate is to use
                // Delegate.addValidator. See testAddValidator for an example of this.
                if (x % 2 != 0)
                    throw InvalidArgumentException("${xDelegate.valueName} must be even, $x is odd")
            }
        }

        // This should pass validation
        val opts0 = Opts(parserOf("-y1", "-x10"))
        assertEquals(1, opts0.y)
        assertEquals(10, opts0.x)

        shouldThrow(InvalidArgumentException::class.java) {
            Opts(parserOf("-y20", "-x10")).x
        }.run {
            assertEquals("Y must be less than X", message)
        }

        shouldThrow(InvalidArgumentException::class.java) {
            Opts(parserOf("-y10", "-x15")).x
        }.run {
            assertEquals("X must be even, 15 is odd", message)
        }

        shouldThrow(InvalidArgumentException::class.java) {
            Opts(parserOf("-y10", "-x15")).x
        }.run {
            assertEquals("X must be even, 15 is odd", message)
        }
    }

    @Test
    fun testAddValidator() {
        class Opts(parser: OptionParser) {
            val yDelegate = parser.storing("-y"){toInt()}
            val y by yDelegate

            val xDelegate = parser.storing("-x"){toInt()}
                    .addValidtator {
                        if (value % 2 != 0)
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
        val opts0 = Opts(parserOf("-y1", "-x10"))
        assertEquals(1, opts0.y)
        assertEquals(10, opts0.x)

        shouldThrow(InvalidArgumentException::class.java) {
            Opts(parserOf("-y20", "-x10")).x
        }.run {
            assertEquals("Y must be less than X", message)
        }

        shouldThrow(InvalidArgumentException::class.java) {
            Opts(parserOf("-y10", "-x15")).x
        }.run {
            assertEquals("X must be even, 15 is odd", message)
        }

        shouldThrow(InvalidArgumentException::class.java) {
            Opts(parserOf("-y10", "-x15")).x
        }.run {
            assertEquals("X must be even, 15 is odd", message)
        }
    }

    @Test
    fun testUnconsumed() {
        class Opts(parser: OptionParser) {
            val y by parser.flagging("-y", "--why")
            val x by parser.flagging("-x", "--ecks")
        }

        // No problem.
        Opts(parserOf("-yx")).run {
            assertTrue(x)
            assertTrue(y)
        }

        // Attempting to give -y a parameter, "z", is treated as unrecognized option.
        shouldThrow(UnrecognizedOptionException::class.java) {
            Opts(parserOf("-yz")).y
        }.run {
            assertEquals("unrecognized option '-z'", message)
        }

        // Unconsumed "z" again, but note that it triggers even if we don't look at y.
        shouldThrow(UnrecognizedOptionException::class.java) {
            Opts(parserOf("-yz")).x
        }.run {
            assertEquals("unrecognized option '-z'", message)
        }

        // No problem again, this time with long opts.
        Opts(parserOf("--why", "--ecks")).run {
            assertTrue(x)
            assertTrue(y)
        }

        // Attempting to give --why a parameter, "z" causes an error.
        shouldThrow(UnexpectedOptionArgumentException::class.java) {
            Opts(parserOf("--why=z")).y
        }.run {
            assertEquals("option '--why' doesn't allow an argument", message)
        }

        // Unconsumed "z" again, but note that it triggers even if we don't look at y.
        shouldThrow(UnexpectedOptionArgumentException::class.java) {
            Opts(parserOf("--why=z")).x
        }.run {
            assertEquals("option '--why' doesn't allow an argument", message)
        }
    }

    @Test
    fun testPositional_basic() {
        class Opts(parser: OptionParser) {
            val flag by parser.flagging("-f", "--flag")
            val store by parser.storing("-s", "--store").default("DEFAULT")
            val sources by parser.argumentList("SOURCE...")
            val destination by parser.argument("DEST")
        }

        Opts(parserOf("foo", "bar", "baz", "quux")).run {
            assertFalse(flag)
            assertEquals("DEFAULT", store)
            assertEquals(listOf("foo", "bar", "baz"), sources)
            assertEquals("quux", destination)
        }

        Opts(parserOf("-f", "foo", "bar", "baz", "quux")).run {
            assertTrue(flag)
            assertEquals("DEFAULT", store)
            assertEquals(listOf("foo", "bar", "baz"), sources)
            assertEquals("quux", destination)
        }

        Opts(parserOf("-s", "foo", "bar", "baz", "quux")).run {
            assertFalse(flag)
            assertEquals("foo", store)
            assertEquals(listOf("bar", "baz"), sources)
            assertEquals("quux", destination)
        }

        Opts(parserOf("-s", "foo", "bar", "-f", "baz", "quux")).run {
            assertTrue(flag)
            assertEquals("foo", store)
            assertEquals(listOf("bar", "baz"), sources)
            assertEquals("quux", destination)
        }

        // "--" disables option processing for all further arguments.
        // Note that "-f" is now considered a positional argument.
        Opts(parserOf("-s", "foo", "--", "bar", "-f", "baz", "quux")).run {
            assertFalse(flag)
            assertEquals("foo", store)
            assertEquals(listOf("bar", "-f", "baz"), sources)
            assertEquals("quux", destination)
        }

        // "--" disables option processing for all further arguments.
        // Note that the second "--" is also considered a positional argument.
        Opts(parserOf("-s", "foo", "--", "bar", "--", "-f", "baz", "quux")).run {
            assertFalse(flag)
            assertEquals("foo", store)
            assertEquals(listOf("bar", "--", "-f", "baz"), sources)
            assertEquals("quux", destination)
        }
    }

    @Test
    fun testPositional_withParser() {
        class Opts(parser: OptionParser) {
            val flag by parser.flagging("-f", "--flag")
            val store by parser.storing("-s", "--store").default("DEFAULT")
            val start by parser.argumentList("START...", 3..4){toInt()}
            val end by parser.argumentList("END...", 3..5){toInt()}
        }

        shouldThrow(MissingRequiredPositionalArgumentException::class.java) {
            Opts(parserOf("1", "2")).flag
        }.run {
            assertEquals("missing START operand", message)
        }

        shouldThrow(MissingRequiredPositionalArgumentException::class.java) {
            Opts(parserOf("1", "2", "3", "4", "5")).flag
        }.run {
            assertEquals("missing END operand", message)
        }

        Opts(parserOf("1", "2", "3", "4", "5", "6")).run {
            assertFalse(flag)
            assertEquals("DEFAULT", store)

            // end needs at least 3 args, so start only consumes 3
            assertEquals(listOf(1, 2, 3), start)
            assertEquals(listOf(4, 5, 6), end)
        }

        Opts(parserOf("1", "2", "3", "4", "5", "6", "7")).run {
            assertFalse(flag)
            assertEquals("DEFAULT", store)

            // end only needs at 3 args, so start can consume 4
            assertEquals(listOf(1, 2, 3, 4), start)
            assertEquals(listOf(5, 6, 7), end)
        }

        Opts(parserOf("1", "2", "3", "4", "5", "6", "7", "8")).run {
            assertFalse(flag)
            assertEquals("DEFAULT", store)

            // start can't consume more than 4, so end gets the rest.
            assertEquals(listOf(1, 2, 3, 4), start)
            assertEquals(listOf(5, 6, 7, 8), end)
        }

        Opts(parserOf("1", "2", "3", "4", "5", "6", "7", "8", "9")).run {
            assertFalse(flag)
            assertEquals("DEFAULT", store)

            // once again, start can't consume more than 4, so end gets the rest.
            assertEquals(listOf(1, 2, 3, 4), start)
            assertEquals(listOf(5, 6, 7, 8, 9), end)
        }

        shouldThrow(UnexpectedPositionalArgumentException::class.java) {
            Opts(parserOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")).flag
        }.run {
            assertEquals("unexpected argument after END...", message)
        }
    }
}
