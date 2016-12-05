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
open class ArgParser(val args: Array<String>) {
    protected fun <T> action(vararg names: String,
                             needsValue: Boolean, // TODO: move into type system?
                             help: String? = null,
                             handler: Arg<T>.() -> T): Action<T> {
        val action = Action<T>(help, needsValue, handler)
        for (name in names) {
            register(name, action)
        }
        return action
    }

    private fun <T> register(name: String, action: ArgParser.Action<T>) {
        if (name.startsWith("--")) {
            TODO()
        } else if (name.startsWith("-")) {
            if (name.length != 2)
                throw IllegalArgumentException("illegal short flag '$name' -- can only have one character after hyphen")
            val key = name.get(1)
            if (key in shortFlags)
                throw IllegalStateException("short flag '$name' already in use")
            shortFlags.put(key, action)
        } else {
            TODO()
        }

    }

    open class Exception(message: String, val returnCode: Int) : java.lang.Exception(message)

    data class Arg<T>(
            val name: String,
            val oldParsed: Holder<T>?,
            val newUnparsed: String?)

    private fun parseArgs() {
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            val arg2 = if (i + 1 < args.size) args[i + 1] else null
            if (arg.startsWith("--")) {
                if (parseLongArg(arg.substring(2), arg2)) i++
            } else if (arg.startsWith("-")) {
                if(parseShortArgs(arg.substring(1), arg2)) i++
            } else {
                parsePositionalArg(arg)
            }

            i++
        }
    }

    private fun parsePositionalArg(arg: String) {
        TODO("not implemented -- $arg")
    }

    private fun parseLongArg(arg: String, arg2: String?): Boolean {
        TODO("not implemented")
    }

    private val shortFlags = mutableMapOf<Char, Action<*>>()

    private fun parseShortArgs(arg: String, start: Int, arg2: String?): Boolean {
        var pos = start
        while (pos < arg.length) {
            val argName = arg[pos]
            val action = shortFlags.get(argName)
            if (action == null) {
                throw InvalidOption(argName.toString())
            } else {
                if (action.needsValue) {
                    if (pos == arg.length - 1) {
                        action.go(argName.toString(), arg2)
                        return true
                    } else {
                        TODO()
                        return false
                    }
                } else {
                    action.go(argName.toString(), null)
                }
            }

            pos++
        }
        return false
    }

    private fun parseShortArgs(arg: String, arg2: String?) = parseShortArgs(arg, 0, arg2)

    inner class Action<T> internal constructor (val help: String?,
                          val needsValue: Boolean,
                          val handler: Arg<T>.() -> T) {

        private var holder: Holder<T>? = null

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            parseArgs()
            return holder!!.value
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T): Unit {
            TODO()
        }

        fun go(name: String, value: String?) {
            holder = Holder(handler(Arg(name, holder, value)))
        }
    }
}

data class InvalidOption(val argName: String) :
        ArgParser.Exception("invalid option -- '$argName'", 2)

/**
 * Compensates for the fact that nullable types don't compose in Kotlin. If you want to be able to distinguish between a
 * T (where T may or may not be a nullable type) and lack of a T, use a Holder<T>?.
 */
data class Holder<T> (val value: T)

fun <T> Holder<T>?.orElse(f: () -> T) : T{
    if (this == null) {
        return f()
    } else {
        return value
    }
}