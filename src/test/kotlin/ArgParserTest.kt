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
            val zaphod by action<MutableList<String>>("-x", "-y", "-z",
                    needsValue = false,
                    help="Really hoopy frood"
            ){
                oldParsed.orElse{mutableListOf<String>()}.apply {
                    add("$name")
                }
            }
        }

        val myArgs = MyArguments(arrayOf("-x", "-y", "-z", "-z", "-y"))
        Assert.assertEquals(listOf("x", "y", "z", "z", "y"), myArgs.zaphod)
    }

//    @Test
//    fun testSelf() {
//        class MyArguments(args: Array<String>) : ArgParser(args) {
//            val zaphod by action<MutableList<String>>("-x", "-y", "-z",
//                    needsValue = true,
//                    help="Really hoopy frood"
//            ){
//                oldParsed.orElse{mutableListOf<String>()}.apply {
//                    add("$name:$newUnparsed")
//                }
//            }
//        }
//
//        val myArgs = MyArguments(arrayOf("-x0", "-y1", "-z2", "-z3", "-y4"))
//        Assert.assertEquals(listOf("x:0", "y:1", "z:2", "z:3", "y:4"), myArgs.zaphod)
//    }
}
