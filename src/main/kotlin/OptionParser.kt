/**
 * Copyright 2016 Laurence Gonsalves
 */

package com.xenomachina.optionparser

import kotlin.reflect.KProperty
import kotlin.system.exitProcess

/**
 * A command-line option/argument parser.
 *
 * Example usage:
 *
 *     // Define class to hold parsed options
 *     class MyOptions(parser: OptionParser) {
 *         // boolean flags
 *         val verbose by parser.flag("-v", "--verbose")
 *
 *         // simple options with arguments
 *         val name by parser.argument("-N", "--name",
 *             help="My Name")
 *         val size by parser.argument("-s", "--size"
 *             help="My Size"){toInt} = 8
 *
 *         // optional options
 *         val name by parser.argument("-O", "--output",
 *             help="Output location")
 *             .default("./")
 *
 *         // accumulating values (turns into a List)
 *         val includeDirs by parser.accumulator("-I",
 *             help="Directories to search for headers"
 *         ){
 *             File(this)
 *         }
 *
 *         // TODO: implement mapping()
 *         // map options to values
 *         val mode by parser.mapping(
 *                 "--fast" to Mode.FAST,
 *                 "--small" to Mode.SMALL,
 *                 "--quiet" to Mode.QUIET,
 *             default = Mode.FAST,
 *             help="Operating mode")
 *
 *         // All of these methods are based upon the "action" method, which
 *         // can do anything they can do and more (but is harder to use in the
 *         // common cases)
 *         val zaphod by parser.action("-z", "--zaphod"
 *             help="Directories to search for headers"
 *         ){
 *             return parseZaphod(name, value, argument)
 *         }
 *     }
 *
 *  Your main function can then look like this:
 *
 *     fun main(args : Array<String>) =
 *         MyOptions(args).runMain {
 *             // `this` is the MyOptions instance, and will already be parsed
 *             // and validated at this point.
 *             println("Hello, {name}!")
 *         }
 */
open class OptionParser(val args: Array<String>) {
    // TODO: add --help support
    // TODO: add addValidator method
    // TODO: rename these to flagging, storing, adding?
    fun flag(vararg names: String,
             help: String? = null): Action<Boolean> =
            action<Boolean>(*names, help=help) {true}.default(false)

    fun <T> argument(vararg names: String,
                     help: String? = null,
                     parser: String.()->T): Action<T> =
            actionWithArgument(*names, help=help) {parser(this.argument)}

    fun argument(vararg names: String,
                 help: String? = null): Action<String> =
            argument(*names, help=help){this}

    fun <T> accumulator(vararg names: String,
                        help: String? = null,
                        parser: String.()->T): Action<MutableList<T>> =
            actionWithArgument<MutableList<T>>(*names, help=help) {
                value!!.value.add(parser(argument))
                value.value
            }.default(mutableListOf<T>())

    fun accumulator(vararg names: String,
                     help: String? = null): Action<MutableList<String>> =
        accumulator(*names, help = help){this}

    fun <T> action(vararg names: String,
                   help: String? = null,
                   handler: Action.WithoutArgument.Input<T>.() -> T): Action<T> {
        val action = Action.WithoutArgument<T>(this, help = help, handler = handler)
        for (name in names) {
            register(name, action)
        }
        return action
    }

    fun <T> actionWithArgument(vararg names: String,
                                         help: String? = null,
                                         handler: Action.WithArgument.Input<T>.() -> T): Action<T> {
        val action = Action.WithArgument<T>(this, help = help, handler = handler)
        // TODO: verify that there is at least one name
        // TODO: verify that all names are of same type (or split positional actions into their own method?)
        // TODO: verify that positional actions have exactly one name
        for (name in names) {
            register(name, action)
        }
        return action
    }

    open class Exception(message: String, val returnCode: Int) : java.lang.Exception(message) {
        fun printAndExit(): Nothing {
            // TODO: include program name?
            System.err.println(message)
            exitProcess(returnCode)
        }
    }

