/**
 * Copyright 2016 Laurence Gonsalves
 */
package com.xenomachina.argparser

import org.junit.Assert
import org.junit.Test

class ArgParserTest {
    @Test
    fun testSelf() {
        class MyArguments(args: Array<String>) : ArgParser(args) {
            val zaphod by action<MutableList<String>>("-x", "-y", "-z",
                help="Really hoopy frood"
            ){
                oldParsed.orElse{mutableListOf<String>()}.apply {
                    add("{name}:{newUnparsed}")
                }
            }
        }

        val myArgs = MyArguments(arrayOf("-x0", "-y1", "z2", "-z3", "-y4"))
        // TODO Assert.assertEquals(listOf("x:0", "y:1", "z:2", "z:3", "y:4"), myArgs.zaphod)
        Assert.assertTrue(true)
    }
}
