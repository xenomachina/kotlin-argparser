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

import com.xenomachina.argparser.PosixNaming.identifierToOptionName
import com.xenomachina.common.orElse
import io.kotlintest.matchers.beOfType
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import java.io.File
import java.io.StringWriter

class ArgParserTest : FunSpec({
    test("Option name validation") {
        val parser = parserOf()

        // These are all acceptable.
        parser.option<Int>("-x", help = TEST_HELP) { 0 }
        parser.option<Int>("--x", help = TEST_HELP) { 0 }
        parser.option<Int>("--xy", help = TEST_HELP) { 0 }
        parser.option<Int>("-X", help = TEST_HELP) { 0 }
        parser.option<Int>("--X", help = TEST_HELP) { 0 }
        parser.option<Int>("--XY", help = TEST_HELP) { 0 }
        parser.option<Int>("--X-Y", help = TEST_HELP) { 0 }
        parser.option<Int>("--X_Y", help = TEST_HELP) { 0 }
        parser.option<Int>("-5", help = TEST_HELP) { 0 }
        parser.option<Int>("--5", help = TEST_HELP) { 0 }
        parser.option<Int>("--5Y", help = TEST_HELP) { 0 }
        parser.option<Int>("--X5", help = TEST_HELP) { 0 }
        parser.option<Int>("--x.y", help = TEST_HELP) { 0 }

        shouldThrow<IllegalArgumentException> {
            parser.option<Int>("-_", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.option<Int>("---x", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.option<Int>("x", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.option<Int>("", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.option<Int>("-xx", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.option<Int>("--foo bar", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.option<Int>("--foo--bar", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.option<Int>("--f!oobar", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.option<Int>("--.", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.option<Int>("--.foo", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.option<Int>("--foo.", help = TEST_HELP) { 0 }
        }
    }

    test("Positional name validation") {
        val parser = parserOf()

        // These are all acceptable.
        parser.positional<Int>("X", help = TEST_HELP) { 0 }
        parser.positional<Int>("XYZ", help = TEST_HELP) { 0 }
        parser.positional<Int>("XY-Z", help = TEST_HELP) { 0 }
        parser.positional<Int>("XY_Z", help = TEST_HELP) { 0 }
        parser.positional<Int>("XY.Z", help = TEST_HELP) { 0 }

        shouldThrow<IllegalArgumentException> {
            parser.positional<Int>("-", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.positional<Int>("_", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.positional<Int>("x", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.positional<Int>("", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.positional<Int>("-X", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.positional<Int>("X-", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.positional<Int>("X--Y", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.positional<Int>("X!", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.positional<Int>("5", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.positional<Int>(".", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.positional<Int>(".XY", help = TEST_HELP) { 0 }
        }

        shouldThrow<IllegalArgumentException> {
            parser.positional<Int>("XY.", help = TEST_HELP) { 0 }
        }

        // This should be acceptable
        parser.option<Int>("--foobar", argNames = listOf("X-Y"), help = TEST_HELP) { 0 }

        // This should not
        shouldThrow<IllegalArgumentException> {
            parser.option<Int>("--foobar", argNames = listOf("X--Y"), help = TEST_HELP) { 0 }
        }
    }

    test("Argless short options") {
        class Args(parser: ArgParser) {
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z",
                help = TEST_HELP) {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
        }

        Args(parserOf("-x", "-y", "-z", "-z", "-y")).xyz shouldBe listOf("-x", "-y", "-z", "-z", "-y")

        Args(parserOf("-xyz")).xyz shouldBe listOf("-x", "-y", "-z")
    }

    test("Short options with args") {
        class Args(parser: ArgParser) {
            val a by parser.flagging("-a", help = TEST_HELP)
            val b by parser.flagging("-b", help = TEST_HELP)
            val c by parser.flagging("-c", help = TEST_HELP)
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z",
                argNames = oneArgName, help = TEST_HELP) {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName:${arguments.first()}")
                }
            }
        }

        // Test with value as separate arg
        Args(parserOf("-x", "0", "-y", "1", "-z", "2", "-z", "3", "-y", "4")).xyz shouldBe listOf("-x:0", "-y:1", "-z:2", "-z:3", "-y:4")

        // Test with value concatenated
        Args(parserOf("-x0", "-y1", "-z2", "-z3", "-y4")).xyz shouldBe listOf("-x:0", "-y:1", "-z:2", "-z:3", "-y:4")

        // Test with = between option and value. Note that the "=" is treated as part of the option value for short options.
        Args(parserOf("-x=0", "-y=1", "-z=2", "-z=3", "-y=4")).xyz shouldBe listOf("-x:=0", "-y:=1", "-z:=2", "-z:=3", "-y:=4")

        // Test chained options. Note that an option with arguments must be last in the chain
        val chain1 = Args(parserOf("-abxc"))
        chain1.a shouldBe true
        chain1.b shouldBe true
        chain1.c shouldBe false
        chain1.xyz shouldBe listOf("-x:c")

        val chain2 = Args(parserOf("-axbc"))
        chain2.a shouldBe true
        chain2.b shouldBe false
        chain2.c shouldBe false
        chain2.xyz shouldBe listOf("-x:bc")
    }

    test("Mixed short options") {
        class Args(parser: ArgParser) {
            val def by parser.option<MutableList<String>>("-d", "-e", "-f",
                help = TEST_HELP) {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
            val abc by parser.option<MutableList<String>>("-a", "-b", "-c",
                help = TEST_HELP) {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
        }

        Args(parserOf("-adbefccbafed")).run {
            def shouldBe listOf("-d", "-e", "-f", "-f", "-e", "-d")
            abc shouldBe listOf("-a", "-b", "-c", "-c", "-b", "-a")
        }
    }

    test("Mixed short options with args") {
        class Args(parser: ArgParser) {
            val def by parser.option<MutableList<String>>("-d", "-e", "-f",
                help = TEST_HELP) {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
            val abc by parser.option<MutableList<String>>("-a", "-b", "-c",
                help = TEST_HELP) {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
            val xyz by parser.option<MutableList<String>>("-x", "-y", "-z",
                argNames = oneArgName,
                help = TEST_HELP) {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName:${arguments.first()}")
                }
            }
        }

        Args(parserOf("-adecfy5", "-x0", "-bzxy")).run {
            abc shouldBe listOf("-a", "-c", "-b")
            def shouldBe listOf("-d", "-e", "-f")
            xyz shouldBe listOf("-y:5", "-x:0", "-z:xy")
        }
    }

    test("Argless long options") {
        class Args(parser: ArgParser) {
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zebra",
                help = TEST_HELP) {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
        }

        Args(parserOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow")).xyz shouldBe listOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow")

        Args(parserOf("--xray", "--yellow", "--zebra")).xyz shouldBe listOf("--xray", "--yellow", "--zebra")
    }

    test("Dotted long options") {
        class Args(parser: ArgParser) {
            val xyz by parser.option<MutableList<String>>("--x.ray", "--color.yellow", "--animal.zebra",
                help = TEST_HELP) {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName")
                }
            }
        }

        Args(parserOf("--x.ray", "--animal.zebra", "--color.yellow", "--x.ray")).xyz shouldBe listOf("--x.ray", "--animal.zebra", "--color.yellow", "--x.ray")
    }

    test("Long options with one arg") {
        class Args(parser: ArgParser) {
            val xyz by parser.option<MutableList<String>>("--xray", "--yellow", "--zaphod",
                argNames = oneArgName,
                help = TEST_HELP) {
                value.orElse { mutableListOf<String>() }.apply {
                    add("$optionName:${arguments.first()}")
                }
            }
        }

        // Test with value as separate arg
        Args(parserOf("--xray", "0", "--yellow", "1", "--zaphod", "2", "--zaphod", "3", "--yellow", "4")).xyz shouldBe listOf("--xray:0", "--yellow:1", "--zaphod:2", "--zaphod:3", "--yellow:4")

        // Test with = between option and value
        Args(parserOf("--xray=0", "--yellow=1", "--zaphod=2", "--zaphod=3", "--yellow=4")).xyz shouldBe listOf("--xray:0", "--yellow:1", "--zaphod:2", "--zaphod:3", "--yellow:4")

        shouldThrow<UnrecognizedOptionException> {
            Args(parserOf("--xray0", "--yellow1", "--zaphod2", "--zaphod3", "--yellow4")).xyz
        }.run {
            message shouldBe "unrecognized option '--xray0'"
        }
    }

    test("Long options with multiple args") {
        class Args(parser: ArgParser) {
            val xyz by parser.option<MutableList<String>>("--xray", "--yak", "--zaphod",
                argNames = listOf("COLOR", "SIZE", "FLAVOR"),
                help = TEST_HELP) {
                value.orElse { mutableListOf<String>() }.apply { add("$optionName:$arguments") }
            }
        }

        // Test with value as separate arg
        Args(parserOf("--xray", "red", "5", "salty")).xyz shouldBe listOf("--xray:[red, 5, salty]")

        Args(parserOf("--zaphod", "green", "42", "sweet", "--yak", "blue", "7", "bitter")).xyz shouldBe listOf(
            "--zaphod:[green, 42, sweet]", "--yak:[blue, 7, bitter]")

        // Note that something that looks like an option is consumed as an argument if it appears where an argument
        // should be. This is expected behavior.
        Args(parserOf("--zaphod", "green", "42", "--yak")).xyz shouldBe listOf(
            "--zaphod:[green, 42, --yak]")

        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("--zaphod", "green", "42", "sweet", "--yak", "blue", "7")).xyz
        }.run {
            message shouldBe "option '--yak' is missing the required argument FLAVOR"
        }

        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("--zaphod", "green")).xyz
        }.run {
            message shouldBe "option '--zaphod' is missing the required argument SIZE"
        }

        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("--xray")).xyz
        }.run {
            message shouldBe "option '--xray' is missing the required argument COLOR"
        }
    }

    test("Delegate provider") {
        fun ArgParser.putting(vararg names: String, help: String) =
            option<MutableMap<String, String>>(*names,
                argNames = listOf("KEY", "VALUE"),
                help = help) {
                value.orElse { mutableMapOf<String, String>() }.apply {
                    put(arguments.first(), arguments.last()) }
            }

        fun ArgParser.putting(help: String) =
            ArgParser.DelegateProvider { identifier ->
                putting(identifierToOptionName(identifier), help = help) }

        class Args(parser: ArgParser) {
            val dict by parser.putting(TEST_HELP)
        }

        // Test with value as separate arg
        Args(parserOf("--dict", "red", "5")).dict shouldBe mapOf("red" to "5")

        Args(parserOf(
            "--dict", "green", "42",
            "--dict", "blue", "7"
        )).dict shouldBe mapOf(
            "green" to "42",
            "blue" to "7")

        // Note that something that looks like an option is consumed as an argument if it appears where an argument
        // should be. This is expected behavior.
        Args(parserOf("--dict", "green", "--dict")).dict shouldBe mapOf("green" to "--dict")

        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("--dict", "green", "42", "--dict", "blue")).dict
        }.run {
            message shouldBe "option '--dict' is missing the required argument VALUE"
        }

        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("--dict")).dict
        }.run {
            message shouldBe "option '--dict' is missing the required argument KEY"
        }
    }

    test("Default") {
        class Args(parser: ArgParser) {
            val x by parser.storing("-x",
                help = TEST_HELP) { toInt() }.default(5)
        }

        // Test with no value
        Args(parserOf()).x shouldBe 5

        // Test with value
        Args(parserOf("-x6")).x shouldBe 6

        // Test with value as separate arg
        Args(parserOf("-x", "7")).x shouldBe 7

        // Test with multiple values
        Args(parserOf("-x9", "-x8")).x shouldBe 8
    }

    test("DDefault with providerefault") {
        class Args(parser: ArgParser) {
            val x by parser.storing(help = TEST_HELP) { toInt() }.default(5)
        }

        // Test with no value
        Args(parserOf()).x shouldBe 5

        // Test with value
        Args(parserOf("-x6")).x shouldBe 6

        // Test with value as separate arg
        Args(parserOf("-x", "7")).x shouldBe 7

        // Test with multiple values
        Args(parserOf("-x9", "-x8")).x shouldBe 8
    }

    test("Default with la") {
        class Args(parser: ArgParser) {
            var defaultCalled = false
            val x by parser.storing(help = TEST_HELP) { toInt() }.default { defaultCalled = true; 5 }
        }

        // Test default hasn't been called
        Args(parserOf("-x6")).defaultCalled shouldBe false

        // Test with no value
        val args = Args(parserOf())
        args.x shouldBe 5
        args.defaultCalled shouldBe true
    }

    test("Flag") {
        class Args(parser: ArgParser) {
            val x by parser.flagging("-x", "--ecks",
                help = TEST_HELP)
            val y by parser.flagging("-y",
                help = TEST_HELP)
            val z by parser.flagging("--zed",
                help = TEST_HELP)
        }

        Args(parserOf("-x", "-y", "--zed", "--zed", "-y")).run {
            x shouldBe true
            y shouldBe true
            z shouldBe true
        }

        Args(parserOf()).run {
            x shouldBe false
            y shouldBe false
            z shouldBe false
        }

        Args(parserOf("-y", "--ecks")).run {
            x shouldBe true
            y shouldBe true
        }

        Args(parserOf("--zed")).run {
            x shouldBe false
            y shouldBe false
            z shouldBe true
        }
    }

    test("Argument no parser") {
        class Args(parser: ArgParser) {
            val x by parser.storing("--ecks", "-x",
                help = TEST_HELP)
        }

        Args(parserOf("-x", "foo")).x shouldBe "foo"

        Args(parserOf("-x", "bar", "-x", "baz")).x shouldBe "baz"

        Args(parserOf("--ecks", "long", "-x", "short")).x shouldBe "short"

        Args(parserOf("-x", "short", "--ecks", "long")).x shouldBe "long"

        val args = Args(parserOf())
        shouldThrow<MissingValueException> {
            args.x
        }.run {
            message shouldBe "missing ECKS"
        }
    }

    test("Argument missing long") {
        class Args(parser: ArgParser) {
            val x by parser.storing("--ecks",
                help = TEST_HELP)
        }

        val args = Args(parserOf())
        shouldThrow<MissingValueException> {
            args.x
        }.run {
            message shouldBe "missing ECKS"
        }
    }

    test("Argument missing short") {
        class Args(parser: ArgParser) {
            val x by parser.storing("-x",
                help = TEST_HELP)
        }

        val args = Args(parserOf())
        shouldThrow<MissingValueException> {
            args.x
        }.run {
            message shouldBe "missing X"
        }
    }

    test("Argument withParser") {
        class Args(parser: ArgParser) {
            val x by parser.storing("-x", "--ecks",
                help = TEST_HELP) { toInt() }
        }

        val opts1 = Args(parserOf("-x", "5"))
        opts1.x shouldBe 5

        val opts2 = Args(parserOf("-x", "1", "-x", "2"))
        opts2.x shouldBe 2

        val opts3 = Args(parserOf("--ecks", "3", "-x", "4"))
        opts3.x shouldBe 4

        val opts4 = Args(parserOf("-x", "5", "--ecks", "6"))
        opts4.x shouldBe 6

        val opts6 = Args(parserOf())
        shouldThrow<MissingValueException> {
            opts6.x
        }.run {
            message shouldBe "missing ECKS"
        }
    }

    test("Accumulator noParser") {
        class Args(parser: ArgParser) {
            val x by parser.adding("-x", "--ecks",
                help = TEST_HELP)
        }

        Args(parserOf()).x shouldBe listOf<String>()

        Args(parserOf("-x", "foo")).x shouldBe listOf("foo")

        Args(parserOf("-x", "bar", "-x", "baz")).x shouldBe listOf("bar", "baz")

        Args(parserOf("--ecks", "long", "-x", "short")).x shouldBe listOf("long", "short")

        Args(parserOf("-x", "short", "--ecks", "long")).x shouldBe listOf("short", "long")
    }

    test("Accumulator withParser") {
        class Args(parser: ArgParser) {
            val x by parser.adding("-x", "--ecks",
                help = TEST_HELP) { toInt() }
        }

        Args(parserOf()).x shouldBe listOf<Int>()
        Args(parserOf("-x", "5")).x shouldBe listOf(5)
        Args(parserOf("-x", "1", "-x", "2")).x shouldBe listOf(1, 2)
        Args(parserOf("--ecks", "3", "-x", "4")).x shouldBe listOf(3, 4)
        Args(parserOf("-x", "5", "--ecks", "6")).x shouldBe listOf(5, 6)
    }

    class ColorArgs(parser: ArgParser) {
        val color by parser.mapping(
            "--red" to Color.RED,
            "--green" to Color.GREEN,
            "--blue" to Color.BLUE,
            help = TEST_HELP)
    }

    test("Mapping") {
        ColorArgs(parserOf("--red")).color shouldBe Color.RED
        ColorArgs(parserOf("--green")).color shouldBe Color.GREEN
        ColorArgs(parserOf("--blue")).color shouldBe Color.BLUE

        // Last one takes precedence
        ColorArgs(parserOf("--blue", "--red")).color shouldBe Color.RED
        ColorArgs(parserOf("--blue", "--green")).color shouldBe Color.GREEN
        ColorArgs(parserOf("--red", "--blue")).color shouldBe Color.BLUE

        val args = ColorArgs(parserOf())
        shouldThrow<MissingValueException> {
            args.color
        }.run {
            message shouldBe "missing --red|--green|--blue"
        }
    }

    class OptionalColorArgs(parser: ArgParser) {
        val color by parser.mapping(
            "--red" to Color.RED,
            "--green" to Color.GREEN,
            "--blue" to Color.BLUE,
            help = TEST_HELP)
            .default(Color.GREEN)
    }

    test("Mapping withDefault") {
        OptionalColorArgs(parserOf("--red")).color shouldBe Color.RED
        OptionalColorArgs(parserOf("--green")).color shouldBe Color.GREEN
        OptionalColorArgs(parserOf("--blue")).color shouldBe Color.BLUE
        OptionalColorArgs(parserOf()).color shouldBe Color.GREEN
    }

    test("Unrecognized short opt") {
        shouldThrow<UnrecognizedOptionException> {
            OptionalColorArgs(parserOf("-x")).color
        }.run {
            message shouldBe "unrecognized option '-x'"
        }
    }

    test("Unrecognized long opt") {
        shouldThrow<UnrecognizedOptionException> {
            OptionalColorArgs(parserOf("--ecks")).color
        }.run {
            message shouldBe "unrecognized option '--ecks'"
        }
    }

    test("Storing no arg") {
        class Args(parser: ArgParser) {
            val x by parser.storing("-x", "--ecks",
                help = TEST_HELP)
        }

        // Note that name actually used for option is used in message
        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("-x")).x
        }.run {
            message shouldBe "option '-x' is missing a required argument"
        }

        // Note that name actually used for option is used in message
        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("--ecks")).x
        }.run {
            message shouldBe "option '--ecks' is missing a required argument"
        }
    }

    test("Short storing no arg chained") {
        class Args(parser: ArgParser) {
            val y by parser.flagging("-y",
                help = TEST_HELP)
            val x by parser.storing("-x",
                help = TEST_HELP)
        }

        // Note that despite chaining, hyphen appears in message
        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("-yx")).x
        }.run {
            message shouldBe "option '-x' is missing a required argument"
        }
    }

    test("Init validation") {
        class Args(parser: ArgParser) {
            val yDelegate = parser.storing("-y",
                help = TEST_HELP) { toInt() }
            val y by yDelegate

            val xDelegate = parser.storing("-x",
                help = TEST_HELP) { toInt() }
            val x by xDelegate

            init {
                if (y >= x)
                    throw InvalidArgumentException("${yDelegate.errorName} must be less than ${xDelegate.errorName}")

                // A better way to accomplish validation that only depends on one Delegate is to use
                // Delegate.addValidator. See testAddValidator for an example of this.
                if (x.rem(2) != 0)
                    throw InvalidArgumentException("${xDelegate.errorName} must be even, $x is odd")
            }
        }

        // This should pass validation
        val opts0 = Args(parserOf("-y1", "-x10"))
        opts0.y shouldBe 1
        opts0.x shouldBe 10

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("-y20", "-x10")).x
        }.run {
            message shouldBe "Y must be less than X"
        }

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("-y10", "-x15")).x
        }.run {
            message shouldBe "X must be even, 15 is odd"
        }

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("-y10", "-x15")).x
        }.run {
            message shouldBe "X must be even, 15 is odd"
        }
    }

    test("Add validator") {
        class Args(parser: ArgParser) {
            val yDelegate = parser.storing("-y",
                help = TEST_HELP) { toInt() }
            val y by yDelegate

            val xDelegate = parser.storing("-x",
                help = TEST_HELP) { toInt() }
                .addValidator {
                    if (value.rem(2) != 0)
                        throw InvalidArgumentException("$errorName must be even, $value is odd")
                }
            val x by xDelegate

            init {
                if (y >= x)
                    throw InvalidArgumentException("${yDelegate.errorName} must be less than ${xDelegate.errorName}")
            }
        }

        // This should pass validation
        val opts0 = Args(parserOf("-y1", "-x10"))
        opts0.y shouldBe 1
        opts0.x shouldBe 10

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("-y20", "-x10")).x
        }.run {
            message shouldBe "Y must be less than X"
        }

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("-y10", "-x15")).x
        }.run {
            message shouldBe "X must be even, 15 is odd"
        }
    }

    test("Unconsumed") {
        class Args(parser: ArgParser) {
            val y by parser.flagging("-y", "--why",
                help = TEST_HELP)
            val x by parser.flagging("-x", "--ecks",
                help = TEST_HELP)
        }

        // No problem.
        Args(parserOf("-yx")).run {
            x shouldBe true
            y shouldBe true
        }

        // Attempting to give -y a parameter, "z", is treated as unrecognized option.
        shouldThrow<UnrecognizedOptionException> {
            Args(parserOf("-yz")).y
        }.run {
            message shouldBe "unrecognized option '-z'"
        }

        // Unconsumed "z" again, but note that it triggers even if we don't look at y.
        shouldThrow<UnrecognizedOptionException> {
            Args(parserOf("-yz")).x
        }.run {
            message shouldBe "unrecognized option '-z'"
        }

        // No problem again, this time with long opts.
        Args(parserOf("--why", "--ecks")).run {
            x shouldBe true
            y shouldBe true
        }

        // Attempting to give --why a parameter, "z" causes an error.
        shouldThrow<UnexpectedOptionArgumentException> {
            Args(parserOf("--why=z")).y
        }.run {
            message shouldBe "option '--why' doesn't allow an argument"
        }

        // Unconsumed "z" again, but note that it triggers even if we don't look at y.
        shouldThrow<UnexpectedOptionArgumentException> {
            Args(parserOf("--why=z")).x
        }.run {
            message shouldBe "option '--why' doesn't allow an argument"
        }
    }

    test("Positional basic") {
        class Args(parser: ArgParser) {
            val flag by parser.flagging("-f", "--flag",
                help = TEST_HELP)
            val store by parser.storing("-s", "--store",
                help = TEST_HELP).default("DEFAULT")
            val sources by parser.positionalList("SOURCE",
                help = TEST_HELP)
            val destination by parser.positional("DEST",
                help = TEST_HELP)
        }

        Args(parserOf("foo", "bar", "baz", "quux")).run {
            flag shouldBe false
            store shouldBe "DEFAULT"
            sources shouldBe listOf("foo", "bar", "baz")
            destination shouldBe "quux"
        }

        Args(parserOf("-f", "foo", "bar", "baz", "quux")).run {
            flag shouldBe true
            store shouldBe "DEFAULT"
            sources shouldBe listOf("foo", "bar", "baz")
            destination shouldBe "quux"
        }

        Args(parserOf("-s", "foo", "bar", "baz", "quux")).run {
            flag shouldBe false
            store shouldBe "foo"
            sources shouldBe listOf("bar", "baz")
            destination shouldBe "quux"
        }

        Args(parserOf("-s", "foo", "bar", "-f", "baz", "quux")).run {
            flag shouldBe true
            store shouldBe "foo"
            sources shouldBe listOf("bar", "baz")
            destination shouldBe "quux"
        }

        // "--" disables option processing for all further arguments.
        // Note that "-f" is now considered a positional argument.
        Args(parserOf("-s", "foo", "--", "bar", "-f", "baz", "quux")).run {
            flag shouldBe false
            store shouldBe "foo"
            sources shouldBe listOf("bar", "-f", "baz")
            destination shouldBe "quux"
        }

        // "--" disables option processing for all further arguments.
        // Note that the second "--" is also considered a positional argument.
        Args(parserOf("-s", "foo", "--", "bar", "--", "-f", "baz", "quux")).run {
            flag shouldBe false
            store shouldBe "foo"
            sources shouldBe listOf("bar", "--", "-f", "baz")
            destination shouldBe "quux"
        }

        Args(parserOf("-s", "foo", "bar", "-f", "baz", "quux", mode = ArgParser.Mode.POSIX)).run {
            flag shouldBe false
            store shouldBe "foo"
            sources shouldBe listOf("bar", "-f", "baz")
            destination shouldBe "quux"
        }
    }

    test("Positional with parser") {
        class Args(parser: ArgParser) {
            val flag by parser.flagging("-f", "--flag",
                help = TEST_HELP)
            val store by parser.storing("-s", "--store",
                help = TEST_HELP).default("DEFAULT")
            val start by parser.positionalList("START", TEST_HELP, 3..4) { toInt() }
            val end by parser.positionalList("END", TEST_HELP, 3..5) { toInt() }
        }

        shouldThrow<MissingRequiredPositionalArgumentException> {
            Args(parserOf("1", "2")).flag
        }.run {
            message shouldBe "missing START operand"
        }

        shouldThrow<MissingRequiredPositionalArgumentException> {
            Args(parserOf("1", "2", "3", "4", "5")).flag
        }.run {
            message shouldBe "missing END operand"
        }

        Args(parserOf("1", "2", "3", "4", "5", "6")).run {
            flag shouldBe false
            store shouldBe "DEFAULT"

            // end needs at least 3 args, so start only consumes 3
            start shouldBe listOf(1, 2, 3)
            end shouldBe listOf(4, 5, 6)
        }

        Args(parserOf("1", "2", "3", "4", "5", "6", "7")).run {
            flag shouldBe false
            store shouldBe "DEFAULT"

            // end only needs at 3 args, so start can consume 4
            start shouldBe listOf(1, 2, 3, 4)
            end shouldBe listOf(5, 6, 7)
        }

        Args(parserOf("1", "2", "3", "4", "5", "6", "7", "8")).run {
            flag shouldBe false
            store shouldBe "DEFAULT"

            // start can't consume more than 4, so end gets the rest.
            start shouldBe listOf(1, 2, 3, 4)
            end shouldBe listOf(5, 6, 7, 8)
        }

        Args(parserOf("1", "2", "3", "4", "5", "6", "7", "8", "9")).run {
            flag shouldBe false
            store shouldBe "DEFAULT"

            // once again, start can't consume more than 4, so end gets the rest.
            start shouldBe listOf(1, 2, 3, 4)
            end shouldBe listOf(5, 6, 7, 8, 9)
        }

        shouldThrow<UnexpectedPositionalArgumentException> {
            Args(parserOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")).flag
        }.run {
            message shouldBe "unexpected argument after END"
        }
    }

    test("Counting") {
        class Args(parser: ArgParser) {
            val verbosity by parser.counting("-v", "--verbose",
                help = TEST_HELP)
        }

        Args(parserOf()).run {
            verbosity shouldBe 0
        }

        Args(parserOf("-v")).run {
            verbosity shouldBe 1
        }

        Args(parserOf("-v", "-v")).run {
            verbosity shouldBe 2
        }
    }

    test("Help") {
        class Args(parser: ArgParser) {
            val dryRun by parser.flagging("-n", "--dry-run",
                help = "don't do anything")
            val includes by parser.adding("-I", "--include",
                help = "search in this directory for header files")
            val outDir by parser.storing("-o", "--output",
                help = "directory in which all output should be generated")
            val verbosity by parser.counting("-v", "--verbose",
                help = "increase verbosity")
            val sources by parser.positionalList("SOURCE",
                help = "source file")
            val destination by parser.positional("DEST",
                help = "destination file")
        }

        shouldThrow<ShowHelpException> {
            Args(parserOf("--help",
                helpFormatter = DefaultHelpFormatter(
                    prologue = """
                            This is the prologue. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam
                            malesuada maximus eros. Fusce luctus risus eget quam consectetur, eu auctor est ullamcorper.
                            Maecenas eget suscipit dui, sed sodales erat. Phasellus.

                            This is the second paragraph of the prologue. I don't have anything else to say, but I'd
                            like there to be enough text that it wraps to the next line.
                            """,
                    epilogue = """
                            This is the epilogue. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec vel tortor nunc. Sed eu massa sed turpis auctor faucibus. Donec vel pellentesque tortor. Ut ultrices tempus lectus fermentum vestibulum. Phasellus.
                            """))).dryRun
        }.run {
            val help = StringWriter().apply { printUserMessage(this, "program_name", 60) }.toString()
            help shouldBe """
usage: program_name [-h] [-n] [-I INCLUDE]... -o OUTPUT
                    [-v]... SOURCE... DEST


This is the prologue. Lorem ipsum dolor sit amet,
consectetur adipiscing elit. Aliquam malesuada maximus eros.
Fusce luctus risus eget quam consectetur, eu auctor est
ullamcorper. Maecenas eget suscipit dui, sed sodales erat.
Phasellus.

This is the second paragraph of the prologue. I don't have
anything else to say, but I'd like there to be enough text
that it wraps to the next line.


required arguments:
  -o OUTPUT,          directory in which all output should
  --output OUTPUT     be generated


optional arguments:
  -h, --help          show this help message and exit

  -n, --dry-run       don't do anything

  -I INCLUDE,         search in this directory for header
  --include INCLUDE   files

  -v, --verbose       increase verbosity


positional arguments:
  SOURCE              source file

  DEST                destination file


This is the epilogue. Lorem ipsum dolor sit amet,
consectetur adipiscing elit. Donec vel tortor nunc. Sed eu
massa sed turpis auctor faucibus. Donec vel pellentesque
tortor. Ut ultrices tempus lectus fermentum vestibulum.
Phasellus.
""".trimStart()

            val help2 = StringWriter().apply { printUserMessage(this, "a_really_long_program_name", 60) }.toString()
            help2 shouldBe """
usage: a_really_long_program_name
         [-h] [-n] [-I INCLUDE]... -o OUTPUT [-v]...
         SOURCE... DEST


This is the prologue. Lorem ipsum dolor sit amet,
consectetur adipiscing elit. Aliquam malesuada maximus eros.
Fusce luctus risus eget quam consectetur, eu auctor est
ullamcorper. Maecenas eget suscipit dui, sed sodales erat.
Phasellus.

This is the second paragraph of the prologue. I don't have
anything else to say, but I'd like there to be enough text
that it wraps to the next line.


required arguments:
  -o OUTPUT,          directory in which all output should
  --output OUTPUT     be generated


optional arguments:
  -h, --help          show this help message and exit

  -n, --dry-run       don't do anything

  -I INCLUDE,         search in this directory for header
  --include INCLUDE   files

  -v, --verbose       increase verbosity


positional arguments:
  SOURCE              source file

  DEST                destination file


This is the epilogue. Lorem ipsum dolor sit amet,
consectetur adipiscing elit. Donec vel tortor nunc. Sed eu
massa sed turpis auctor faucibus. Donec vel pellentesque
tortor. Ut ultrices tempus lectus fermentum vestibulum.
Phasellus.
""".trimStart()

            // Regression test for issue #17
            val help_wide = StringWriter().apply { printUserMessage(this, "program_name", 0) }.toString()
            help_wide shouldBe """
usage: program_name [-h] [-n] [-I INCLUDE]... -o OUTPUT [-v]... SOURCE... DEST


This is the prologue. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam malesuada maximus eros. Fusce luctus risus eget quam consectetur, eu auctor est ullamcorper. Maecenas eget suscipit dui, sed sodales erat. Phasellus.

This is the second paragraph of the prologue. I don't have anything else to say, but I'd like there to be enough text that it wraps to the next line.


required arguments:
  -o OUTPUT, --output OUTPUT      directory in which all output should be generated


optional arguments:
  -h, --help                      show this help message and exit

  -n, --dry-run                   don't do anything

  -I INCLUDE, --include INCLUDE   search in this directory for header files

  -v, --verbose                   increase verbosity


positional arguments:
  SOURCE                          source file

  DEST                            destination file


This is the epilogue. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec vel tortor nunc. Sed eu massa sed turpis auctor faucibus. Donec vel pellentesque tortor. Ut ultrices tempus lectus fermentum vestibulum. Phasellus.
""".trimStart()
        }
    }

    test("Version") {
        class Args(parser: ArgParser) {
            val flag by parser.flagging(help = TEST_HELP)
        }

        shouldThrow<ShowVersionException> {
            Args(parserOf("--version",
                    version = "1.0.0")).flag
        }.run {
            val help = StringWriter().apply { printUserMessage(this, "program_name", 60) }.toString()
            help shouldBe "1.0.0"
        }
    }

    test("Implicit long flag name") {
        class Args(parser: ArgParser) {
            val flag1 by parser.flagging(help = TEST_HELP)
            val flag2 by parser.flagging(help = TEST_HELP)
            val count by parser.counting(help = TEST_HELP)
            val store by parser.storing(help = TEST_HELP)
            val store_int by parser.storing(help = TEST_HELP) { toInt() }
            val adder by parser.adding(help = TEST_HELP)
            val int_adder by parser.adding(help = TEST_HELP) { toInt() }
            val int_set_adder by parser.adding(initialValue = mutableSetOf<Int>(), help = TEST_HELP) { toInt() }
            val positional by parser.positional(help = TEST_HELP)
            val positional_int by parser.positional(help = TEST_HELP) { toInt() }
            val positionalList by parser.positionalList(sizeRange = 2..2, help = TEST_HELP)
            val positionalList_int by parser.positionalList(sizeRange = 2..2, help = TEST_HELP) { toInt() }
        }

        Args(parserOf(
            "--flag1", "--count", "--count", "--store=hello", "--store-int=42",
            "--adder=foo", "--adder=bar",
            "--int-adder=2", "--int-adder=4", "--int-adder=6",
            "--int-set-adder=64", "--int-set-adder=128", "--int-set-adder=20",
            "1", "1", "2", "3", "5", "8"
        )).run {
            flag1 shouldBe true
            flag2 shouldBe false
            count shouldBe 2
            store shouldBe "hello"
            store_int shouldBe 42
            adder shouldBe listOf("foo", "bar")
            int_adder shouldBe listOf(2, 4, 6)
            int_set_adder shouldBe setOf(20, 64, 128)
            positional shouldBe "1"
            positional_int shouldBe 1
            positionalList shouldBe listOf("2", "3")
            positionalList_int shouldBe listOf(5, 8)
        }

        shouldThrow<MissingRequiredPositionalArgumentException> {
            Args(parserOf(
                "13", "21", "34", "55", "89"
            )).run {
                flag1 shouldBe false
            }
        }.run {
            message shouldBe "missing POSITIONAL-LIST-INT operand"
        }
    }

    fun nullableString(): String? = null

    test("Nullable optional") {
        class Args(parser: ArgParser) {
            val path by parser.storing("The path", transform = ::File)
                .default(nullableString()?.let(::File))
        }
    }

    test("Nullable optional without transform") {
        class Args(parser: ArgParser) {
            val str by parser.storing(TEST_HELP)
                .default(nullableString())
        }
        Args(parserOf("--str=foo")).run {
            str shouldBe "foo"
        }
        Args(parserOf()).run {
            str shouldBe null
        }
    }

    test("Default generalization") {
        class Args(parser: ArgParser) {
            val shape by parser.storing("The path", transform = ::Rectangle)
                .default(Circle())
            val rect by parser.storing("The path", transform = ::Rectangle)
        }
        val args = Args(parserOf("--rect=foo"))
        staticType(args.shape) shouldBe Shape::class
        args.shape should beOfType<Circle>()
        staticType(args.rect) shouldBe Rectangle::class

        val args2 = Args(parserOf())
        shouldThrow<MissingValueException> {
            args2.rect
        }.run {
            message shouldBe "missing RECT"
        }
    }

    test("Default generalization without transform") {
        class Args(parser: ArgParser) {
            val str by parser.storing(TEST_HELP)
                .default(5)
        }
        Args(parserOf("--str=foo")).run {
            str shouldBe "foo"
        }
        Args(parserOf()).run {
            str shouldBe 5
        }
    }

    test("Auto named flagging") {
        class Args(parser: ArgParser) {
            val autoFlag by parser.flagging(TEST_HELP)
        }
        Args(parserOf()).autoFlag shouldBe false
        Args(parserOf("--auto-flag")).autoFlag shouldBe true
    }

    test("Auto named counting") {
        class Args(parser: ArgParser) {
            val autoCount by parser.counting(TEST_HELP)
        }
        Args(parserOf()).autoCount shouldBe 0
        Args(parserOf("--auto-count")).autoCount shouldBe 1
        Args(parserOf("--auto-count", "--auto-count")).autoCount shouldBe 2
    }

    test("Auto named storing") {
        class Args(parser: ArgParser) {
            val autoStore by parser.storing(TEST_HELP)
        }

        shouldThrow<MissingValueException> {
            Args(parserOf()).autoStore
        }.run {
            message shouldBe "missing AUTO_STORE"
        }

        Args(parserOf("--auto-store=foo")).autoStore shouldBe "foo"
        Args(parserOf("--auto-store", "bar", "--auto-store", "baz")).autoStore shouldBe "baz"
    }

    test("Auto named storing with transform") {
        class Args(parser: ArgParser) {
            val autoStore by parser.storing(TEST_HELP) { toInt() }
        }

        shouldThrow<MissingValueException> {
            Args(parserOf()).autoStore
        }.run {
            message shouldBe "missing AUTO_STORE"
        }

        Args(parserOf("--auto-store=5")).autoStore shouldBe 5
        Args(parserOf("--auto-store", "11", "--auto-store", "42")).autoStore shouldBe 42
    }

    test("Auto named adding") {
        class Args(parser: ArgParser) {
            val autoAccumulator by parser.adding(TEST_HELP)
        }

        Args(parserOf()).autoAccumulator shouldBe emptyList<String>()
        Args(parserOf("--auto-accumulator=foo")).autoAccumulator shouldBe listOf("foo")
        Args(parserOf("--auto-accumulator", "bar", "--auto-accumulator", "baz")).autoAccumulator shouldBe listOf("bar", "baz")
    }

    test("Auto named adding with transform") {
        class Args(parser: ArgParser) {
            val autoAccumulator by parser.adding(TEST_HELP) { toInt() }
        }

        Args(parserOf()).autoAccumulator shouldBe emptyList<Int>()
        Args(parserOf("--auto-accumulator=5")).autoAccumulator shouldBe listOf(5)
        Args(parserOf("--auto-accumulator", "11", "--auto-accumulator", "42")).autoAccumulator shouldBe listOf(11, 42)
    }

    test("Auto named adding with transform and initial") {
        class Args(parser: ArgParser) {
            val autoAccumulator by parser.adding(TEST_HELP, initialValue = mutableSetOf<Int>()) { toInt() }
        }

        Args(parserOf()).autoAccumulator shouldBe emptySet<Int>()
        Args(parserOf("--auto-accumulator=5")).autoAccumulator shouldBe setOf(5)
        Args(parserOf("--auto-accumulator", "11", "--auto-accumulator", "42")).autoAccumulator shouldBe setOf(42, 11)
    }

    test("Auto named positional") {
        class Args(parser: ArgParser) {
            val autoPositional by parser.positional(TEST_HELP)
        }

        shouldThrow<MissingRequiredPositionalArgumentException> {
            Args(parserOf()).autoPositional
        }.run {
            message shouldBe "missing AUTO-POSITIONAL operand"
        }
        Args(parserOf("foo")).autoPositional shouldBe "foo"
    }

    test("Auto named positional with transform") {
        class Args(parser: ArgParser) {
            val autoPositional by parser.positional(TEST_HELP) { toInt() }
        }

        shouldThrow<MissingRequiredPositionalArgumentException> {
            Args(parserOf()).autoPositional
        }.run {
            message shouldBe "missing AUTO-POSITIONAL operand"
        }
        Args(parserOf("47")).autoPositional shouldBe 47
    }

    test("Auto named positional list") {
        class Args(parser: ArgParser) {
            val autoPositional by parser.positionalList(TEST_HELP)
        }

        shouldThrow<MissingRequiredPositionalArgumentException> {
            Args(parserOf()).autoPositional
        }.run {
            message shouldBe "missing AUTO-POSITIONAL operand"
        }
        Args(parserOf("foo")).autoPositional shouldBe listOf("foo")
    }

    test("Auto named positional list with transform") {
        class Args(parser: ArgParser) {
            val autoPositional by parser.positionalList(TEST_HELP) { toInt() }
        }

        shouldThrow<MissingRequiredPositionalArgumentException> {
            Args(parserOf()).autoPositional
        }.run {
            message shouldBe "missing AUTO-POSITIONAL operand"
        }
        Args(parserOf("47")).autoPositional shouldBe listOf(47)
        Args(parserOf("27", "38")).autoPositional shouldBe listOf(27, 38)
    }

    test("Positional default") {
        class Args(parser: ArgParser) {
            val name by parser.positional("NAME", TEST_HELP).default("John")
        }

        Args(parserOf()).name shouldBe "John"
        Args(parserOf("Alfred")).name shouldBe "Alfred"
    }

    test("Positional list default") {
        class Args(parser: ArgParser) {
            val name by parser.positionalList("NAME", TEST_HELP).default(listOf("Jack", "Jill"))
        }

        Args(parserOf()).name shouldBe listOf("Jack", "Jill")
        Args(parserOf("Jack")).name shouldBe listOf("Jack")
        Args(parserOf("John", "Jim", "Jack", "Jason")).name shouldBe listOf("John", "Jim", "Jack", "Jason")
    }

    test("Auto named long option with multiple args") {
        class Args(parser: ArgParser) {
            val xyz by parser.option<MutableList<String>>(
                "--xyz",
                argNames = listOf("COLOR", "SIZE", "FLAVOR"),
                help = TEST_HELP) {
                value.orElse { mutableListOf<String>() }.apply { add("$optionName:$arguments")
                }
            }
        }

        // Test with value as separate arg
        Args(parserOf("--xyz", "red", "5", "salty")).xyz shouldBe listOf("--xyz:[red, 5, salty]")

        Args(parserOf("--xyz", "green", "42", "sweet", "--xyz", "blue", "7", "bitter")).xyz shouldBe listOf(
            "--xyz:[green, 42, sweet]", "--xyz:[blue, 7, bitter]")

        // Note that something that looks like an option is consumed as an argument if it appears where an argument
        // should be. This is expected behavior.
        Args(parserOf("--xyz", "green", "42", "--xyz")).xyz shouldBe listOf(
            "--xyz:[green, 42, --xyz]")

        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("--xyz", "green", "42", "sweet", "--xyz", "blue", "7")).xyz
        }.run {
            message shouldBe "option '--xyz' is missing the required argument FLAVOR"
        }

        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("--xyz", "green")).xyz
        }.run {
            message shouldBe "option '--xyz' is missing the required argument SIZE"
        }

        shouldThrow<OptionMissingRequiredArgumentException> {
            Args(parserOf("--xyz")).xyz
        }.run {
            message shouldBe "option '--xyz' is missing the required argument COLOR"
        }
    }

    test("Positional add validator") {
        class Args(parser: ArgParser) {
            val yDelegate = parser.positional("Y", TEST_HELP) { toInt() }
            val y by yDelegate

            val xDelegate = parser.positional("X", TEST_HELP) { toInt() }
                .addValidator {
                    if (value.rem(2) != 0)
                        throw InvalidArgumentException("$errorName must be even, $value is odd")
                }
            val x by xDelegate

            init {
                if (y >= x)
                    throw InvalidArgumentException("${yDelegate.errorName} must be less than ${xDelegate.errorName}")
            }
        }

        // This should pass validation
        val opts0 = Args(parserOf("1", "10"))
        opts0.y shouldBe 1
        opts0.x shouldBe 10

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("20", "10")).x
        }.run {
            message shouldBe "Y must be less than X"
        }

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("10", "15")).x
        }.run {
            message shouldBe "X must be even, 15 is odd"
        }
    }

    test("Positional list add validator") {
        class Args(parser: ArgParser) {
            val yDelegate = parser.positionalList("Y", TEST_HELP, 2..2) { toInt() }
            val y by yDelegate

            val xDelegate = parser.positionalList("X", TEST_HELP, 2..2) { toInt() }
                .addValidator {
                    for (i in value) {
                        if (i.rem(2) != 0)
                            throw InvalidArgumentException("$errorName elements must be even, $i is odd")
                    }
                }
            val x by xDelegate
        }

        // This should pass validation
        val opts0 = Args(parserOf("1", "10", "4", "8"))
        opts0.y shouldBe listOf(1, 10)
        opts0.x shouldBe listOf(4, 8)

        shouldThrow<InvalidArgumentException> {
            Args(parserOf("10", "15", "42", "37")).x
        }.run {
            message shouldBe "X elements must be even, 37 is odd"
        }
    }

    test("Parse into") {
        class Args(parser: ArgParser) {
            val str by parser.storing(TEST_HELP)
        }

        parserOf("--str=foo").parseInto(::Args).run {
            str shouldBe "foo"
        }
    }

    test("Parse into unrecognized option failure") {
        class Args(parser: ArgParser) {
            val str by parser.storing(TEST_HELP)
        }

        shouldThrow<UnrecognizedOptionException> {
            parserOf("--str=foo", "--eggs=bacon").parseInto(::Args)
        }
    }

    test("Parse into missing value failure") {
        class Args(parser: ArgParser) {
            val str by parser.storing(TEST_HELP)
            val eggs by parser.storing(TEST_HELP)
        }

        shouldThrow<MissingValueException> {
            parserOf("--str=foo").parseInto(::Args)
        }
    }

    test("Parse into illegal state test") {
        class Args(parser: ArgParser) {
            val str by parser.storing(TEST_HELP)
        }

        shouldThrow<IllegalStateException> {
            val parser = parserOf("--str=foo")
            @Suppress("unused_variable")
            val oops by parser.storing("--oops", help = TEST_HELP).default("oops")
            parser.parseInto(::Args)
        }
    }

    test("Issue15") {
        class Args(parser: ArgParser) {
            val manual by parser.storing("--named-by-hand", help = TEST_HELP, argName = "HANDYS-ARG")
            val auto by parser.storing(TEST_HELP, argName = "OTTOS-ARG")
            val foo by parser.adding(help = TEST_HELP, argName = "BAR") { toInt() }
            val bar by parser.adding("--baz", help = TEST_HELP, argName = "QUUX")
        }

        shouldThrow<ShowHelpException> {
            Args(parserOf("--help")).manual
        }.run {
            // TODO: find a way to make this less brittle (ie: don't use help text)
            StringWriter().apply { printUserMessage(this, null, 10000) }.toString().trim() shouldBe """
usage: [-h] --named-by-hand HANDYS-ARG --auto OTTOS-ARG [--foo BAR]... [--baz QUUX]...

required arguments:
  --named-by-hand HANDYS-ARG   test help message

  --auto OTTOS-ARG             test help message


optional arguments:
  -h, --help                   show this help message and exit

  --foo BAR                    test help message

  --baz QUUX                   test help message""".trim()
        }
    }

    test("Issue18 addValidator then default") {
        class Args(parser: ArgParser) {
            val x by parser.storing(
                "-x",
                help = TEST_HELP,
                transform = String::toInt
            ).addValidator {
                value shouldBe 0
            }.default(0)
        }
        shouldThrow<IllegalStateException> {
            Args(parserOf())
        }.message shouldBe "Cannot add default after adding validators"
    }

    test("Issue18 default then addValidator") {
        class Args(parser: ArgParser) {
            val x by parser.storing(
                "-x",
                help = "",
                transform = String::toInt
            ).default(0).addValidator {
                value shouldBe 0
            }
        }
        val x = Args(parserOf()).x
        x shouldBe 0
    }

    test("Issue47") {
        class Args(parser: ArgParser) {
            val caseInsensitive by parser.flagging("-c", "--case_insensitive", help = TEST_HELP)

            val includedExtensions by parser.adding("-e", "--include_ext", help = TEST_HELP) {
                extensionCheckCaseInsensitive()
            }

            private fun String.extensionCheckCaseInsensitive() =
                if (caseInsensitive) this.toLowerCase() else this
        }

        val includeExtensions = Args(parserOf("-e", "Foo", "-c", "-e", "Bar")).includedExtensions
        includeExtensions shouldBe listOf("Foo", "bar")
    }

    class DependentArgs(parser: ArgParser) {
        val suffix by parser.storing(TEST_HELP)

        val x by parser.adding(TEST_HELP) {
            "$this:$suffix"
        }
    }

    test("Dependent args test order mat") {
        val result = DependentArgs(parserOf("--suffix", "bar", "-x", "foo", "-x", "dry", "--suffix", "fish", "-x", "cat")).x
        result shouldBe listOf("foo:bar", "dry:bar", "cat:fish")
    }

    test("Dependent args test unset throws missing value excep") {
        shouldThrow<MissingValueException> {
            DependentArgs(parserOf("-x", "foo", "-x", "dry", "--suffix", "fish", "-x", "cat")).x
        }.run {
            message shouldBe "missing SUFFIX"
        }
    }

    class DependentArgsWithDefault(parser: ArgParser) {
        val suffix by parser.storing(TEST_HELP).default("")

        val x by parser.adding(TEST_HELP) {
            "$this:$suffix"
        }
    }

    test("Dependent args test with default unset") {
        DependentArgsWithDefault(parserOf("-x", "foo", "-x", "dry", "--suffix", "fish", "-x", "cat")).x shouldBe
            listOf("foo:", "dry:", "cat:fish")
    }

    class DependentArgsWithDependentDefault(parser: ArgParser) {
        val a by parser.storing(TEST_HELP).default("")

        val b by parser.storing(TEST_HELP).default { "=$a" }
    }

    test("Dependent args test with dependent def") {
        DependentArgsWithDependentDefault(parserOf()).run {
            a shouldBe ""
            b shouldBe "="
        }

        DependentArgsWithDependentDefault(parserOf("-aFoo")).run {
            a shouldBe "Foo"
            b shouldBe "=Foo"
        }

        DependentArgsWithDependentDefault(parserOf("-bBar")).run {
            a shouldBe ""
            b shouldBe "Bar"
        }

        DependentArgsWithDependentDefault(parserOf("-aFoo", "-bBar")).run {
            a shouldBe "Foo"
            b shouldBe "Bar"
        }

        DependentArgsWithDependentDefault(parserOf("-bBar", "-aFoo")).run {
            a shouldBe "Foo"
            b shouldBe "Bar"
        }
    }
})

/** Used in tests where we need a simple enum */
enum class Color { RED, GREEN, BLUE }

/** Used in tests where we need a simple class hierarchy */
open class Shape
class Rectangle(val s: String) : Shape()
class Circle : Shape()

/** Creates an ArgParser for the specified (var)args. */
fun parserOf(
    vararg args: String,
    mode: ArgParser.Mode = ArgParser.Mode.GNU,
    helpFormatter: HelpFormatter? = DefaultHelpFormatter(),
    version: String? = null
) = ArgParser(args, mode, helpFormatter, version)

/**
 * Helper function for getting the static (not runtime) type of an expression. This is useful for verifying that the
 * inferred type of an expression is what you think it should be. For example:
 *
 *     staticType(actuallyACircle) shouldBe Shape::class
 */
inline fun <reified T : Any> staticType(@Suppress("UNUSED_PARAMETER") x: T) = T::class

val oneArgName = listOf("ARG_NAME")

val TEST_HELP = "test help message"
