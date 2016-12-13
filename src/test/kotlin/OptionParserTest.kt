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

    private fun optionParser(args: Array<String>) = OptionParser(testName.getMethodName(), args)

    @Test
    fun testArglessShortOptions() {
        class MyOpts(args: Array<String>) {
            private val parser = optionParser(args)
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
        }

        Assert.assertEquals(
                listOf("-x", "-y", "-z", "-z", "-y"),
                MyOpts(arrayOf("-x", "-y", "-z", "-z", "-y")).xyz)

        Assert.assertEquals(
                listOf("-x", "-y", "-z"),
                MyOpts(arrayOf("-xyz")).xyz)
    }

    @Test
    fun testShortOptionsWithArgs() {
        class MyOpts(args: Array<String>) {
            private val parser = optionParser(args)
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name:${next()}")
                }
            }
        }

        // Test with value as separate arg
        Assert.assertEquals(
                listOf("-x:0", "-y:1", "-z:2", "-z:3", "-y:4"),
                MyOpts(arrayOf("-x", "0", "-y", "1", "-z", "2", "-z", "3", "-y", "4")).xyz)

        // Test with value concatenated
        Assert.assertEquals(
                listOf("-x:0", "-y:1", "-z:2", "-z:3", "-y:4"),
                MyOpts(arrayOf("-x0", "-y1", "-z2", "-z3", "-y4")).xyz)

        // Test with = between option and value
        Assert.assertEquals(
                listOf("-x:=0", "-y:=1", "-z:=2", "-z:=3", "-y:=4"),
                MyOpts(arrayOf("-x=0", "-y=1", "-z=2", "-z=3", "-y=4")).xyz)

        // TODO test with chaining
    }

    @Test
    fun testMixedShortOptions() {
        class MyOpts(args: Array<String>) {
            private val parser = optionParser(args)
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

        val myOpts = MyOpts(arrayOf("-adbefccbafed"))

        Assert.assertEquals(
                listOf("-d", "-e", "-f", "-f", "-e", "-d"),
                myOpts.def)
        Assert.assertEquals(
                listOf("-a", "-b", "-c", "-c", "-b", "-a"),
                myOpts.abc)
    }

    @Test
    fun testMixedShortOptionsWithArgs() {
        class MyOpts(args: Array<String>) {
            private val parser = optionParser(args)
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

        val myOpts = MyOpts(arrayOf("-adecfy5", "-x0", "-bzxy"))

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
        class MyOpts(args: Array<String>) {
            private val parser = optionParser(args)
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zebra", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
        }

        Assert.assertEquals(
                listOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow"),
                MyOpts(arrayOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow")).xyz)

        Assert.assertEquals(
                listOf("--xray", "--yellow", "--zebra"),
                MyOpts(arrayOf("--xray", "--yellow", "--zebra")).xyz)
    }

    @Test
    fun testLongOptionsWithArgs() {
        class MyOpts(args: Array<String>) {
            private val parser = optionParser(args)
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zaphod", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name:${next()}")
                }
            }
        }

        // Test with value as separate arg
        Assert.assertEquals(
                listOf("--xray:0", "--yellow:1", "--zaphod:2", "--zaphod:3", "--yellow:4"),
                MyOpts(arrayOf("--xray", "0", "--yellow", "1", "--zaphod", "2", "--zaphod", "3", "--yellow", "4")).xyz)


        // Test with = between option and value
        Assert.assertEquals(
                listOf("--xray:0", "--yellow:1", "--zaphod:2", "--zaphod:3", "--yellow:4"),
                MyOpts(arrayOf("--xray=0", "--yellow=1", "--zaphod=2", "--zaphod=3", "--yellow=4")).xyz)
    }

    @Test
    fun testLongOptionsWithConcatenatedArgs() {
        class MyOpts(args: Array<String>) {
            private val parser = optionParser(args)
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zaphod", valueName = "ARG_NAME"){
                value.orElse{mutableListOf<String>()}.apply {
                    add("$name:${next()}")
                }
            }
        }

        thrown.expect(UnrecognizedOptionException::class.java)
        thrown.expectMessage("unrecognized option '--xray0'")
        MyOpts(arrayOf("--xray0", "--yellow1", "--zaphod2", "--zaphod3", "--yellow4")).xyz
    }

    @Test
    fun testDefault() {
        class MyOpts(args: Array<String>) {
            private val parser = optionParser(args)
            val x by parser.option<Int>("-x", valueName = "ARG_NAME"){
                next().toInt()
            }.default(5)
        }

        // Test with no value
        Assert.assertEquals(
                5,
                MyOpts(arrayOf()).x)

        // Test with value
        Assert.assertEquals(
                6,
                MyOpts(arrayOf("-x6")).x)

        // Test with value as separate arg
        Assert.assertEquals(
                7,
                MyOpts(arrayOf("-x", "7")).x)

        // Test with multiple values
        Assert.assertEquals(
                8,
                MyOpts(arrayOf("-x9", "-x8")).x)
    }

    @Test
    fun testFlag() {
        class MyOpts(args: Array<String>) {
            private val parser = optionParser(args)
            val x by parser.flagging("-x", "--ecks")
            val y by parser.flagging("-y")
            val z by parser.flagging("--zed")
        }

        val opts1 = MyOpts(arrayOf("-x", "-y", "--zed", "--zed", "-y"))
        Assert.assertTrue(opts1.x)
        Assert.assertTrue(opts1.y)
        Assert.assertTrue(opts1.z)

        val opts2 = MyOpts(arrayOf())
        Assert.assertFalse(opts2.x)
        Assert.assertFalse(opts2.y)
        Assert.assertFalse(opts2.z)

        val opts3 = MyOpts(arrayOf("-y", "--ecks"))
        Assert.assertTrue(opts3.x)
        Assert.assertTrue(opts3.y)

        val opts4 = MyOpts(arrayOf("--zed"))
        Assert.assertFalse(opts4.x)
        Assert.assertFalse(opts4.y)
        Assert.assertTrue(opts4.z)
    }

    @Test
    fun testArgument_noParser() {
        class StoringNoParserOpts(parser: OptionParser) {
            val x by parser.storing("--ecks", "-x")
        }

        Assert.assertEquals("foo",
                StoringNoParserOpts(optionParser(arrayOf("-x", "foo"))).x)

        Assert.assertEquals("baz",
                StoringNoParserOpts(optionParser(arrayOf("-x", "bar", "-x", "baz"))).x)

        Assert.assertEquals("short",
                StoringNoParserOpts(optionParser(arrayOf("--ecks", "long", "-x", "short"))).x)

        Assert.assertEquals("long",
                StoringNoParserOpts(optionParser(arrayOf("-x", "short", "--ecks", "long"))).x)
    }

    @Test
    fun testArgument_missing_long() {
        class StoringNoParserOpts(parser: OptionParser) {
            val x by parser.storing("--ecks")
        }

        val opts = StoringNoParserOpts(optionParser(arrayOf()))
        thrown.expect(MissingValueException::class.java)
        thrown.expectMessage("missing ECKS")
        opts.x
    }

    @Test
    fun testArgument_missing_short() {
        class StoringNoParserOpts(parser: OptionParser) {
            val x by parser.storing("-x")
        }

        val opts = StoringNoParserOpts(optionParser(arrayOf()))
        thrown.expect(MissingValueException::class.java)
        thrown.expectMessage("missing X")
        opts.x
    }

    @Test
    fun testArgument_missingShortAndLong() {
        class StoringNoParserOpts(parser: OptionParser) {
            val x by parser.storing("--ecks", "-x")
        }

        val opts = StoringNoParserOpts(optionParser(arrayOf()))
        thrown.expect(MissingValueException::class.java)
        thrown.expectMessage("missing ECKS")
        opts.x
    }

    @Test
    fun testArgument_withParser() {
        class MyOpts(args: Array<String>) {
            private val parser = optionParser(args)
            val x by parser.storing("-x", "--ecks"){toInt()}
        }

        val opts1 = MyOpts(arrayOf("-x", "5"))
        Assert.assertEquals(5, opts1.x)

        val opts2 = MyOpts(arrayOf("-x", "1", "-x", "2"))
        Assert.assertEquals(2, opts2.x)

        val opts3 = MyOpts(arrayOf("--ecks", "3", "-x", "4"))
        Assert.assertEquals(4, opts3.x)

        val opts4 = MyOpts(arrayOf("-x", "5", "--ecks", "6"))
        Assert.assertEquals(6, opts4.x)

        // TODO test missing
    }

    @Test
    fun testAccumulator_noParser() {
        class MyOpts(parser: OptionParser) {
            val x by parser.adding("-x", "--ecks")
        }

        Assert.assertEquals(
                listOf<String>(),
                MyOpts(optionParser(arrayOf())).x)

        Assert.assertEquals(
                listOf("foo"),
                MyOpts(optionParser(arrayOf("-x", "foo"))).x)

        Assert.assertEquals(
                listOf("bar", "baz"),
                MyOpts(optionParser(arrayOf("-x", "bar", "-x", "baz"))).x)

        Assert.assertEquals(
                listOf("long", "short"),
                MyOpts(optionParser(arrayOf("--ecks", "long", "-x", "short"))).x)

        Assert.assertEquals(
                listOf("short", "long"),
                MyOpts(optionParser(arrayOf("-x", "short", "--ecks", "long"))).x)
    }

    @Test
    fun testAccumulator_withParser() {
        class MyOpts(args: Array<String>) {
            private val parser = optionParser(args)
            val x by parser.adding("-x", "--ecks"){toInt()}
        }

        Assert.assertEquals(listOf<Int>(), MyOpts(arrayOf()).x)
        Assert.assertEquals(listOf(5), MyOpts(arrayOf("-x", "5")).x)
        Assert.assertEquals(listOf(1, 2), MyOpts(arrayOf("-x", "1", "-x", "2")).x)
        Assert.assertEquals(listOf(3, 4), MyOpts(arrayOf("--ecks", "3", "-x", "4")).x)
        Assert.assertEquals(listOf(5, 6), MyOpts(arrayOf("-x", "5", "--ecks", "6")).x)
    }

    enum class Color { RED, GREEN, BLUE }

    @Test
    fun testMapping() {
        class MyOpts(args: Array<String>) {
            private val parser = optionParser(args)
            val color by parser.mapping(
                    "--red" to Color.RED,
                    "--green" to Color.GREEN,
                    "--blue" to Color.BLUE,
                    "--null" to null)
        }

        Assert.assertEquals(Color.RED,   MyOpts(arrayOf("--red")).color)
        Assert.assertEquals(Color.GREEN, MyOpts(arrayOf("--green")).color)
        Assert.assertEquals(Color.BLUE,  MyOpts(arrayOf("--blue")).color)

        // Last one takes precedence
        Assert.assertEquals(Color.RED,   MyOpts(arrayOf("--blue", "--red")).color)
        Assert.assertEquals(Color.GREEN, MyOpts(arrayOf("--blue", "--green")).color)
        Assert.assertEquals(Color.BLUE,  MyOpts(arrayOf("--red", "--blue")).color)

        // TODO: test with no args
        // TODO: test with default set
    }

    // TODO: test UnrecognizedOptionException
    // TODO: test short option needs arg at end
    // TODO: test long option needs arg at end
    // TODO: test printAndExit()
    // TODO: change help into a Delegate method
}
