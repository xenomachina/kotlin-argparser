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

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TestName

class OptionParserTest {
    @JvmField @Rule
    val testName: TestName = TestName()

    @JvmField @Rule
    val thrown = ExpectedException.none()

    fun parserOf(vararg args: String) = OptionParser(args)

    @Test
    fun testArglessShortOptions() {
        class Opts(parser: OptionParser) {
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
        }

        Assert.assertEquals(
                listOf("-x", "-y", "-z", "-z", "-y"),
                Opts(parserOf("-x", "-y", "-z", "-z", "-y")).xyz)

        Assert.assertEquals(
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
                    add("$name:${next()}")
                }
            }
        }

        // Test with value as separate arg
        Assert.assertEquals(
                listOf("-x:0", "-y:1", "-z:2", "-z:3", "-y:4"),
                Opts(parserOf("-x", "0", "-y", "1", "-z", "2", "-z", "3", "-y", "4")).xyz)

        // Test with value concatenated
        Assert.assertEquals(
                listOf("-x:0", "-y:1", "-z:2", "-z:3", "-y:4"),
                Opts(parserOf("-x0", "-y1", "-z2", "-z3", "-y4")).xyz)

        // Test with = between option and value. Note that the "=" is treated as part of the option value for short options.
        Assert.assertEquals(
                listOf("-x:=0", "-y:=1", "-z:=2", "-z:=3", "-y:=4"),
                Opts(parserOf("-x=0", "-y=1", "-z=2", "-z=3", "-y=4")).xyz)

        // Test chained options. Note that an option with arguments must be last in the chain
        val chain1 = Opts(parserOf("-abxc"))
        Assert.assertTrue(chain1.a)
        Assert.assertTrue(chain1.b)
        Assert.assertFalse(chain1.c)
        Assert.assertEquals(listOf("-x:c"), chain1.xyz)

        val chain2 = Opts(parserOf("-axbc"))
        Assert.assertTrue(chain2.a)
        Assert.assertFalse(chain2.b)
        Assert.assertFalse(chain2.c)
        Assert.assertEquals(listOf("-x:bc"), chain2.xyz)
    }

    @Test
    fun testMixedShortOptions() {
        class Opts(parser: OptionParser) {
            val def by parser.option<MutableList<String>>("-d", "-e", "-f", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
            val abc by parser.option<MutableList<String>>("-a", "-b", "-c", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
        }

        val myOpts = Opts(parserOf("-adbefccbafed"))

        Assert.assertEquals(
                listOf("-d", "-e", "-f", "-f", "-e", "-d"),
                myOpts.def)
        Assert.assertEquals(
                listOf("-a", "-b", "-c", "-c", "-b", "-a"),
                myOpts.abc)
    }

    @Test
    fun testMixedShortOptionsWithArgs() {
        class Opts(parser: OptionParser) {
            val def by parser.option<MutableList<String>>("-d", "-e", "-f", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
            val abc by parser.option<MutableList<String>>("-a", "-b", "-c", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name:${next()}")
                }
            }
        }

        val myOpts = Opts(parserOf("-adecfy5", "-x0", "-bzxy"))

        Assert.assertEquals(
                listOf("-a", "-c", "-b"),
                myOpts.abc)
        Assert.assertEquals(
                listOf("-d", "-e", "-f"),
                myOpts.def)
        Assert.assertEquals(
                listOf("-y:5", "-x:0", "-z:xy"),
                myOpts.xyz)
    }

    @Test
    fun testArglessLongOptions() {
        class Opts(parser: OptionParser) {
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zebra", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
        }

        Assert.assertEquals(
                listOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow"),
                Opts(parserOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow")).xyz)

        Assert.assertEquals(
                listOf("--xray", "--yellow", "--zebra"),
                Opts(parserOf("--xray", "--yellow", "--zebra")).xyz)
    }

    @Test
    fun testLongOptionsWithArgs() {
        class Opts(parser: OptionParser) {
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zaphod", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name:${next()}")
                }
            }
        }

        // Test with value as separate arg
        Assert.assertEquals(
                listOf("--xray:0", "--yellow:1", "--zaphod:2", "--zaphod:3", "--yellow:4"),
                Opts(parserOf("--xray", "0", "--yellow", "1", "--zaphod", "2", "--zaphod", "3", "--yellow", "4")).xyz)


        // Test with = between option and value
        Assert.assertEquals(
                listOf("--xray:0", "--yellow:1", "--zaphod:2", "--zaphod:3", "--yellow:4"),
                Opts(parserOf("--xray=0", "--yellow=1", "--zaphod=2", "--zaphod=3", "--yellow=4")).xyz)
    }

    @Test
    fun testLongOptionsWithConcatenatedArgs() {
        class Opts(parser: OptionParser) {
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zaphod", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name:${next()}")
                }
            }
        }

        thrown.expect(UnrecognizedOptionException::class.java)
        thrown.expectMessage("unrecognized option '--xray0'")
        Opts(parserOf("--xray0", "--yellow1", "--zaphod2", "--zaphod3", "--yellow4")).xyz
    }

    @Test
    fun testDefault() {
        class Opts(parser: OptionParser) {
            val x by parser.option<Int>("-x", valueName = "ARG_NAME"){
                next().toInt()
            }.default(5)
        }

        // Test with no value
        Assert.assertEquals(
                5,
                Opts(parserOf()).x)

        // Test with value
        Assert.assertEquals(
                6,
                Opts(parserOf("-x6")).x)

        // Test with value as separate arg
        Assert.assertEquals(
                7,
                Opts(parserOf("-x", "7")).x)

        // Test with multiple values
        Assert.assertEquals(
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
        Assert.assertTrue(opts1.x)
        Assert.assertTrue(opts1.y)
        Assert.assertTrue(opts1.z)

        val opts2 = Opts(parserOf())
        Assert.assertFalse(opts2.x)
        Assert.assertFalse(opts2.y)
        Assert.assertFalse(opts2.z)

        val opts3 = Opts(parserOf("-y", "--ecks"))
        Assert.assertTrue(opts3.x)
        Assert.assertTrue(opts3.y)

        val opts4 = Opts(parserOf("--zed"))
        Assert.assertFalse(opts4.x)
        Assert.assertFalse(opts4.y)
        Assert.assertTrue(opts4.z)
    }

    @Test
    fun testArgument_noParser() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("--ecks", "-x")
        }

        Assert.assertEquals("foo",
                Opts(parserOf("-x", "foo")).x)

        Assert.assertEquals("baz",
                Opts(parserOf("-x", "bar", "-x", "baz")).x)

        Assert.assertEquals("short",
                Opts(parserOf("--ecks", "long", "-x", "short")).x)

        Assert.assertEquals("long",
                Opts(parserOf("-x", "short", "--ecks", "long")).x)
    }

    @Test
    fun testArgument_missing_long() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("--ecks")
        }

        val opts = Opts(parserOf())
        thrown.expect(MissingValueException::class.java)
        thrown.expectMessage("missing ECKS")
        opts.x
    }

    @Test
    fun testArgument_missing_short() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("-x")
        }

        val opts = Opts(parserOf())
        thrown.expect(MissingValueException::class.java)
        thrown.expectMessage("missing X")
        opts.x
    }

    @Test
    fun testArgument_missingShortAndLong() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("--ecks", "-x")
        }

        val opts = Opts(parserOf())
        thrown.expect(MissingValueException::class.java)
        thrown.expectMessage("missing ECKS")
        opts.x
    }

    @Test
    fun testArgument_withParser() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("-x", "--ecks"){toInt()}
        }

        val opts1 = Opts(parserOf("-x", "5"))
        Assert.assertEquals(5, opts1.x)

        val opts2 = Opts(parserOf("-x", "1", "-x", "2"))
        Assert.assertEquals(2, opts2.x)

        val opts3 = Opts(parserOf("--ecks", "3", "-x", "4"))
        Assert.assertEquals(4, opts3.x)

        val opts4 = Opts(parserOf("-x", "5", "--ecks", "6"))
        Assert.assertEquals(6, opts4.x)
    }

    @Test
    fun testArgument_missing_withParser() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("-x", "--ecks"){toInt()}
        }

        val opts = Opts(parserOf())
        thrown.expect(MissingValueException::class.java)
        thrown.expectMessage("missing ECKS")
        opts.x
    }

    @Test
    fun testAccumulator_noParser() {
        class Opts(parser: OptionParser) {
            val x by parser.adding("-x", "--ecks")
        }

        Assert.assertEquals(
                listOf<String>(),
                Opts(parserOf()).x)

        Assert.assertEquals(
                listOf("foo"),
                Opts(parserOf("-x", "foo")).x)

        Assert.assertEquals(
                listOf("bar", "baz"),
                Opts(parserOf("-x", "bar", "-x", "baz")).x)

        Assert.assertEquals(
                listOf("long", "short"),
                Opts(parserOf("--ecks", "long", "-x", "short")).x)

        Assert.assertEquals(
                listOf("short", "long"),
                Opts(parserOf("-x", "short", "--ecks", "long")).x)
    }

    @Test
    fun testAccumulator_withParser() {
        class Opts(parser: OptionParser) {
            val x by parser.adding("-x", "--ecks"){toInt()}
        }

        Assert.assertEquals(listOf<Int>(), Opts(parserOf()).x)
        Assert.assertEquals(listOf(5), Opts(parserOf("-x", "5")).x)
        Assert.assertEquals(listOf(1, 2), Opts(parserOf("-x", "1", "-x", "2")).x)
        Assert.assertEquals(listOf(3, 4), Opts(parserOf("--ecks", "3", "-x", "4")).x)
        Assert.assertEquals(listOf(5, 6), Opts(parserOf("-x", "5", "--ecks", "6")).x)
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
        Assert.assertEquals(Color.RED,   ColorOpts(parserOf("--red")).color)
        Assert.assertEquals(Color.GREEN, ColorOpts(parserOf("--green")).color)
        Assert.assertEquals(Color.BLUE,  ColorOpts(parserOf("--blue")).color)

        // Last one takes precedence
        Assert.assertEquals(Color.RED,   ColorOpts(parserOf("--blue", "--red")).color)
        Assert.assertEquals(Color.GREEN, ColorOpts(parserOf("--blue", "--green")).color)
        Assert.assertEquals(Color.BLUE,  ColorOpts(parserOf("--red", "--blue")).color)
    }

    @Test
    fun testMapping_noArgs() {
        val opts = ColorOpts(parserOf())
        thrown.expect(MissingValueException::class.java)
        thrown.expectMessage("missing --red|--green|--blue")
        opts.color
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
        Assert.assertEquals(Color.RED,   OptionalColorOpts(parserOf("--red")).color)
        Assert.assertEquals(Color.GREEN, OptionalColorOpts(parserOf("--green")).color)
        Assert.assertEquals(Color.BLUE,  OptionalColorOpts(parserOf("--blue")).color)
        Assert.assertEquals(Color.GREEN, OptionalColorOpts(parserOf()).color)
    }

    @Test
    fun testUnrecognizedShortOpt() {
        thrown.expect(UnrecognizedOptionException::class.java)
        thrown.expectMessage("unrecognized option '-x'")
        OptionalColorOpts(parserOf("-x")).color
    }

    @Test
    fun testUnrecognizedLongOpt() {
        thrown.expect(UnrecognizedOptionException::class.java)
        thrown.expectMessage("unrecognized option '--ecks'")
        OptionalColorOpts(parserOf("--ecks")).color
    }

    @Test
    fun testShortStoringNoArg() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("-x", "--ecks")
        }

        // Note that name actually used for option is used in message
        thrown.expect(OptionMissingRequiredArgumentException::class.java)
        thrown.expectMessage("option '-x' is missing a required argument")
        Opts(parserOf("-x")).x
    }

    @Test
    fun testLongStoringNoArg() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("-x", "--ecks")
        }

        // Note that name actually used for option is used in message
        thrown.expect(OptionMissingRequiredArgumentException::class.java)
        thrown.expectMessage("option '--ecks' is missing a required argument")
        Opts(parserOf("--ecks")).x
    }

    @Test
    fun testShortStoringNoArgChained() {
        class Opts(parser: OptionParser) {
            val y by parser.flagging("-y")
            val x by parser.storing("-x")
        }

        // Note that despite chaining, hyphen appears in message
        thrown.expect(OptionMissingRequiredArgumentException::class.java)
        thrown.expectMessage("option '-x' is missing a required argument")
        Opts(parserOf("-yx")).x
    }
}
