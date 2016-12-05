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
            val myFlags by action<MutableList<String>>("-x", "-y", "-z",
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
                MyArguments(arrayOf("-x", "-y", "-z", "-z", "-y")).myFlags)

        Assert.assertEquals(
                listOf("x", "y", "z"),
                MyArguments(arrayOf("-xyz")).myFlags)
    }

    @Test
    fun testShortFlagsWithValues() {
        class MyArguments(args: Array<String>) : ArgParser(args) {
            val myArgs by action<MutableList<String>>("-x", "-y", "-z",
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
                MyArguments(arrayOf("-x", "0", "-y", "1", "-z", "2", "-z", "3", "-y", "4")).myArgs)

        // Test with value concatenated
        Assert.assertEquals(
                listOf("x:0", "y:1", "z:2", "z:3", "y:4"),
                MyArguments(arrayOf("-x0", "-y1", "-z2", "-z3", "-y4")).myArgs)

        // Test with = between flag and value
        Assert.assertEquals(
                listOf("x:=0", "y:=1", "z:=2", "z:=3", "y:=4"),
                MyArguments(arrayOf("-x=0", "-y=1", "-z=2", "-z=3", "-y=4")).myArgs)

        // TODO test with chaining
    }
}
