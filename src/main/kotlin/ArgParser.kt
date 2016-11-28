/**
 * Copyright 2016 Laurence Gonsalves
 */

package com.xenomachina.argparser

import kotlin.reflect.KProperty

/**
 * A parser of command-line arguments.
 *
 * Example usage:
 *
 *     // Define class to hold parsed argument
 *     class MyArguments(args: Array<String> : ArgParser(args) {
 *         // boolean flags
 *         val verbose by flag("-v", "--verbose")
 *
 *         // simple values
 *         val name by value("-N", "--name",
 *             help="My Name")
 *         val size by value("-s", "--size"
 *             help="My Size"){toInt} = 8
 *
 *         // required values
 *         val name by required("-O", "--output",
 *             help="Output location")
 *
 *         // accumulating values (turns into a List)
 *         val includeDirs by accumulate("-I",
 *             help="Directories to search for headers"
 *         ){File(this)}
 *
 *         // map flags to values
 *         val mode by map("--fast" to Mode.FAST,
 *                         "--small" to Mode.SMALL,
 *                         "--quiet" to Mode.QUIET,
 *             help="Operating mode")
 *
 *         // All of these methods are based upon the "action" method, which
 *         // can do anything they can do and more (but is harder to use in the
 *         // common cases)
 *         val zaphod by action("-z", "--zaphod"
 *             help="Directories to search for headers"
 *         ){
 *             return newParsed(name, oldParsed, newUnparsed)
 *         }
 *     }
 *
 *     fun main(args : Array<String>) {
 *         try {
 *             val myArgs = MyArguments(args)
 *             println("Hello, {args.name}!")
 *         } catch (e: ArgParser.Exception) {
 *             e.printAndExit()
 *         }
 *     }
 */
class ArgParser(val args: Array<String>) {
    private val handlers = mutableMapOf<String, Value<*>>()

    private val x: Int = "5".toInt()
    private val f: (String) -> Int = String::toInt

    data class Arg<T>(
            val name: String,
            val oldValue: T,
            val value: String)

    fun <T> handle(vararg names: String, handler: (Arg<T>, String) -> T) : Value<T> {
        val result = Value(handler)
        for (name in names) {
            handlers.put(name, result)
        }
        return result
    }

    private fun parseArgs() {
        TODO()
    }

    inner class Value<T>(handler: (Arg<T>, String) -> T) {

        private val holder: Holder<T>? = null

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            parseArgs()
            return holder!!.value
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T): Unit {
            TODO()
        }
    }
}

data class Holder<T> (val value: T)
