/**
 * Copyright 2016 Laurence Gonsalves
 */
package com.xenomachina.argparser

import org.junit.Assert
import org.junit.Test

class ArgParserTest {
    @Test
    fun testValuelessShortFlags() {
        class MyArguments(args: Array<String>) : ArgParser(args) {
            val xyz by action<MutableList<String>>("-x", "-y", "-z",
                    help="Really hoopy frood"
            ){
                oldValue.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
        }

        Assert.assertEquals(
                listOf("x", "y", "z", "z", "y"),
                MyArguments(arrayOf("-x", "-y", "-z", "-z", "-y")).xyz)

        Assert.assertEquals(
                listOf("x", "y", "z"),
                MyArguments(arrayOf("-xyz")).xyz)
    }

    @Test
    fun testShortFlagsWithValues() {
        class MyArguments(args: Array<String>) : ArgParser(args) {
            val xyz by actionWithValue<MutableList<String>>("-x", "-y", "-z",
                    help="Really hoopy frood"
            ){
                oldValue.orElse{mutableListOf<String>()}.apply {
                    add("$name:$argument")
                }
            }
        }

        // Test with value as separate arg
        Assert.assertEquals(
                listOf("x:0", "y:1", "z:2", "z:3", "y:4"),
                MyArguments(arrayOf("-x", "0", "-y", "1", "-z", "2", "-z", "3", "-y", "4")).xyz)

        // Test with value concatenated
        Assert.assertEquals(
                listOf("x:0", "y:1", "z:2", "z:3", "y:4"),
                MyArguments(arrayOf("-x0", "-y1", "-z2", "-z3", "-y4")).xyz)

        // Test with = between flag and value
        Assert.assertEquals(
                listOf("x:=0", "y:=1", "z:=2", "z:=3", "y:=4"),
                MyArguments(arrayOf("-x=0", "-y=1", "-z=2", "-z=3", "-y=4")).xyz)

        // TODO test with chaining
    }

    @Test
    fun testMixedShortFlags() {
        class MyArguments(args: Array<String>) : ArgParser(args) {
            val myFoo by action<MutableList<String>>("-d", "-e", "-f",
                    help="Foo"
            ){
                oldValue.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
            val myBar by action<MutableList<String>>("-a", "-b", "-c",
                    help="Bar"
            ){
                oldValue.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
        }

        val myArgs = MyArguments(arrayOf("-adbefccbafed"))

        Assert.assertEquals(
                listOf("d", "e", "f", "f", "e", "d"),
                myArgs.myFoo)
        Assert.assertEquals(
                listOf("a", "b", "c", "c", "b", "a"),
                myArgs.myBar)
    }

    @Test
    fun testMixedShortFlagsWithValues() {
        class MyArguments(args: Array<String>) : ArgParser(args) {
            val myFoo by action<MutableList<String>>("-d", "-e", "-f",
                    help="Foo"
            ){
                oldValue.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
            val myBar by action<MutableList<String>>("-a", "-b", "-c",
                    help="Bar"
            ){
                oldValue.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
            val myBaz by actionWithValue<MutableList<String>>("-x", "-y", "-z",
                    help="Baz"
            ){
                oldValue.orElse{mutableListOf<String>()}.apply {
                    add("$name:$argument")
                }
            }
        }

        val myArgs = MyArguments(arrayOf("-adecfy5", "-x0", "-bzxy"))

        Assert.assertEquals(
                listOf("a", "c", "b"),
                myArgs.myBar)
        Assert.assertEquals(
                listOf("d", "e", "f"),
                myArgs.myFoo)
        Assert.assertEquals(
                listOf("y:5", "x:0", "z:xy"),
                myArgs.myBaz)
    }

    @Test
    fun testValuelessLongFlags() {
        class MyArguments(args: Array<String>) : ArgParser(args) {
            val xyz by action<MutableList<String>>("--xray", "--yellow", "--zebra",
                    help="Really hoopy frood"
            ){
                oldValue.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
        }

        Assert.assertEquals(
                listOf("xray", "yellow", "zebra", "zebra", "yellow"),
                MyArguments(arrayOf("--xray", "--yellow", "--zebra", "--zebra", "--yellow")).xyz)

        Assert.assertEquals(
                listOf("xray", "yellow", "zebra"),
                MyArguments(arrayOf("--xray", "--yellow", "--zebra")).xyz)
    }

    @Test
    fun testLongFlagsWithValues() {
        class MyArguments(args: Array<String>) : ArgParser(args) {
            val xyz by actionWithValue<MutableList<String>>("--xray", "--yellow", "--zaphod",
                    help="Xyz"
            ){
                oldValue.orElse{mutableListOf<String>()}.apply {
                    add("$name:$argument")
                }
            }
        }

        // Test with value as separate arg
        Assert.assertEquals(
                listOf("xray:0", "yellow:1", "zaphod:2", "zaphod:3", "yellow:4"),
                MyArguments(arrayOf("--xray", "0", "--yellow", "1", "--zaphod", "2", "--zaphod", "3", "--yellow", "4")).xyz)

        // Test with value concatenated TODO should fail
//        Assert.assertEquals(
//                listOf("xray:0", "yellow:1", "zaphod:2", "zaphod:3", "yellow:4"),
//                MyArguments(arrayOf("--xray0", "--yellow1", "--zaphod2", "--zaphod3", "--yellow4")).xyz)

        // Test with = between flag and value
        Assert.assertEquals(
                listOf("xray:0", "yellow:1", "zaphod:2", "zaphod:3", "yellow:4"),
                MyArguments(arrayOf("--xray=0", "--yellow=1", "--zaphod=2", "--zaphod=3", "--yellow=4")).xyz)
    }

    @Test
    fun testSettingValues() {
        class MyArguments(args: Array<String>) : ArgParser(args) {
            val xyz by actionWithValue<Int>("-x",
                    help="an integer"
            ){
                argument.toInt()
            }.default(5)
        }

        // Test with no value
        Assert.assertEquals(
                5,
                MyArguments(arrayOf()).xyz)

        // Test with value
        Assert.assertEquals(
                6,
                MyArguments(arrayOf("-x6")).xyz)

        // Test with value as separate arg
        Assert.assertEquals(
                7,
                MyArguments(arrayOf("-x", "7")).xyz)

        // Test with multiple values
        Assert.assertEquals(
                8,
                MyArguments(arrayOf("-x9", "-x8")).xyz)
    }

    // TODO test InvalidOption
    // TODO test short arg needs value at end
    // TODO test long arg needs value at end
}
