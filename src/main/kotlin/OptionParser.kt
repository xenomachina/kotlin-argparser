// Copyright © 2016 Laurence Gonsalves
//
// This file is part of kotlin-optionparser, a library which can be found at
// http://github.com/xenomachina/kotlin-optionparser
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by the
// Free Software Foundation; either version 2.1 of the License, or (at your
// option) any later version.
//
// This library is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
// for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this library; if not, see http://www.gnu.org/licenses/

package com.xenomachina.optionparser

import kotlin.reflect.KProperty

/**
 * A command-line option/argument parser.
 */
class OptionParser(val args: Array<out String>) {
    // TODO: add --help support
    // TODO: add "--" support
    // TODO: make it possible to inline arguments from elsewhere (eg: -@filename)

    /**
     * Returns a Delegate that returns true if and only if an option with one of specified names is present.
     */
    fun flagging(vararg names: String): Delegate<Boolean> =
            option<Boolean>(*names, valueName = bestOptionName(names)) { true }.default(false)

    /**
     * Returns an option Delegate that returns the option's parsed argument.
     */
    inline fun <T> storing(vararg names: String,
                           crossinline parser: String.() -> T): Delegate<T> =
            option(*names, valueName = optionNamesToBestArgName(names)) { parser(this.next()) }

    /**
     * Returns an option Delegate that returns the option's unparsed argument.
     */
    fun storing(vararg names: String): Delegate<String> =
            storing(*names) { this }

    /**
     * Returns an option Delegate that adds the option's parsed argument to a MutableCollection.
     */
    inline fun <E, T : MutableCollection<E>> adding(vararg names: String,
                                             initialValue: T,
                                             crossinline parser: String.() -> E): Delegate<T> =
        option<T>(*names, valueName = optionNamesToBestArgName(names) + ELLIPSIS) {
            value!!.value.add(parser(next()))
            value.value
        }.default(initialValue)

    /**
     * Returns an option Delegate that adds the option's parsed argument to a MutableList.
     */
    inline fun <T> adding(vararg names: String,
                   crossinline parser: String.() -> T) =
            adding(*names, initialValue = mutableListOf(), parser = parser)

    /**
     * Returns an option Delegate that adds the option's unparsed argument to a MutableList.
     */
    fun adding(vararg names: String): Delegate<MutableList<String>> =
            adding(*names) { this }

    /**
     * Returns an option Delegate that maps from th option name to a value.
     */
    fun <T> mapping(vararg pairs: Pair<String, T>): Delegate<T> =
            mapping(mapOf(*pairs))

    /**
     * Returns an option Delegate that maps from th option name to a value.
     */
    fun <T> mapping(map: Map<String, T>): Delegate<T> {
        val names = map.keys.toTypedArray()
        return option(*names,
                valueName = map.keys.joinToString("|")){
            map[name]!!
        }
    }

    /**
     * Returns an option Delegate that handles options with the specified names.
     * @param names names of options, with leading "-" or "--"
     * @param valueName name to use when talking about value of this option in error messages or help text
     * @param handler A function that assists in parsing arguments by computing the value of this option
     */
    fun <T> option(vararg names: String,
                   valueName: String,
                   handler: Delegate.Input<T>.() -> T): Delegate<T> {
        val delegate = Delegate<T>(
                parser = this,
                valueName = valueName,
                handler = handler)
        delegates.add(delegate)
        for (name in names) {
            register(name, delegate)
        }
        return delegate
    }

    // TODO: add `argument` method for positional argument handling
    // TODO: verify that positional arguments have exactly one name

    // TODO: add addValidator method
    class Delegate<T> internal constructor (private val parser: OptionParser,
                                            val valueName: String,
                                            val handler: Input<T>.() -> T) {
        init {
            parser.assertNotParsed("create ${javaClass.canonicalName}")
        }

        /**
         * Sets the value for this Delegate. Should be called prior to parsing.
         */
        fun default(value: T): Delegate<T> {
            parser.assertNotParsed("set default")
            holder = Holder(value)
            return this
        }

        fun help(help: String): Delegate<T> {
            parser.assertNotParsed("set help")
            return this
        }

        // TODO: pass valueName down to Input?
        class Input<T> internal constructor (
                val value: Holder<T>?,
                val name: String,
                val firstArg: String?,
                val offset: Int,
                val args: Array<out String>,
                private val parser: OptionParser) {

            internal var consumed = 0

            fun hasNext(): Boolean {
                TODO("hasNext")
            }

            fun next(): String {
                try {
                    val result = if (firstArg == null) {
                        args[offset + consumed]
                    } else if (consumed == 0) {
                        firstArg
                    } else {
                        args[offset + consumed - 1]
                    }
                    consumed++
                    return result
                } catch (aioobx: ArrayIndexOutOfBoundsException) {
                    throw OptionMissingRequiredArgumentException(name)
                }
            }

            fun peek(): String {
                TODO("peek")
            }
        }

        private var holder: Holder<T>? = null

        internal fun parseOption(name: String, firstArg: String?, index: Int, args: Array<out String>): Int {
            val input = Input(holder, name, firstArg, index, args, parser)
            holder = Holder(handler(input))
            return input.consumed
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            parser.force()
            return holder!!.value
        }

        internal fun validate() {
            if (holder == null)
                throw MissingValueException(valueName)
        }
    }

