// Copyright © 2016 Laurence Gonsalves
//
// This file is part of kotlin-argparser, a library which can be found at
// http://github.com/xenomachina/kotlin-argparser
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

package com.xenomachina.argparser

import kotlin.reflect.KProperty
import kotlin.system.exitProcess

/**
 * A command-line option/argument parser.
 */
class ArgParser(args: Array<out String>,
                mode: Mode = Mode.GNU,
                helpFormatter: HelpFormatter? = DefaultHelpFormatter()) {
    // TODO: add support for inlining (eg: -@filename)
    // TODO: add sub-command support

    enum class Mode { GNU, POSIX }

    /**
     * Creates a Delegate for a zero-argument option that returns true if and only the option is present in args.
     */
    fun flagging(vararg names: String): Delegate<Boolean> =
            option<Boolean>(
                    *names,
                    valueName = bestOptionName(names),
                    usageArgument = null,
                    isRepeating = false) { true }.default(false)

    /**
     * Creates a Delegate for a zero-argument option that returns the count of how many times the option appears in args.
     */
    fun counting(vararg names: String): Delegate<Int> =
            option<Int>(
                    *names,
                    valueName = bestOptionName(names),
                    usageArgument = null,
                    isRepeating = true) { value.orElse { 0 } + 1 }.default(0)

    /**
     * Creates a Delegate for a single-argument option that stores and returns the option's (transformed) argument.
     */
    inline fun <T> storing(vararg names: String,
                           crossinline transform: String.() -> T): Delegate<T> {
        val valueName = optionNamesToBestArgName(names);
        return option(
                *names,
                valueName = valueName,
                usageArgument = valueName,
                isRepeating = false) { transform(this.next()) }
    }

    /**
     * Creates a Delegate for a single-argument option that stores and returns the option's argument.
     */
    fun storing(vararg names: String): Delegate<String> =
            storing(*names) { this }

    /**
     * Creates a Delegate for a single-argument option that adds the option's (transformed) argument to a
     * MutableCollection each time the option appears in args, and returns said MutableCollection.
     */
    inline fun <E, T : MutableCollection<E>> adding(vararg names: String,
                                                    initialValue: T,
                                                    crossinline transform: String.() -> E): Delegate<T> {
        val valueName = optionNamesToBestArgName(names)
        return option<T>(
                *names,
                valueName = valueName,
                usageArgument = valueName,
                isRepeating = true) {
            value!!.value.add(transform(next()))
            value.value
        }.default(initialValue)
    }

    /**
     * Creates a Delegate for a single-argument option that adds the option's (transformed) argument to a
     * MutableList each time the option appears in args, and returns said MutableCollection.
     */
    inline fun <T> adding(vararg names: String,
                          crossinline transform: String.() -> T) =
            adding(*names, initialValue = mutableListOf(), transform = transform)

    /**
     * Creates a Delegate for a single-argument option that adds the option's argument to a MutableList each time the
     * option appears in args, and returns said MutableCollection.
     */
    fun adding(vararg names: String): Delegate<MutableList<String>> =
            adding(*names) { this }

    /**
     * Creates a Delegate for a zero-argument option that maps from the option's name as it appears in args to one of a
     * fixed set of values.
     */
    fun <T> mapping(vararg pairs: Pair<String, T>): Delegate<T> =
            mapping(mapOf(*pairs))

    /**
     * Creates a Delegate for a zero-argument option that maps from the option's name as it appears in args to one of a
     * fixed set of values.
     */
    fun <T> mapping(map: Map<String, T>): Delegate<T> {
        val names = map.keys.toTypedArray()
        return option(*names,
                valueName = map.keys.joinToString("|"),
                usageArgument = null,
                isRepeating = false) {
            map[optionName]!!
        }
    }

    /**
     * Creates a Delegate for an option with the specified names.
     * @param names names of options, with leading "-" or "--"
     * @param valueName name to use when talking about value of this option in error messages
     * @param usageArgument how to represent argument(s) in help text, or null if consumes no arguments
     * @param handler A function that assists in parsing arguments by computing the value of this option
     */
    fun <T> option(vararg names: String,
                   valueName: String,
                   usageArgument: String?,
                   isRepeating: Boolean = true,
                   handler: OptionArgumentIterator<T>.() -> T): Delegate<T> {
        val delegate = OptionDelegate<T>(
                parser = this,
                valueName = valueName,
                optionNames = listOf(*names),
                usageArgument = usageArgument,
                isRepeating = isRepeating,
                handler = handler)
        for (name in names) {
            registerOption(name, delegate)
        }
        return delegate
    }

    /**
     * Creates a Delegate for a single positional argument which returns the argument's value.
     */
    fun positional(name: String) = positional(name) { this }

    /**
     * Creates a Delegate for a single positional argument which returns the argument's transformed value.
     */
    fun <T> positional(name: String,
                       transform: String.() -> T): Delegate<T> {
        return object : WrappingDelegate<List<T>, T>(positionalList(name, 1..1, transform)) {
            override fun wrap(u: List<T>): T = u[0]

            override fun unwrap(w: T): List<T> = listOf(w)
        }
    }

    /**
     * Creates a Delegate for a sequence of positional arguments which returns a List containing the arguments.
     */
    fun positionalList(name: String,
                       sizeRange: IntRange = 1..Int.MAX_VALUE) =
            positionalList(name, sizeRange) { this }

    /**
     * Creates a Delegate for a sequence of positional arguments which returns a List containing the transformed
     * arguments.
     */
    fun <T> positionalList(name: String,
                           sizeRange: IntRange = 1..Int.MAX_VALUE,
                           transform: String.() -> T): Delegate<List<T>> {
        sizeRange.run {
            if (step != 1)
                throw IllegalArgumentException("step must be 1, not $step")
            if (first > last)
                throw IllegalArgumentException("backwards ranges are not allowed: $first > $last")
            if (first < 0)
                throw IllegalArgumentException("sizeRange cannot start at $first, must be non-negative")
            // technically last == 0 is ok, but not especially useful so we
            // disallow it as it's probably unintentional
            if (last < 1)
                throw IllegalArgumentException("sizeRange only allows $last arguments, must allow at least 1")
        }

        return PositionalDelegate<T>(this, name, sizeRange, transform).apply {
            positionalDelegates.add(this)
        }
    }

    abstract class WrappingDelegate<U, W>(private val inner: Delegate<U>) : Delegate<W> {

        abstract fun wrap(u: U): W
        abstract fun unwrap(w: W): U

        override val value: W
            get() = wrap(inner.value)

        override val valueName: String
            get() = inner.valueName

        override fun default(value: W): Delegate<W> =
                apply { inner.default(unwrap(value)) }

        override fun help(help: String): Delegate<W> =
                apply { inner.help(help) }

        override fun addValidtator(validator: Delegate<W>.() -> Unit): Delegate<W> =
                apply { validator(this) }
    }

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
            val parser: ArgParser,
            override val valueName: String) : Delegate<T> {

        protected var holder: Holder<T>? = null
        protected var helpText: String? = null

        init {
            parser.assertNotParsed()
            parser.delegates.add(this)
        }

        /**
         * Sets the value for this Delegate. Should be called prior to parsing.
         */
        override fun default(value: T): Delegate<T> {
            parser.assertNotParsed()
            holder = Holder(value)
            return this
        }

        override fun help(help: String): Delegate<T> {
            parser.assertNotParsed()
            this.helpText = help
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

        abstract fun toValueHelp(): HelpFormatter.Value

        private val validators = mutableListOf<Delegate<T>.() -> Unit>()
    }

    private class OptionDelegate<T>(
            parser: ArgParser,
            valueName: String,
            val optionNames: List<String>,
            val usageArgument: String?,
            val isRepeating: Boolean,
            val handler: OptionArgumentIterator<T>.() -> T) : ParsingDelegate<T>(parser, valueName) {

        fun parseOption(name: String, firstArg: String?, index: Int, args: Array<out String>): Int {
            val input = OptionArgumentIterator(holder, name, firstArg, index, args)
            holder = Holder(handler(input))
            return input.consumed
        }

        override fun toValueHelp(): HelpFormatter.Value {
            return HelpFormatter.Value(
                    isRequired = (holder == null),
                    isRepeating = isRepeating,
                    usages = if (usageArgument != null)
                                 optionNames.map { "$it $usageArgument" }
                             else optionNames,
                    isPositional = false,
                    help = helpText)
        }
    }

    private class PositionalDelegate<T>(
            parser: ArgParser,
            valueName: String,
            val sizeRange: IntRange,
            val f: String.() -> T) : ParsingDelegate<List<T>>(parser, valueName) {

        fun parseArguments(args: List<String>) {
            holder = Holder(args.map(f))
        }

        override fun toValueHelp(): HelpFormatter.Value {
            return HelpFormatter.Value(
                    isRequired = sizeRange.first > 0,
                    isRepeating = sizeRange.last > 1,
                    usages = listOf(valueName),
                    isPositional = true,
                    help = helpText)
        }
    }

    /**
     * Iterator over arguments that follow the option being processed.
     * @property value a Holder containing the current value associated with this option, or null if unset
     * @property optionName the name of the option
     */
    class OptionArgumentIterator<T> internal constructor(
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

    private val shortOptionDelegates = mutableMapOf<Char, OptionDelegate<*>>()
    private val longOptionDelegates = mutableMapOf<String, OptionDelegate<*>>()
    private val positionalDelegates = mutableListOf<PositionalDelegate<*>>()
    private val delegates = mutableListOf<ParsingDelegate<*>>()

    private fun <T> registerOption(name: String, delegate: OptionDelegate<T>) {
        if (name.startsWith("--")) {
            if (name.length <= 2)
                throw IllegalArgumentException("long option '$name' must have at least one character after hyphen")
            if (name in longOptionDelegates)
                throw IllegalStateException("long option '$name' already in use")
            longOptionDelegates.put(name, delegate)
        } else if (name.startsWith("-")) {
            if (name.length != 2)
                throw IllegalArgumentException("short option '$name' can only have one character after hyphen")
            val key = name.get(1)
            if (key in shortOptionDelegates)
                throw IllegalStateException("short option '$name' already in use")
            shortOptionDelegates.put(key, delegate)
        } else {
            throw IllegalArgumentException("illegal option name '$name' -- must start with '-' or '--'")
        }

    }

    private var inValidation = false
    private var finished = false

    fun force() {
        if (!finished) {
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
        finished = true
    }

    private var parseStarted = false

    private fun assertNotParsed() {
        if (parseStarted) throw IllegalStateException("arguments have already been parsed")
    }

    private val parseOptions by lazy {
        val positionalArguments = mutableListOf<String>()
        parseStarted = true
        var i = 0
        optionLoop@ while (i < args.size) {
            val arg = args[i]
            i += when {
                arg == "--" -> {
                    i++
                    break@optionLoop
                }
                arg.startsWith("--") ->
                    parseLongOpt(i, args)
                arg.startsWith("-") ->
                    parseShortOpts(i, args)
                else -> {
                    positionalArguments.add(arg)
                    when (mode) {
                        Mode.GNU -> 1
                        Mode.POSIX -> {
                            i++
                            break@optionLoop
                        }
                    }
                }
            }
        }

        // Collect remaining arguments as positional-only arguments
        positionalArguments.addAll(args.slice(i..args.size - 1))

        parsePositionalArguments(positionalArguments)
    }

    private fun parsePositionalArguments(args: List<String>) {
        var lastValueName: String? = null
        var index = 0
        var remaining = args.size
        var extra = (remaining - positionalDelegates.map { it.sizeRange.first }.sum()).coerceAtLeast(0)
        for (delegate in positionalDelegates) {
            val sizeRange = delegate.sizeRange
            val chunkSize = (sizeRange.first + extra).coerceIn(sizeRange)
            if (chunkSize > remaining) {
                throw MissingRequiredPositionalArgumentException(delegate.valueName)
            }
            delegate.parseArguments(args.subList(index, index + chunkSize))
            lastValueName = delegate.valueName
            index += chunkSize
            remaining -= chunkSize
            extra -= chunkSize - sizeRange.first
        }
        if (remaining > 0) {
            throw UnexpectedPositionalArgumentException(lastValueName)
        }
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
        val delegate = longOptionDelegates.get(name)
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

            val delegate = shortOptionDelegates.get(optKey)
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

    init {
        if (helpFormatter != null) {
            option<Unit>("-h", "--help",
                    valueName = "SHOW_HELP",
                    usageArgument = null,
                    isRepeating = false) {
                throw ShowHelpException {
                    progName ->
                    helpFormatter.format(progName, delegates.map { it.toValueHelp() })
                }
            }.default(Unit).help("show this help message and exit")
        }
    }
}

/**
 * Compensates for the fact that nullable types don't compose in Kotlin. If you want to be able to distinguish between a
 * T (where T may or may not be a nullable type) and lack of a T, use a Holder<T>?.
 */
data class Holder<T>(val value: T)

fun <T> Holder<T>?.orElse(f: () -> T): T {
    if (this == null) {
        return f()
    } else {
        return value
    }
}

interface HelpFormatter {
    fun format(progName: String?, values: List<Value>): String

    /**
     * An option or positional argument type which should be formatted for help
     *
     * @param usages possible usage strings for this argument type
     * @param isRequired indicates whether this is required
     * @param isRepeating indicates whether it makes sense to repeat this argument
     * @param isPositional indicates whether this is a positional argument
     * @param help help text provided at Delegate construction time
     */
    data class Value(
            val usages: List<String>,
            val isRequired: Boolean,
            val isRepeating: Boolean,
            val isPositional: Boolean,
            val help: String?)
}

// TODO: implement word wrapping and fix indentation
class DefaultHelpFormatter(val prologue: String? = null,
                           val epilogue: String? = null) : HelpFormatter {
    override fun format(progName: String?,
                        values: List<HelpFormatter.Value>): String {
        val sb = StringBuilder()
        appendUsage(sb, progName, values)

        if (!prologue.isNullOrEmpty()) {
            sb.append("\n")
            sb.append(prologue)
            sb.append("\n")
        }

        val required = mutableListOf<HelpFormatter.Value>()
        val optional = mutableListOf<HelpFormatter.Value>()
        val positional = mutableListOf<HelpFormatter.Value>()

        for (value in values) {
            when {
                value.isPositional -> positional
                value.isRequired -> required
                else -> optional
            }.add(value)
        }
        appendSection(sb, "required", required)
        appendSection(sb, "optional", optional)
        appendSection(sb, "positional", positional)

        if (!epilogue.isNullOrEmpty()) {
            sb.append("\n")
            sb.append(epilogue)
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun appendSection(sb: StringBuilder, name: String, values: List<HelpFormatter.Value>) {
        if (!values.isEmpty()) {
            sb.append("\n")
            sb.append("$name arguments:\n")
            for (value in values) {
                sb.append("  ")
                sb.append(value.usages.joinToString(", "))
                value.help?.let {
                    sb.append("\t")
                    sb.append(it)
                }
                sb.append("\n")
            }
        }
    }

    private fun appendUsage(sb: StringBuilder, progName: String?, values: List<HelpFormatter.Value>) {
        sb.append("usage:")
        if (progName != null) sb.append(" $progName")
        for (value in values) value.run {
            if (!usages.isEmpty()) {
                val usage = usages[0]
                if (isRequired) {
                    sb.append(" $usage")
                } else {
                    sb.append(" [$usage]")
                }
                if (isRepeating) {
                    sb.append("...")
                }
            }
        }
        sb.append("\n")
    }
}

class ShowHelpException(val formatHelp: (String?) -> String) : SystemExitException("Help was requested", 0) {
    override fun printAndExit(progName: String?): Nothing {
        System.out.print(formatHelp(progName))
        exitProcess(returnCode)
    }
}
