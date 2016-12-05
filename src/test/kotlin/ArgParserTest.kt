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
                    needsValue = false,
                    help="Really hoopy frood"
            ){
                oldParsed.orElse{mutableListOf<String>()}.apply {
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
            val xyz by action<MutableList<String>>("-x", "-y", "-z",
                    needsValue = true,
                    help="Really hoopy frood"
            ){
                oldParsed.orElse{mutableListOf<String>()}.apply {
                    add("$name:$newUnparsed")
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
                    needsValue = false,
                    help="Foo"
            ){
                oldParsed.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
            val myBar by action<MutableList<String>>("-a", "-b", "-c",
                    needsValue = false,
                    help="Bar"
            ){
                oldParsed.orElse{mutableListOf<String>()}.apply {
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
                    needsValue = false,
                    help="Foo"
            ){
                oldParsed.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
            val myBar by action<MutableList<String>>("-a", "-b", "-c",
                    needsValue = false,
                    help="Bar"
            ){
                oldParsed.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
            val myBaz by action<MutableList<String>>("-x", "-y", "-z",
                    needsValue = true,
                    help="Baz"
            ){
                oldParsed.orElse{mutableListOf<String>()}.apply {
                    add("$name:$newUnparsed")
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
}
