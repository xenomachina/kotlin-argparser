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

import com.xenomachina.argparser.ArgParser.DelegateProvider.Companion.identifierToArgName
import com.xenomachina.argparser.ArgParser.DelegateProvider.Companion.identifierToOptionName
import com.xenomachina.common.Holder
import com.xenomachina.common.orElse
import com.xenomachina.text.NBSP_CODEPOINT
import com.xenomachina.text.term.codePointWidth
import com.xenomachina.text.term.columnize
import com.xenomachina.text.term.wrapText
import java.io.Writer
import java.util.LinkedHashSet
import kotlin.reflect.KProperty

/**
 * A command-line option/argument parser.
 *
 * @param args the command line arguments to parse
 * @param mode parsing mode, defaults to GNU-style parsing
 * @param helpFormatter if non-null, creates `--help` and `-h` options that trigger a [ShowHelpException] which will use
 * the supplied [HelpFormatter] to generate a help message.
 */
class ArgParser(args: Array<out String>,
                mode: Mode = Mode.GNU,
                helpFormatter: HelpFormatter? = DefaultHelpFormatter()) {

    enum class Mode {
        /** For GNU-style option parsing, where options may appear after positional arguments. */
        GNU,

        /** For POSIX-style option parsing, where options must appear before positional arguments. */
        POSIX
    }

    /**
     * Creates a Delegate for a zero-argument option that returns true if and only the option is present in args.
     */
    fun flagging(vararg names: String, help: String): Delegate<Boolean> =
            option<Boolean>(
                    *names,
                    errorName = optionNamesToErrorName(names),
                    help = help) { true }.default(false)

    /**
     * Creates a DelegateProvider for a zero-argument option that returns true if and only the option is present in args.
     */
    fun flagging(help: String) =
            DelegateProvider { identifier -> flagging(identifierToOptionName(identifier), help = help) }

    /**
     * Creates a Delegate for a zero-argument option that returns the count of how many times the option appears in args.
     */
    fun counting(vararg names: String, help: String): Delegate<Int> =
            option<Int>(
                    *names,
                    errorName = optionNamesToErrorName(names),
                    isRepeating = true,
                    help = help) { value.orElse { 0 } + 1 }.default(0)

    /**
     * Creates a DelegateProvider for a zero-argument option that returns the count of how many times the option appears in args.
     */
    fun counting(help: String) = DelegateProvider { identifier -> counting(identifierToOptionName(identifier), help = help) }

    /**
     * Creates a Delegate for a single-argument option that stores and returns the option's (transformed) argument.
     */
    fun <T> storing(
            vararg names: String,
            help: String,
            transform: String.() -> T
    ): Delegate<T> {
        val errorName = optionNamesToErrorName(names)
        return option(
                *names,
                errorName = errorName,
                argNames = listOf(errorName),
                help = help) { transform(arguments.first()) }
    }

    /**
     * Creates a DelegateProvider for a single-argument option that stores and returns the option's (transformed) argument.
     */
    fun <T> storing(
            help: String,
            transform: String.() -> T
    ) = DelegateProvider { identifier -> storing(identifierToOptionName(identifier), help = help, transform = transform) }

    /**
     * Creates a Delegate for a single-argument option that stores and returns the option's argument.
     */
    fun storing(vararg names: String, help: String): Delegate<String> =
            storing(*names, help = help) { this }

    /**
     * Creates a DelegateProvider for a single-argument option that stores and returns the option's argument.
     */
    fun storing(help: String) = DelegateProvider { identifier -> storing(identifierToOptionName(identifier), help = help) }

    /**
     * Creates a Delegate for a single-argument option that adds the option's (transformed) argument to a
     * MutableCollection each time the option appears in args, and returns said MutableCollection.
     */
    fun <E, T : MutableCollection<E>> adding(
            vararg names: String,
            help: String,
            initialValue: T,
            transform: String.() -> E
    ): Delegate<T> {
        val errorName = optionNamesToErrorName(names)
        return option<T>(
                *names,
                errorName = errorName,
                help = help,
                argNames = listOf(errorName),
                isRepeating = true) {
            val result = value.orElse { initialValue }
            result.add(transform(arguments.first()))
            result
        }.default(initialValue)
    }

    /**
     * Creates a DelegateProvider for a single-argument option that adds the option's (transformed) argument to a
     * MutableCollection each time the option appears in args, and returns said MutableCollection.
     */
    fun <E, T : MutableCollection<E>> adding(
            help: String,
            initialValue: T,
            transform: String.() -> E
    ) = DelegateProvider { identifier ->
                adding(identifierToOptionName(identifier), initialValue = initialValue, help = help, transform = transform) }

    /**
     * Creates a Delegate for a single-argument option that adds the option's (transformed) argument to a
     * MutableList each time the option appears in args, and returns said MutableCollection.
     */
    fun <T> adding(
            vararg names: String,
            help: String,
            transform: String.() -> T
    ) = adding(*names, initialValue = mutableListOf(), help = help, transform = transform)

    /**
     * Creates a DelegateProvider for a single-argument option that adds the option's (transformed) argument to a
     * MutableList each time the option appears in args, and returns said MutableCollection.
     */
    fun <T> adding(
            help: String,
            transform: String.() -> T
    ) = DelegateProvider { identifier ->
        adding(identifierToOptionName(identifier), help = help, transform = transform) }

    /**
     * Creates a Delegate for a single-argument option that adds the option's argument to a MutableList each time the
     * option appears in args, and returns said MutableCollection.
     */
    fun adding(vararg names: String, help: String): Delegate<MutableList<String>> =
            adding(*names, help = help) { this }

    /**
     * Creates a DelegateProvider for a single-argument option that adds the option's argument to a MutableList each time the
     * option appears in args, and returns said MutableCollection.
     */
    fun adding(help: String) = DelegateProvider { identifier ->
        adding(identifierToOptionName(identifier), help = help) }

    /**
     * Creates a Delegate for a zero-argument option that maps from the option's name as it appears in args to one of a
     * fixed set of values.
     */
    fun <T> mapping(vararg pairs: Pair<String, T>, help: String): Delegate<T> =
            mapping(mapOf(*pairs), help = help)

    /**
     * Creates a Delegate for a zero-argument option that maps from the option's name as it appears in args to one of a
     * fixed set of values.
     */
    fun <T> mapping(map: Map<String, T>, help: String): Delegate<T> {
        val names = map.keys.toTypedArray()
        return option(*names,
                errorName = map.keys.joinToString("|"),
                help = help) {
            // This cannot be null, because the optionName was added to the map
            // at the same time it was registered with the ArgParser.
            map[optionName]!!
        }
    }

    /**
     * Creates a Delegate for an option with the specified names.
     * @param names names of options, with leading "-" or "--"
     * @param errorName name to use when talking about this option in error messages
     * @param help the help text for this option
     * @param argNames names of this option's arguments
     * @param isRepeating whether or not it make sense to repeat this option -- usually used for options where
     * specifying the option more than once yields a value than cannot be expressed by specifying the option only once
     * @param handler a function that computes the value of this option from an [OptionInvocation]
     */
    fun <T> option(
            // TODO: add optionalArg: Boolean
            vararg names: String,
            help: String,
            // TODO: make errorName nullable, and choose name from option names if null
            errorName: String,
            argNames: List<String> = emptyList(),
            isRepeating: Boolean = false,
            handler: OptionInvocation<T>.() -> T
    ): Delegate<T> {
        val delegate = OptionDelegate<T>(
                parser = this,
                errorName = errorName,
                help = help,
                optionNames = listOf(*names),
                argNames = argNames.toList(),
                isRepeating = isRepeating,
                handler = handler)
        return delegate
    }

    /**
     * Creates a Delegate for a single positional argument which returns the argument's value.
     */
    fun positional(name: String, help: String) = positional(name, help = help) { this }

    /**
     * Creates a DelegateProvider for a single positional argument which returns the argument's value.
     */
    fun positional(help: String) =
            DelegateProvider { identifier -> positional(identifierToArgName(identifier), help = help) }

    /**
     * Creates a Delegate for a single positional argument which returns the argument's transformed value.
     */
    fun <T> positional(
            name: String,
            help: String,
            transform: String.() -> T
    ): Delegate<T> {
        return WrappingDelegate(
                positionalList(name, help = help, sizeRange = 1..1, transform = transform)
        ) { it[0] }
    }

    /**
     * Creates a DelegateProvider for a single positional argument which returns the argument's transformed value.
     */
    fun <T> positional(
            help: String,
            transform: String.() -> T
    ) = DelegateProvider { identifier -> positional(identifierToArgName(identifier), help = help, transform = transform) }

    /**
     * Creates a Delegate for a sequence of positional arguments which returns a List containing the arguments.
     */
    fun positionalList(
            name: String,
            help: String,
            sizeRange: IntRange = 1..Int.MAX_VALUE
    ) = positionalList(name, help = help, sizeRange = sizeRange) { this }

    /**
     * Creates a DelegateProvider for a sequence of positional arguments which returns a List containing the arguments.
     */
    fun positionalList(
            help: String,
            sizeRange: IntRange = 1..Int.MAX_VALUE
    ) = DelegateProvider { identifier -> positionalList(identifierToArgName(identifier), help = help, sizeRange = sizeRange) }

    /**
     * Creates a Delegate for a sequence of positional arguments which returns a List containing the transformed
     * arguments.
     */
    fun <T> positionalList(
            name: String,
            help: String,
            sizeRange: IntRange = 1..Int.MAX_VALUE,
            transform: String.() -> T
    ): Delegate<List<T>> {
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

        return PositionalDelegate<T>(this, name, sizeRange, help = help, transform = transform)
    }

    /**
     * Creates a DelegateProvider for a sequence of positional arguments which returns a List containing the transformed
     * arguments.
     */
    fun <T> positionalList(
            help: String,
            sizeRange: IntRange = 1..Int.MAX_VALUE,
            transform: String.() -> T
    ) = DelegateProvider { identifier -> positionalList(identifierToArgName(identifier), help, sizeRange, transform) }

    internal class WrappingDelegate<U, W>(
            private val inner: Delegate<U>,
            private val wrap: (U) -> W
    ) : Delegate<W>() {

        override val parser: ArgParser
            get() = inner.parser

        override val value: W
            get() = wrap(inner.value)

        override val hasValue: Boolean
            get() = inner.hasValue

        override val errorName: String
            get() = inner.errorName

        override val help: String
            get() = inner.help

        override fun validate() {
            inner.validate()
        }

        override fun toHelpFormatterValue(): HelpFormatter.Value = inner.toHelpFormatterValue()

        override fun addValidator(validator: Delegate<W>.() -> Unit): Delegate<W> =
                apply { inner.addValidator { validator(this@WrappingDelegate) } }

        override fun registerLeaf(root: Delegate<*>) {
            inner.registerLeaf(root)
        }
    }

    abstract class Delegate<out T> internal constructor() {
        /** The value associated with this delegate */
        abstract val value: T

        /** The name used to refer to this delegate's value in error messages */
        abstract val errorName: String

        /** The user-visible help text for this delegate */
        abstract val help: String

        /** Add validation logic. Validator should throw a [SystemExitException] on failure. */
        abstract fun addValidator(validator: Delegate<T>.() -> Unit): Delegate<T>

        /** Allows this object to act as a property delegate */
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

        /**
         * Allows this object to act as a property delegate provider.
         *
         * It provides itself, and also registers itself with the [ArgParser] at that time.
         */
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgParser.Delegate<T> {
            registerRoot()
            return this
        }

        abstract internal val parser: ArgParser

        /**
         * Indicates whether or not a value has been set for this delegate
         */
        internal abstract val hasValue: Boolean

        internal fun checkHasValue() {
            if (!hasValue) throw MissingValueException(errorName)
        }

        internal abstract fun validate()

        internal abstract fun toHelpFormatterValue(): HelpFormatter.Value

        internal fun registerRoot() {
            parser.checkNotParsed()
            parser.delegates.add(this)
            registerLeaf(this)
        }

        internal abstract fun registerLeaf(root: Delegate<*>)
    }

    /**
     * Provides a [Delegate] when given a name. This makes it possible to infer
     * a name for the `Delegate` based on the name it is bound to, rather than
     * specifying a name explicitly.
     */
    class DelegateProvider<out T>(
            private val defaultHolder: Holder<T>? = null,
            internal val ctor: (identifier: String) -> Delegate<T>
    ) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): Delegate<T> {
            val delegate = ctor(prop.name)
            return (if (defaultHolder == null)
                delegate
            else
                delegate.default(defaultHolder.value)).provideDelegate(thisRef, prop)
        }

        companion object {
            fun identifierToOptionName(identifier: String): String {
                return if (identifier.length == 1) ("-" + identifier) else ("--" + identifier.replace('_', '-'))
            }

            fun identifierToArgName(identifier: String): String {
                return identifier.toUpperCase()
            }
        }
    }

    internal abstract class ParsingDelegate<T>(
            override val parser: ArgParser,
            override val errorName: String,
            override val help: String) : Delegate<T>() {

        protected var holder: Holder<T>? = null

        override fun addValidator(validator: Delegate<T>.() -> Unit): Delegate<T> = apply {
            validators.add(validator)
        }

        override val value: T
            get() {
                parser.force()
                // checkHasValue should have ensured that this is non-null
                return holder!!.value
            }

        override val hasValue: Boolean
            get() = holder != null

        override fun validate() {
            for (validator in validators) validator()
        }

        private val validators = mutableListOf<Delegate<T>.() -> Unit>()
    }

    private class OptionDelegate<T>(
            parser: ArgParser,
            errorName: String,
            help: String,
            val optionNames: List<String>,
            val argNames: List<String>,
            val isRepeating: Boolean,
            val handler: OptionInvocation<T>.() -> T) : ParsingDelegate<T>(parser, errorName, help) {
        init {
            for (optionName in optionNames) {
                if (!OPTION_NAME_RE.matches(optionName)) {
                    throw IllegalArgumentException("$optionName is not a valid option name")
                }
            }
            for (argName in argNames) {
                if (!ARG_NAME_RE.matches(argName)) {
                    throw IllegalArgumentException("$argName is not a valid argument name")
                }
            }
        }

        fun parseOption(name: String, firstArg: String?, index: Int, args: Array<out String>): Int {
            val arguments = mutableListOf<String>()
            if (!argNames.isEmpty()) {
                if (firstArg != null) arguments.add(firstArg)
                val required = argNames.size - arguments.size
                if (required + index > args.size) {
                    // Only pass an argName if more than one argument.
                    // Naming it when there's just one seems unnecessarily verbose.
                    val argName = if (argNames.size > 1) argNames[args.size - index] else null
                    throw OptionMissingRequiredArgumentException(name, argName)
                }
                for (i in 0 until required) {
                    arguments.add(args[index + i])
                }
            }
            val input = OptionInvocation(holder, name, arguments)
            holder = Holder(handler(input))
            return argNames.size
        }

        override fun toHelpFormatterValue(): HelpFormatter.Value {
            return HelpFormatter.Value(
                    isRequired = (holder == null),
                    isRepeating = isRepeating,
                    usages = if (!argNames.isEmpty())
                        optionNames.map { "$it ${argNames.joinToString(" ")}" }
                    else optionNames,
                    isPositional = false,
                    help = help)
        }

        override fun registerLeaf(root: Delegate<*>) {
            for (name in optionNames) {
                parser.registerOption(name, this)
            }
        }
    }

    private class PositionalDelegate<T>(
            parser: ArgParser,
            argName: String,
            val sizeRange: IntRange,
            help: String,
            val transform: String.() -> T) : ParsingDelegate<List<T>>(parser, argName, help) {

        init {
            if (!ARG_NAME_RE.matches(argName)) {
                throw IllegalArgumentException("$argName is not a valid argument name")
            }
        }

        override fun registerLeaf(root: Delegate<*>) {
            assert(holder == null)
            val hasDefault = root.hasValue
            if (hasDefault && sizeRange.first != 1) {
                throw IllegalStateException("default value can only be applied to positional that requires a minimum of 1 arguments")
            }
            // TODO: this feels like a bit of a kludge. Consider making .default only work on positional and not
            // postionalList by having them return different types?
            parser.positionalDelegates.add(this to hasDefault)
        }

        fun parseArguments(args: List<String>) {
            holder = Holder(args.map(transform))
        }

        override fun toHelpFormatterValue(): HelpFormatter.Value {
            return HelpFormatter.Value(
                    isRequired = sizeRange.first > 0,
                    isRepeating = sizeRange.last > 1,
                    usages = listOf(errorName),
                    isPositional = true,
                    help = help)
        }
    }

    /**
     * @property value a Holder containing the current value associated with this option, or null if unset
     * @property optionName the name used for this option in this invocation
     * @property arguments the arguments supplied for this option
     */
    data class OptionInvocation<T> internal constructor(
            // Internal constructor so future versions can add properties
            // without breaking compatibility.
            val value: Holder<T>?,
            val optionName: String,
            val arguments: List<String>)

    private val shortOptionDelegates = mutableMapOf<Char, OptionDelegate<*>>()
    private val longOptionDelegates = mutableMapOf<String, OptionDelegate<*>>()
    private val positionalDelegates = mutableListOf<Pair<PositionalDelegate<*>, Boolean>>()
    internal val delegates = LinkedHashSet<Delegate<*>>()

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

    /**
     * Ensures that arguments have been parsed and validated.
     *
     * @throws SystemExitException if parsing or validation failed.
     */
    fun force() {
        if (!finished) {
            parseOptions
            if (!inValidation) {
                inValidation = true
                try {
                    for (delegate in delegates) delegate.checkHasValue()
                    for (delegate in delegates) delegate.validate()
                } finally {
                    inValidation = false
                }
            }
        }
        finished = true
    }

    private var parseStarted = false

    internal fun checkNotParsed() {
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
        var extra = (remaining - positionalDelegates.map {
            if (it.second) 0 else it.first.sizeRange.first
        }.sum()).coerceAtLeast(0)
        for ((delegate, hasDefault) in positionalDelegates) {
            val minSize = if (hasDefault) 0 else delegate.sizeRange.first
            val sizeRange = delegate.sizeRange
            val chunkSize = (minSize + extra).coerceAtMost(sizeRange.endInclusive)
            if (chunkSize > remaining) {
                throw MissingRequiredPositionalArgumentException(delegate.errorName)
            }
            if (chunkSize != 0 || !hasDefault) {
                delegate.parseArguments(args.subList(index, index + chunkSize))
            }
            lastValueName = delegate.errorName
            index += chunkSize
            remaining -= chunkSize
            extra -= chunkSize - minSize
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
            // if NAME_EQUALS_VALUE_REGEX then there must be groups 1 and 2
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

        internal fun selectRepresentativeOptionName(names: Array<out String>): String {
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

        internal fun optionNamesToErrorName(names: Array<out String>): String {
            return optionNameToArgName(selectRepresentativeOptionName(names))
        }

        private fun optionNameToArgName(name: String) =
                LEADING_HYPHENS.replace(name, "").toUpperCase().replace('-', '_')

    }

    init {
        if (helpFormatter != null) {
            option<Unit>("-h", "--help",
                    errorName = "HELP", // This should never be used, but we need to say something
                    help = "show this help message and exit") {
                throw ShowHelpException(helpFormatter, delegates.toList())
            }.default(Unit).registerRoot()
        }
    }
}

private const val OPTION_CHAR_CLASS = "[a-zA-Z0-9]"
private val OPTION_NAME_RE = Regex("^(-$OPTION_CHAR_CLASS)|(--$OPTION_CHAR_CLASS+([-_]$OPTION_CHAR_CLASS+)*)$")
private const val ARG_INITIAL_CHAR_CLASS = "[A-Z]"
private const val ARG_CHAR_CLASS = "[A-Z0-9]"
private val ARG_NAME_RE = Regex("^$ARG_INITIAL_CHAR_CLASS+([-_]$ARG_CHAR_CLASS+)*$")

fun <T> ArgParser.DelegateProvider<T>.default(newDefault: T): ArgParser.DelegateProvider<T> {
    return ArgParser.DelegateProvider(ctor = ctor, defaultHolder = Holder(newDefault))
}

fun <T> ArgParser.Delegate<T>.default(defaultValue: T): ArgParser.Delegate<T> {
    val inner = this

    return object : ArgParser.Delegate<T>() {
        override fun toHelpFormatterValue(): HelpFormatter.Value = inner.toHelpFormatterValue().copy(isRequired = false)

        override fun validate() {
            inner.validate()
        }

        override val parser: ArgParser
            get() = inner.parser

        override val value: T
            get() {
                inner.parser.force()
                return if (inner.hasValue) inner.value else defaultValue
            }

        override val hasValue: Boolean
            get() = true

        override val errorName: String
            get() = inner.errorName

        override val help: String
            get() = inner.help

        override fun addValidator(validator: ArgParser.Delegate<T>.() -> Unit): ArgParser.Delegate<T> =
                inner.addValidator() { validator(inner) }

        override fun registerLeaf(root: ArgParser.Delegate<*>) {
            inner.registerLeaf(root)
        }
    }
}

/**
 * Formats help for an [ArgParser].
 */
interface HelpFormatter {
    /**
     * Formats a help message.
     *
     * @param progName name of the program as it should appear in usage information, or null if
     * program name is unknown.
     * @param columns width of display help should be formatted for, measured in character cells.
     * @param values [Value] objects describing the arguments types available.
     */
    fun format(progName: String?, columns: Int, values: List<Value>): String

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
            val help: String)
}