    class InvalidOption(val argName: String) :
            OptionParser.Exception("invalid option -- '$argName'", 2)

    // TODO: rename to Delegate?
    // TODO: merge with/without argument, and change Input to allow collection of 0 to n arguments
    sealed class Action<T>(private val argParser: OptionParser) {
        protected var holder: Holder<T>? = null

        class WithArgument<T>(argParser: OptionParser, val help: String?, val handler: Input<T>.() -> T) :
                Action<T>(argParser) {

            data class Input<T>(
                    val value: Holder<T>?,
                    val name: String,
                    val argument: String)

            fun parseNameArgument(name: String, argument: String) {
                holder = Holder(handler(Input(holder, name, argument)))
            }
        }

        class WithoutArgument<T>(argParser: OptionParser, val help: String?, val handler: Input<T>.() -> T) :
                Action<T>(argParser) {
            data class Input<T>(
                    val value: Holder<T>?,
                    val name: String)

            fun parseName(name: String) {
                holder = Holder(handler(Input(holder, name)))
            }
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            argParser.parseOptions
            return holder!!.value
        }

        fun  default(value: T): Action<T> {
            // TODO: make this lazy?
            holder = Holder(value)
            return this
        }
    }

    private val shortFlags = mutableMapOf<Char, Action<*>>()
    private val longFlags = mutableMapOf<String, Action<*>>()

    private fun <T> register(name: String, action: OptionParser.Action<T>) {
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

    private val parseOptions by lazy {
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            val nextArg = if (i + 1 < args.size) args[i + 1] else null
            if (arg.startsWith("--")) {
                // TODO: pass in hyphens to parseLongOpt?
                if (parseLongOpt(arg.substring(2), nextArg)) i++
            } else if (arg.startsWith("-")) {
                if(parseShortOpts(arg.substring(1), nextArg)) i++
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

    private fun parseLongOpt(arg: String, nextArg: String?): Boolean {
        val optName: String
        val optArg: String?
        val sawEqual: Boolean
        val m = NAME_EQUALS_VALUE_REGEX.matchEntire(arg)
        if (m == null) {
            optName = arg
            optArg = nextArg
            sawEqual = false
        } else {
            optName = m.groups[1]!!.value
            optArg = m.groups[2]!!.value
            sawEqual = true
        }
        val action = longFlags.get(optName)
        if (action == null) {
            throw InvalidOption(optName)
        } else {
            when(action) {
                is Action.WithArgument -> {
                    if (optArg == null)
                        TODO("throw exception: option '--$optName' requires an argument")
                    action.parseNameArgument(optName, optArg)
                    return !sawEqual
                }
                is Action.WithoutArgument -> {
                    if (sawEqual)
                        TODO("throw exception: option '--$optName' doesn't allow an argument")
                    action.parseName(optName)
                    return false
                }
            }
        }
    }

    private fun parseShortOpts(arg: String, start: Int, nextArg: String?): Boolean {
        var pos = start
        while (pos < arg.length) {
            val argName = arg[pos]
            val action = shortFlags.get(argName)
            if (action == null) {
                throw InvalidOption(argName.toString())
            } else {
                when(action) {
                    is Action.WithArgument -> {
                        if (pos == arg.length - 1) {
                            if (nextArg == null)
                                TODO("throw exception: option '--$argName' requires an argument")
                            action.parseNameArgument(argName.toString(), nextArg)
                            return true
                        } else {
                            action.parseNameArgument(argName.toString(), arg.substring(pos + 1))
                            return false
                        }
                    }
                    is Action.WithoutArgument -> {
                        action.parseName(argName.toString())
                    }
                }
            }

            pos++
        }
        return false
    }

    private fun parseShortOpts(arg: String, nextArg: String?) = parseShortOpts(arg, 0, nextArg)
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

fun <T, R> T.runMain(f: T.() -> R): R {
    try {
        return f()
    } catch (e: OptionParser.Exception) {
        e.printAndExit()
    }
}
