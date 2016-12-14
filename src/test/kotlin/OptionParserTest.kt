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

    @Test
    fun testArglessShortOptions() {
        class Opts(args: Array<String>) {
            private val parser = OptionParser(args)
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
        }

        Assert.assertEquals(
                listOf("-x", "-y", "-z", "-z", "-y"),
                Opts(arrayOf("-x", "-y", "-z", "-z", "-y")).xyz)

        Assert.assertEquals(
                listOf("-x", "-y", "-z"),
                Opts(arrayOf("-xyz")).xyz)
    }

    @Test
    fun testShortOptionsWithArgs() {
        class Opts(args: Array<String>) {
            private val parser = OptionParser(args)
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
                Opts(arrayOf("-x", "0", "-y", "1", "-z", "2", "-z", "3", "-y", "4")).xyz)

        // Test with value concatenated
        Assert.assertEquals(
                listOf("-x:0", "-y:1", "-z:2", "-z:3", "-y:4"),
                Opts(arrayOf("-x0", "-y1", "-z2", "-z3", "-y4")).xyz)

        // Test with = between option and value. Note that the "=" is treated as part of the option value for short options.
        Assert.assertEquals(
                listOf("-x:=0", "-y:=1", "-z:=2", "-z:=3", "-y:=4"),
                Opts(arrayOf("-x=0", "-y=1", "-z=2", "-z=3", "-y=4")).xyz)

        // Test chained options. Note that an option with arguments must be last in the chain
        val chain1 = Opts(arrayOf("-abxc"))
        Assert.assertTrue(chain1.a)
        Assert.assertTrue(chain1.b)
        Assert.assertFalse(chain1.c)
        Assert.assertEquals(listOf("-x:c"), chain1.xyz)

        val chain2 = Opts(arrayOf("-axbc"))
        Assert.assertTrue(chain2.a)
        Assert.assertFalse(chain2.b)
        Assert.assertFalse(chain2.c)
        Assert.assertEquals(listOf("-x:bc"), chain2.xyz)
    }

    @Test
    fun testMixedShortOptions() {
        class Opts(args: Array<String>) {
            private val parser = OptionParser(args)
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

        val myOpts = Opts(arrayOf("-adbefccbafed"))

        Assert.assertEquals(
                listOf("-d", "-e", "-f", "-f", "-e", "-d"),
                myOpts.def)
        Assert.assertEquals(
                listOf("-a", "-b", "-c", "-c", "-b", "-a"),
                myOpts.abc)
    }

    @Test
    fun testMixedShortOptionsWithArgs() {
        class Opts(args: Array<String>) {
            private val parser = OptionParser(args)
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

        val myOpts = Opts(arrayOf("-adecfy5", "-x0", "-bzxy"))

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
        class Opts(args: Array<String>) {
            private val parser = OptionParser(args)
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zebra", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
        }

        Assert.assertEquals(
                listOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow"),
                Opts(arrayOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow")).xyz)

        Assert.assertEquals(
                listOf("--xray", "--yellow", "--zebra"),
                Opts(arrayOf("--xray", "--yellow", "--zebra")).xyz)
    }

    @Test
    fun testLongOptionsWithArgs() {
        class Opts(args: Array<String>) {
            private val parser = OptionParser(args)
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zaphod", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name:${next()}")
                }
            }
        }

        // Test with value as separate arg
        Assert.assertEquals(
                listOf("--xray:0", "--yellow:1", "--zaphod:2", "--zaphod:3", "--yellow:4"),
                Opts(arrayOf("--xray", "0", "--yellow", "1", "--zaphod", "2", "--zaphod", "3", "--yellow", "4")).xyz)


        // Test with = between option and value
        Assert.assertEquals(
                listOf("--xray:0", "--yellow:1", "--zaphod:2", "--zaphod:3", "--yellow:4"),
                Opts(arrayOf("--xray=0", "--yellow=1", "--zaphod=2", "--zaphod=3", "--yellow=4")).xyz)
    }

    @Test
    fun testLongOptionsWithConcatenatedArgs() {
        class Opts(args: Array<String>) {
            private val parser = OptionParser(args)
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zaphod", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name:${next()}")
                }
            }
        }

        thrown.expect(UnrecognizedOptionException::class.java)
        thrown.expectMessage("unrecognized option '--xray0'")
        Opts(arrayOf("--xray0", "--yellow1", "--zaphod2", "--zaphod3", "--yellow4")).xyz
    }

    @Test
    fun testDefault() {
        class Opts(args: Array<String>) {
            private val parser = OptionParser(args)
            val x by parser.option<Int>("-x", valueName = "ARG_NAME"){
                next().toInt()
            }.default(5)
        }

        // Test with no value
        Assert.assertEquals(
                5,
                Opts(arrayOf()).x)

        // Test with value
        Assert.assertEquals(
                6,
                Opts(arrayOf("-x6")).x)

        // Test with value as separate arg
        Assert.assertEquals(
                7,
                Opts(arrayOf("-x", "7")).x)

        // Test with multiple values
        Assert.assertEquals(
                8,
                Opts(arrayOf("-x9", "-x8")).x)
    }

    @Test
    fun testFlag() {
        class Opts(args: Array<String>) {
            private val parser = OptionParser(args)
            val x by parser.flagging("-x", "--ecks")
            val y by parser.flagging("-y")
            val z by parser.flagging("--zed")
        }

        val opts1 = Opts(arrayOf("-x", "-y", "--zed", "--zed", "-y"))
        Assert.assertTrue(opts1.x)
        Assert.assertTrue(opts1.y)
        Assert.assertTrue(opts1.z)

        val opts2 = Opts(arrayOf())
        Assert.assertFalse(opts2.x)
        Assert.assertFalse(opts2.y)
        Assert.assertFalse(opts2.z)

        val opts3 = Opts(arrayOf("-y", "--ecks"))
        Assert.assertTrue(opts3.x)
        Assert.assertTrue(opts3.y)

        val opts4 = Opts(arrayOf("--zed"))
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
                Opts(OptionParser(arrayOf("-x", "foo"))).x)

        Assert.assertEquals("baz",
                Opts(OptionParser(arrayOf("-x", "bar", "-x", "baz"))).x)

        Assert.assertEquals("short",
                Opts(OptionParser(arrayOf("--ecks", "long", "-x", "short"))).x)

        Assert.assertEquals("long",
                Opts(OptionParser(arrayOf("-x", "short", "--ecks", "long"))).x)
    }

    @Test
    fun testArgument_missing_long() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("--ecks")
        }

        val opts = Opts(OptionParser(arrayOf()))
        thrown.expect(MissingValueException::class.java)
        thrown.expectMessage("missing ECKS")
        opts.x
    }

    @Test
    fun testArgument_missing_short() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("-x")
        }

        val opts = Opts(OptionParser(arrayOf()))
        thrown.expect(MissingValueException::class.java)
        thrown.expectMessage("missing X")
        opts.x
    }

    @Test
    fun testArgument_missingShortAndLong() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("--ecks", "-x")
        }

        val opts = Opts(OptionParser(arrayOf()))
        thrown.expect(MissingValueException::class.java)
        thrown.expectMessage("missing ECKS")
        opts.x
    }

    @Test
    fun testArgument_withParser() {
        class Opts(args: Array<String>) {
            private val parser = OptionParser(args)
            val x by parser.storing("-x", "--ecks"){toInt()}
        }

        val opts1 = Opts(arrayOf("-x", "5"))
        Assert.assertEquals(5, opts1.x)

        val opts2 = Opts(arrayOf("-x", "1", "-x", "2"))
        Assert.assertEquals(2, opts2.x)

        val opts3 = Opts(arrayOf("--ecks", "3", "-x", "4"))
        Assert.assertEquals(4, opts3.x)

        val opts4 = Opts(arrayOf("-x", "5", "--ecks", "6"))
        Assert.assertEquals(6, opts4.x)
    }

    @Test
    fun testArgument_missing_withParser() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("-x", "--ecks"){toInt()}
        }

        val opts = Opts(OptionParser(arrayOf()))
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
                Opts(OptionParser(arrayOf())).x)

        Assert.assertEquals(
                listOf("foo"),
                Opts(OptionParser(arrayOf("-x", "foo"))).x)

        Assert.assertEquals(
                listOf("bar", "baz"),
                Opts(OptionParser(arrayOf("-x", "bar", "-x", "baz"))).x)

        Assert.assertEquals(
                listOf("long", "short"),
                Opts(OptionParser(arrayOf("--ecks", "long", "-x", "short"))).x)

        Assert.assertEquals(
                listOf("short", "long"),
                Opts(OptionParser(arrayOf("-x", "short", "--ecks", "long"))).x)
    }

    @Test
    fun testAccumulator_withParser() {
        class Opts(args: Array<String>) {
            private val parser = OptionParser(args)
            val x by parser.adding("-x", "--ecks"){toInt()}
        }

        Assert.assertEquals(listOf<Int>(), Opts(arrayOf()).x)
        Assert.assertEquals(listOf(5), Opts(arrayOf("-x", "5")).x)
        Assert.assertEquals(listOf(1, 2), Opts(arrayOf("-x", "1", "-x", "2")).x)
        Assert.assertEquals(listOf(3, 4), Opts(arrayOf("--ecks", "3", "-x", "4")).x)
        Assert.assertEquals(listOf(5, 6), Opts(arrayOf("-x", "5", "--ecks", "6")).x)
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
        Assert.assertEquals(Color.RED,   ColorOpts(OptionParser(arrayOf("--red"))).color)
        Assert.assertEquals(Color.GREEN, ColorOpts(OptionParser(arrayOf("--green"))).color)
        Assert.assertEquals(Color.BLUE,  ColorOpts(OptionParser(arrayOf("--blue"))).color)

        // Last one takes precedence
        Assert.assertEquals(Color.RED,   ColorOpts(OptionParser(arrayOf("--blue", "--red"))).color)
        Assert.assertEquals(Color.GREEN, ColorOpts(OptionParser(arrayOf("--blue", "--green"))).color)
        Assert.assertEquals(Color.BLUE,  ColorOpts(OptionParser(arrayOf("--red", "--blue"))).color)
    }

    @Test
    fun testMapping_noArgs() {
        val opts = ColorOpts(OptionParser(arrayOf()))
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
        Assert.assertEquals(Color.RED,   OptionalColorOpts(OptionParser(arrayOf("--red"))).color)
        Assert.assertEquals(Color.GREEN, OptionalColorOpts(OptionParser(arrayOf("--green"))).color)
        Assert.assertEquals(Color.BLUE,  OptionalColorOpts(OptionParser(arrayOf("--blue"))).color)
        Assert.assertEquals(Color.GREEN, OptionalColorOpts(OptionParser(arrayOf())).color)
    }

    @Test
    fun testUnrecognizedShortOpt() {
        thrown.expect(UnrecognizedOptionException::class.java)
        thrown.expectMessage("unrecognized option '-x'")
        OptionalColorOpts(OptionParser(arrayOf("-x"))).color
    }

    @Test
    fun testUnrecognizedLongOpt() {
        thrown.expect(UnrecognizedOptionException::class.java)
        thrown.expectMessage("unrecognized option '--ecks'")
        OptionalColorOpts(OptionParser(arrayOf("--ecks"))).color
    }

    @Test
    fun testShortStoringNoArg() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("-x", "--ecks")
        }

        // Note that name actually used for option is used in message
        thrown.expect(OptionMissingRequiredArgumentException::class.java)
        thrown.expectMessage("option '-x' is missing a required argument")
        Opts(OptionParser(arrayOf("-x"))).x
    }

    @Test
    fun testLongStoringNoArg() {
        class Opts(parser: OptionParser) {
            val x by parser.storing("-x", "--ecks")
        }

        // Note that name actually used for option is used in message
        thrown.expect(OptionMissingRequiredArgumentException::class.java)
        thrown.expectMessage("option '--ecks' is missing a required argument")
        Opts(OptionParser(arrayOf("--ecks"))).x
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
        Opts(OptionParser(arrayOf("-yx"))).x
    }
}
