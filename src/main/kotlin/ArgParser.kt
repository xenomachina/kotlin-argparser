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
 *             return newParsed(name, oldValue, argument)
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
                             help: String? = null,
                             handler: Action.WithoutValue.Input<T>.() -> T): Action<T> {
        val action = Action.WithoutValue<T>(this, help = help, handler = handler)
        for (name in names) {
            register(name, action)
        }
        return action
    }

    protected fun <T> actionWithValue(vararg names: String,
                                      help: String? = null,
                                      handler: Action.WithValue.Input<T>.() -> T): Action<T> {
        val action = Action.WithValue<T>(this, help = help, handler = handler)
        for (name in names) {
            register(name, action)
        }
        return action
    }

    open class Exception(message: String, val returnCode: Int) : java.lang.Exception(message)

    class InvalidOption(val argName: String) :
            ArgParser.Exception("invalid option -- '$argName'", 2)

    sealed class Action<T>(private val argParser: ArgParser) {
        protected var holder: Holder<T>? = null

        class WithValue<T>(argParser: ArgParser, val help: String?, val handler: Input<T>.() -> T) :
                Action<T>(argParser) {

            data class Input<T>(
                    val oldValue: Holder<T>?,
                    val name: String,
                    val argument: String)

            fun parseNameValue(name: String, value: String) {
                holder = Holder(handler(Input(holder, name, value)))
            }
        }

        class WithoutValue<T>(argParser: ArgParser, val help: String?, val handler: Input<T>.() -> T) :
                Action<T>(argParser) {
            data class Input<T>(
                    val oldValue: Holder<T>?,
                    val name: String)

            fun parseName(name: String) {
                holder = Holder(handler(Input(holder, name)))
            }
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            argParser.parseArgs
            return holder!!.value
        }

        fun  default(value: T): Action<T> {
            holder = Holder(value)
            return this
        }
    }

    private val shortFlags = mutableMapOf<Char, Action<*>>()
    private val longFlags = mutableMapOf<String, Action<*>>()

    private fun <T> register(name: String, action: ArgParser.Action<T>) {
        if (name.startsWith("--")) {
            if (name.length <= 2)
                throw IllegalArgumentException("illegal long flag '$name' -- must have at least one character after hyphen")
            longFlags.put(name.substring(2), action)
        } else if (name.startsWith("-")) {
            if (name.length != 2)
                throw IllegalArgumentException("illegal short flag '$name' -- can only have one character after hyphen")
            val key = name.get(1)
            if (key in shortFlags)
                throw IllegalStateException("short flag '$name' already in use")
            shortFlags.put(key, action)
        } else {
            TODO("registration of positional args not implemented")
        }

    }

    private val parseArgs by lazy {
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
        // TODO: throw exception if any holders are null
    }

    private fun parsePositionalArg(arg: String) {
        TODO("not implemented -- $arg")
    }

    private fun parseLongArg(arg: String, arg2: String?): Boolean {
        val argName: String
        val argValue: String?
        val sawEqual: Boolean
        val m = NAME_EQUALS_VALUE_REGEX.matchEntire(arg)
        if (m == null) {
            argName = arg
            argValue = arg2
            sawEqual = false
        } else {
            argName = m.groups[1]!!.value
            argValue = m.groups[2]!!.value
            sawEqual = true
        }
        val action = longFlags.get(argName)
        if (action == null) {
            throw InvalidOption(argName)
        } else {
            when(action) {
                is Action.WithValue -> {
                    if (argValue == null)
                        TODO("throw exception: option '--$argName' requires an argument")
                    action.parseNameValue(argName, argValue)
                    return !sawEqual
                }
                is Action.WithoutValue -> {
                    if (sawEqual)
                        TODO("throw exception: option '--$argName' doesn't allow an argument")
                    action.parseName(argName)
                    return false
                }
            }
        }
    }

    private fun parseShortArgs(arg: String, start: Int, arg2: String?): Boolean {
        var pos = start
        while (pos < arg.length) {
            val argName = arg[pos]
            val action = shortFlags.get(argName)
            if (action == null) {
                throw InvalidOption(argName.toString())
            } else {
                when(action) {
                    is Action.WithValue -> {
                        if (pos == arg.length - 1) {
                            if (arg2 == null)
                                TODO("throw exception: option '--$argName' requires an argument")
                            action.parseNameValue(argName.toString(), arg2)
                            return true
                        } else {
                            action.parseNameValue(argName.toString(), arg.substring(pos + 1))
                            return false
                        }
                    }
                    is Action.WithoutValue -> {
                        action.parseName(argName.toString())
                    }
                }
            }

            pos++
        }
        return false
    }

    private fun parseShortArgs(arg: String, arg2: String?) = parseShortArgs(arg, 0, arg2)
}

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

private val NAME_EQUALS_VALUE_REGEX = Regex("^([^=]+)=(.*)$")