/**
 * Default implementation of [HelpFormatter]. Output is modelled after that of common UNIX utilities and looks
 * something like this:
 *
 * ```
 * usage: program_name     [-h] [-n] [-I INCLUDE]... -o OUTPUT
 *                         [-v]... SOURCE... DEST
 *
 * Does something really useful.
 *
 * required arguments:
 *   -o OUTPUT,            directory in which all output should
 *   --output OUTPUT       be generated
 *
 * optional arguments:
 *   -h, --help            show this help message and exit
 *
 *   -n, --dry-run         don't do anything
 *
 *   -I INCLUDE,           search in this directory for header
 *   --include INCLUDE     files
 *
 *  -v, --verbose         increase verbosity
 *
 * positional arguments:
 *   SOURCE                source file
 *
 *   DEST                  destination file
 *
 * More info is available at http://program-name.example.com/
 * ```
 *
 * @property prologue Text that should appear near the beginning of the help, immediately after the usage summary.
 * @property epilogue Text that should appear at the end of the help.
 */
class DefaultHelpFormatter(
        val prologue: String? = null,
        val epilogue: String? = null
) : HelpFormatter {
    val indent = "  "
    val indentWidth = indent.codePointWidth()

    override fun format(
            progName: String?,
            columns: Int,
            values: List<HelpFormatter.Value>
    ): String {
        val sb = StringBuilder()
        appendUsage(sb, columns, progName, values)
        sb.append("\n")

        if (!prologue.isNullOrEmpty()) {
            sb.append("\n")
            // we just checked that prologue is non-null
            sb.append(prologue!!.wrapText(columns))
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
        // Make left column as narrow as possible without wrapping any of the individual usages, though no wider than
        // half the screen.
        val usageColumns = (2 * indentWidth - 1 + (
                values.map { usageText(it).split(" ").map { it.length }.max() ?: 0 }.max() ?: 0)
                ).coerceAtMost(columns / 2)
        appendSection(sb, usageColumns, columns, "required", required)
        appendSection(sb, usageColumns, columns, "optional", optional)
        appendSection(sb, usageColumns, columns, "positional", positional)

        if (!epilogue.isNullOrEmpty()) {
            sb.append("\n")
            // we just checked that epilogue is non-null
            sb.append(epilogue!!.wrapText(columns))
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun appendSection(
            sb: StringBuilder,
            usageColumns: Int,
            columns: Int,
            name: String,
            values: List<HelpFormatter.Value>
    ) {

        if (!values.isEmpty()) {
            sb.append("\n")
            sb.append("$name arguments:\n")
            for (value in values) {
                val left = usageText(value).wrapText(usageColumns - indentWidth).prependIndent(indent)
                val right = value.help.wrapText(columns - usageColumns - 2 * indentWidth).prependIndent(indent)
                sb.append(columnize(left, right, minWidths = intArrayOf(usageColumns)))
                sb.append("\n\n")
            }
        }
    }

    private fun usageText(value: HelpFormatter.Value) =
            value.usages.map { it.replace(' ', '\u00a0') }.joinToString(", ")

    private fun appendUsage(sb: StringBuilder, columns: Int, progName: String?, values: List<HelpFormatter.Value>) {
        val usageStart = "usage:${if (progName != null) " $progName" else ""} "

        val valueSB = StringBuilder()
        for (value in values) value.run {
            if (!usages.isEmpty()) {
                val usage = usages[0].replace(' ', NBSP_CODEPOINT.toChar())
                if (isRequired) {
                    valueSB.append(" $usage")
                } else {
                    valueSB.append(" [$usage]")
                }
                if (isRepeating) {
                    valueSB.append("...")
                }
            }
        }

        if (usageStart.length > columns / 2) {
            sb.append(usageStart)
            sb.append("\n")
            val valueIndent = 8 // TODO don't hardcode this
            val valueColumns = columns - valueIndent
            sb.append(valueSB.toString().wrapText(valueColumns).prependIndent(" ".repeat(valueIndent)))
        } else {
            val valueColumns = columns - usageStart.length
            sb.append(columnize(usageStart, valueSB.toString().wrapText(valueColumns)))
        }
    }
}

/**
 * Indicates that the user requested that help should be shown (with the
 * `--help` option, for example).
 */
class ShowHelpException internal constructor(
        private val helpFormatter: HelpFormatter,
        private val delegates: List<ArgParser.Delegate<*>>
) : SystemExitException("Help was requested", 0) {
    override fun printUserMessage(writer: Writer, progName: String?, columns: Int) {
        writer.write(helpFormatter.format(progName, columns, delegates.map { it.toHelpFormatterValue() }))
    }
}

/**
 * Indicates that an unrecognized option was supplied.
 *
 * @property optName the name of the option
 */
open class UnrecognizedOptionException(val optName: String) :
        SystemExitException("unrecognized option '$optName'", 2)

/**
 * Indicates that a value is missing after parsing has completed.
 *
 * @property valueName the name of the missing value
 */
open class MissingValueException(val valueName: String) :
        SystemExitException("missing $valueName", 2)

/**
 * Indicates that the value of a supplied argument is invalid.
 */
open class InvalidArgumentException(message: String) : SystemExitException(message, 2)

/**
 * Indicates that a required option argument was not supplied.
 *
 * @property optName the name of the option
 * @property argName the name of the missing argument, or null
 */
open class OptionMissingRequiredArgumentException(val optName: String, val argName: String? = null) :
        SystemExitException("option '$optName' is missing "
                + (if (argName == null) "a required argument" else "the required argument $argName"),
                2)

/**
 * Indicates that a required positional argument was not supplied.
 *
 * @property argName the name of the positional argument
 */
open class MissingRequiredPositionalArgumentException(val argName: String) :
        SystemExitException("missing $argName operand", 2)

/**
 * Indicates that an argument was forced upon an option that does not take one.
 *
 * For example, if the arguments contained "--foo=bar" and the "--foo" option does not consume any arguments.
 *
 * @property optName the name of the option
 */
open class UnexpectedOptionArgumentException(val optName: String) :
        SystemExitException("option '$optName' doesn't allow an argument", 2)

/**
 * Indicates that there is an unhandled positional argument.
 *
 * @property valueName the name of the missing value
 */
open class UnexpectedPositionalArgumentException(val valueName: String?) :
        SystemExitException("unexpected argument${if (valueName == null) "" else " after $valueName"}", 2)

// TODO: remove addValidator completely?
// TODO: add validation to option names, option arg names, and positional arg names