    private val shortOptions = mutableMapOf<Char, Delegate<*>>()
    private val longOptions = mutableMapOf<String, Delegate<*>>()
    private val delegates = mutableListOf<Delegate<*>>()

    private fun <T> register(name: String, delegate: OptionParser.Delegate<T>) {
        if (name.startsWith("--")) {
            if (name.length <= 2)
                throw IllegalArgumentException("long option '$name' must have at least one character after hyphen")
            if (name in longOptions)
                throw IllegalStateException("long option '$name' already in use")
            longOptions.put(name, delegate)
        } else if (name.startsWith("-")) {
            if (name.length != 2)
                throw IllegalArgumentException("short option '$name' can only have one character after hyphen")
            val key = name.get(1)
            if (key in shortOptions)
                throw IllegalStateException("short option '$name' already in use")
            shortOptions.put(key, delegate)
        } else {
            TODO("registration of positional args")
        }

    }

    private var inValidation = false
    private fun force() {
        parseOptions
        if (!inValidation) {
            inValidation = true
            try {
                for (delegate in delegates) delegate.validate()
            } finally {
                inValidation = false
            }
        }
    }

    private var parseStarted = false

    private fun assertNotParsed(verb: String) {
        if (parseStarted) throw IllegalStateException("Cannot ${verb} after parsing arguments")
    }

    private val parseOptions by lazy {
        parseStarted = true
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            i += when {
                arg.startsWith("--") ->
                    parseLongOpt(i, args)
                arg.startsWith("-") ->
                    parseShortOpts(i, args)
                else ->
                    parsePositionalArg(i, args)
            }
        }
    }

    private fun parsePositionalArg(index: Int, args: Array<out String>): Int {
        TODO("parsePositionalArg ${args.slice(index..args.size)}")
    }

    /**
     * @param index index into args, starting at a long option, eg: "--verbose"
     * @param args array of command-line arguments
     * @return number of arguments that have been processed
     */
    private fun parseLongOpt(index: Int, args: Array<out String>): Int {
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
        val delegate = longOptions.get(name)
        if (delegate == null) {
            throw UnrecognizedOptionException(name)
        } else {
            var consumedArgs = delegate.parseOption(name, firstArg, index + 1, args)
            if (firstArg != null) {
                if (consumedArgs < 1) TODO("throw exception =argument not consumed")
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
    private fun parseShortOpts(index: Int, args: Array<out String>): Int {
        val opts = args[index]
        var optIndex = 1
        while (optIndex < opts.length) {
            val optKey = opts[optIndex]
            val optName = "-$optKey"
            optIndex++ // optIndex now points just after optKey

            val delegate = shortOptions.get(optKey)
            if (delegate == null) {
                throw UnrecognizedOptionException(optName)
            } else {
                val firstArg = if (optIndex >= opts.length) null else opts.substring(optIndex)
                val consumed = delegate.parseOption(optName, firstArg, index + 1, args)
                // TODO: test case where firstArg not consumed
                if (consumed > 0) {
                    return consumed + (if (firstArg == null) 1 else 0)
                }
            }
        }
        return 1
    }

    companion object {
        private val NAME_EQUALS_VALUE_REGEX = Regex("^([^=]+)=(.*)$")
        private val LEADING_HYPHENS = Regex("^-{1,2}")

        val ELLIPSIS = "..."

        fun bestOptionName(names: Array<out String>): String {
            if (names.size < 1)
                throw IllegalArgumentException("need at least one option name")
            // Return first long option...
            for (name in names) {
                if (name.startsWith("--")) {
                    return name
                }
            }
            // ... but failing that just return first option.
            return names[0]
        }

        fun optionNamesToBestArgName(names: Array<out String>): String {
            return optionNameToArgName(bestOptionName(names))
        }

        private fun optionNameToArgName(name: String) =
                LEADING_HYPHENS.replace(name, "").toUpperCase().replace('-', '_')
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
