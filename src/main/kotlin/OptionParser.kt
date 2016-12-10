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
 *         val verbose by parser.flagging("-v", "--verbose")
 *
 *         // simple options with arguments
 *         val name by parser.storing("-N", "--name",
 *             help="My Name")
 *         val size by parser.storing("-s", "--size"
 *             help="My Size"){toInt} = 8
 *
 *         // optional options
 *         val name by parser.storing("-O", "--output",
 *             help="Output location")
 *             .default("./")
 *
 *         // accumulating values (turns into a List)
 *         val includeDirs by parser.adding("-I",
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
    fun flagging(vararg names: String,
                 help: String? = null): Action<Boolean> =
            action<Boolean>(*names, help=help) {true}.default(false)

    fun <T> storing(vararg names: String,
                    help: String? = null,
                    parser: String.()->T): Action<T> =
            action(*names, help=help) {parser(this.next())}

    fun storing(vararg names: String,
                help: String? = null): Action<String> =
            storing(*names, help=help){this}

    /**
     * Adds argument to a MutableCollection.
     */
    fun <E, T : MutableCollection<E>> adding(vararg names: String,
                                             help: String? = null,
                                             initialValue: T,
                                             parser: String.()->E): Action<T> =
            action<T>(*names, help=help) {
                value!!.value.add(parser(next()))
                value.value
            }.default(initialValue)

// TODO: figure out why this causes "cannot choose among the following candidates" errors everywhere.
//    /**
//     * Convenience for adding argument as an unmodified String to a MutableCollection.
//     */
//    fun <T : MutableCollection<String>> adding(vararg names: String,
//                   help: String? = null,
//                   initialValue: T): Action<T> =
//            adding(*names, help = help, initialValue = initialValue){this}

    /**
     * Convenience for adding argument to a MutableList.
     */
    fun <T> adding(vararg names: String,
                   help: String? = null,
                   parser: String.()->T) =
         adding(*names, help = help, initialValue = mutableListOf(), parser = parser)

    /**
     * Convenience for adding argument as an unmodified String to a MutableList.
     */
    fun adding(vararg names: String,
               help: String? = null): Action<MutableList<String>> =
        adding(*names, help = help){this}

    fun <T> action(vararg names: String,
                   help: String? = null,
                   handler: Action.Input<T>.() -> T): Action<T> {
        val action = Action<T>(this, help = help, handler = handler)
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
    class Action<T>(private val argParser: OptionParser, val help: String?, val handler: Input<T>.() -> T) {
        /**
         * Sets the value for this Action. Should be called prior to parsing.
         */
        fun  default(value: T): Action<T> {
            // TODO: throw exception if parsing already complete?
            holder = Holder(value)
            return this
        }

        class Input<T>(val value: Holder<T>?,
                       val name: String,
                       val firstArg: String?,
                       val offset: Int,
                       val args: Array<String>) {

            internal var consumed = 0

            fun hasNext(): Boolean {
                TODO()
            }

            fun next(): String {
                val result : String
                if (firstArg == null) {
                    result = args[offset + consumed]
                } else {
                    result = if (consumed == 0) firstArg else args[offset + consumed - 1]
                }
                consumed++
                return result
            }

            fun peek(): String {
                TODO()
            }
        }

        private var holder: Holder<T>? = null

        internal fun parseOption(name: String, firstArg: String?, index: Int, args: Array<String>) : Int {
            val input = Input(holder, name, firstArg, index, args)
            holder = Holder(handler(input))
            return input.consumed
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            argParser.parseOptions
            return holder!!.value
        }
    }

    private val shortOptions = mutableMapOf<Char, Action<*>>()
    private val longOptions = mutableMapOf<String, Action<*>>()

    private fun <T> register(name: String, action: OptionParser.Action<T>) {
        if (name.startsWith("--")) {
            if (name.length <= 2)
                throw IllegalArgumentException("illegal long option '$name' -- must have at least one character after hyphen")
            longOptions.put(name, action)
        } else if (name.startsWith("-")) {
            if (name.length != 2)
                throw IllegalArgumentException("illegal short option '$name' -- can only have one character after hyphen")
            val key = name.get(1)
            if (key in shortOptions)
                throw IllegalStateException("short option '$name' already in use")
            shortOptions.put(key, action)
        } else {
            TODO("registration of positional args")
        }

    }

    private val parseOptions by lazy {
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            // TODO: use when here?
            if (arg.startsWith("--")) {
                i += parseLongOpt(i, args)
            } else if (arg.startsWith("-")) {
                i += parseShortOpts(i, args)
            } else {
                i += parsePositionalArg(i, args)
            }
        }
        // TODO: throw exception if any holders are null
    }

    private fun parsePositionalArg(index: Int, args: Array<String>): Int {
        TODO("${args.slice(index..args.size)}")
    }

    /**
     * @param index index into args, starting at a long option, eg: "--verbose"
     * @param args array of command-line arguments
     * @return number of arguments that have been processed
     */
    private fun parseLongOpt(index: Int, args: Array<String>): Int {
        val name: String
        val firstArg: String?
        val m = NAME_EQUALS_VALUE_REGEX.matchEntire(args[index])
        if (m == null) {
            name = args[index]
            firstArg = null
        } else {
            name = m.groups[1]!!.value
            firstArg = m.groups[2]!!.value
        }
        val action = longOptions.get(name)
        if (action == null) {
            throw InvalidOption(name)
        } else {
            var consumedArgs = action.parseOption(name, firstArg, index + 1, args)
            if (firstArg != null) {
                if (consumedArgs < 1) TODO("throw exception -- =argument not consumed")
                consumedArgs -= 1
            }
            return 1 + consumedArgs
        }
    }

    /**
     * @param index index into args, starting at a set of short options, eg: "-abXv"
     * @param args array of command-line arguments
     * @return number of arguments that have been processed
     */
    private fun parseShortOpts(index: Int, args: Array<String>): Int {
        val opts = args[index]
        var optIndex = 1
        while (optIndex < opts.length) {
            val optName = opts[optIndex]
            optIndex++ // optIndex now points just after optName

            val action = shortOptions.get(optName)
            if (action == null) {
                throw InvalidOption(optName.toString())
            } else {
                // TODO: move substring construction into Input.next()?
                val firstArg = if (optIndex >= opts.length) null else opts.substring(optIndex)
                val consumed = action.parseOption(optName.toString(), firstArg, index + 1, args)
                if (consumed > 0) {
                    return consumed + (if (firstArg == null) 1 else 0)
                }
            }
        }
        return 1
    }
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
