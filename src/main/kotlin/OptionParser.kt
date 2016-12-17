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
    // TODO: add support for inlining (eg: -@filename)
    // TODO: add sub-command support

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
            map[optionName]!!
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
                   handler: OptionArgumentIterator<T>.() -> T): Delegate<T> {
        val delegate = OptionDelegate<T>(
                parser = this,
                valueName = valueName,
                handler = handler)
        delegates.add(delegate)
        for (name in names) {
            registerOption(name, delegate)
        }
        return delegate
    }

    fun <T> argument(name: String,
                     parser: String.() -> T) : Delegate<T> {
        return object : WrappingDelegate<List<T>, T>(argumentList(name, 1..1, parser)) {
            override fun wrap(u: List<T>): T = u[0]

            override fun unwrap(w: T): List<T> = listOf(w)
        }
    }

    fun <T> argumentList(name: String,
                         sizeRange: IntRange,
                         parser: String.() -> T) : Delegate<List<T>> {
        return PositionalDelegate<T>(this, name, sizeRange, parser)
    }

    abstract class WrappingDelegate<U, W>(private val inner: Delegate<U>) : Delegate<W> {

        abstract fun wrap(u: U) : W
        abstract fun unwrap(w: W) : U

        override val value: W
            get() = wrap(inner.value)

        override val valueName: String
            get() = inner.valueName

        override fun default(w: W): Delegate<W> =
                apply { inner.default(unwrap(w)) }

        override fun help(help: String): Delegate<W> =
                apply { inner.help(help) }

        override fun addValidtator(validator: Delegate<W>.() -> Unit): Delegate<W> =
                apply { validator(this) }
    }

    // TODO: add `argument` method for positional argument handling
    // TODO: verify that positional arguments have exactly one name

    interface Delegate<T> {
        /** The value associated with this delegate */
        val value: T

        /** The name of the value associated with this delegate */
        val valueName: String

        /** Allows this object to act as a property delegate */
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

        // Fluent setters:

        /** Set default value */
        fun default(value: T): Delegate<T>

        /** Set help text */
        fun help(help: String): Delegate<T>

        /** Add validation logic. Validator should throw a [SystemExitException] on failure. */
        fun addValidtator(validator: Delegate<T>.() -> Unit): Delegate<T>
    }

    private abstract class ParsingDelegate<T>(
            val parser: OptionParser, // TODO: replace with ParsingState
            override val valueName: String) : Delegate<T> {

        protected var holder: Holder<T>? = null

        init {
            parser.assertNotParsed("create ${javaClass.canonicalName}")
        }

        /**
         * Sets the value for this Delegate. Should be called prior to parsing.
         */
        override fun default(value: T): Delegate<T> {
            parser.assertNotParsed("set default")
            holder = Holder(value)
            return this
        }

        override fun help(help: String): Delegate<T> {
            parser.assertNotParsed("set help")
            return this
        }

        override fun addValidtator(validator: Delegate<T>.() -> Unit): Delegate<T> = apply {
            validators.add(validator)
        }

        override val value: T
            get() {
                parser.force()
                return holder!!.value
            }

        fun preValidate() {
            if (holder == null)
                throw MissingValueException(valueName)
        }

        fun validate() {
            for (validator in validators) validator()
        }

        private val validators = mutableListOf<Delegate<T>.() -> Unit>()
    }

    private class OptionDelegate<T>(
            parser: OptionParser,
            valueName: String,
            val handler: OptionArgumentIterator<T>.() -> T) : ParsingDelegate<T>(parser, valueName) {

        fun parseOption(name: String, firstArg: String?, index: Int, args: Array<out String>): Int {
            val input = OptionArgumentIterator(holder, name, firstArg, index, args)
            holder = Holder(handler(input))
            return input.consumed
        }
    }

    private class PositionalDelegate<T>(
            parser: OptionParser,
            valueName: String,
            val sizeRange: IntRange,
            val f: String.() -> T) : ParsingDelegate<List<T>>(parser, valueName) {

        fun parseArguments(args: Array<out String>, indeRange: IntRange) {
            TODO()
        }
    }

    // TODO: pass valueName down to OptionArgumentIterator?
    /**
     * Iterator over arguments that follow the option being processed.
     * @property value a Holder containing the current value associated with this option, or null if unset
     * @property optionName the name of the option
     */
    class OptionArgumentIterator<T> internal constructor (
            val value: Holder<T>?,
            val optionName: String,
            private val firstArg: String?,
            private val offset: Int,
            private val args: Array<out String>) {

        internal var consumed = 0

        /**
         * Returns the next argument without advancing the current position, or null if there is no next argument. This
         * should only be used to determine if the next argument is to be consumed. If its value will impact the value
         * associated with this option then [next()] must be called to advance the current position.
         *
         * Do *not* use the presence of a leading hyphen ('-') as an indication that the next argument should not be
         * consumed.
         */
        fun peek(): String? {
            return if (firstArg != null && consumed == 0) {
                firstArg
            } else {
                val index = offset + consumed - (if (firstArg == null) 0 else 1)
                if (index >= args.size) {
                    null
                } else {
                    args[index]
                }
            }
        }

        /**
         * Indicates whether there is another argument available to this option
         */
        fun hasNext(): Boolean = peek() != null

        /**
         * @returns the next argument available to this option
         * @throws OptionMissingRequiredArgumentException if no more arguments are available
         */
        fun next(): String {
            return peek()?.apply { consumed++ }
                    ?: throw OptionMissingRequiredArgumentException(optionName)
        }
    }

    private val shortOptions = mutableMapOf<Char, OptionDelegate<*>>()
    private val longOptions = mutableMapOf<String, OptionDelegate<*>>()
    private val delegates = mutableListOf<ParsingDelegate<*>>()

    private fun <T> registerOption(name: String, delegate: OptionDelegate<T>) {
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
            throw IllegalArgumentException("illegal option name '$name' -- must start with '-' or '--'")
        }

    }

    private var inValidation = false
    private fun force() {
        parseOptions
        if (!inValidation) {
            inValidation = true
            try {
                for (delegate in delegates) delegate.preValidate()
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
                if (consumedArgs < 1) throw UnexpectedOptionArgumentException(name)
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
