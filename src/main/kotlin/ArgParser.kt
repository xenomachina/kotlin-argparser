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

import java.io.Writer
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
                    errorName = errorNameForOptionNames(names),
                    usageArgument = null,
                    isRepeating = false,
                    help = help) { true }.default(false)

    /**
     * Creates a Delegate for a zero-argument option that returns the count of how many times the option appears in args.
     */
    fun counting(vararg names: String, help: String): Delegate<Int> =
            option<Int>(
                    *names,
                    errorName = errorNameForOptionNames(names),
                    usageArgument = null,
                    isRepeating = true,
                    help = help) { value.orElse { 0 } + 1 }.default(0)

    /**
     * Creates a Delegate for a single-argument option that stores and returns the option's (transformed) argument.
     */
    fun <T> storing(
            vararg names: String,
            help: String,
            transform: String.() -> T
    ): Delegate<T> {
        val errorName = errorNameForOptionNames(names)
        return option(
                *names,
                errorName = errorName,
                usageArgument = errorName,
                isRepeating = false,
                help = help) { transform(this.next()) }
    }

    /**
     * Creates a Delegate for a single-argument option that stores and returns the option's argument.
     */
    fun storing(vararg names: String, help: String): Delegate<String> =
            storing(*names, help = help) { this }

    /**
     * Creates a Delegate for a single-argument option that adds the option's (transformed) argument to a
     * MutableCollection each time the option appears in args, and returns said MutableCollection.
     */
    fun <E, T : MutableCollection<E>> adding(
            vararg names: String,
            initialValue: T,
            help: String,
            transform: String.() -> E
    ): Delegate<T> {
        val errorName = errorNameForOptionNames(names)
        return option<T>(
                *names,
                errorName = errorName,
                help = help,
                usageArgument = errorName,
                isRepeating = true) {
            // preValidate ensures that this is non-null
            value!!.value.add(transform(next()))
            value.value
        }.default(initialValue)
    }

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
     * Creates a Delegate for a single-argument option that adds the option's argument to a MutableList each time the
     * option appears in args, and returns said MutableCollection.
     */
    fun adding(vararg names: String, help: String): Delegate<MutableList<String>> =
            adding(*names, help = help) { this }

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
                help = help,
                usageArgument = null,
                isRepeating = false) {
            // This cannot be null, because the optionName was added to the map
            // at the same time it was registered with the ArgParser.
            map[optionName]!!
        }
    }

    /**
     * Creates a Delegate for an option with the specified names.
     * @param names names of options, with leading "-" or "--"
     * @param errorName name to use when talking about this option in error messages
     * @param usageArgument how to represent argument(s) in help text, or null if consumes no arguments
     * @param handler A function that assists in parsing arguments by computing the value of this option
     */
    internal fun <T> option(
            vararg names: String,
            errorName: String,
            help: String,
            usageArgument: String?,
            isRepeating: Boolean = true,
            handler: OptionArgumentIterator<T>.() -> T
    ): Delegate<T> {
        val delegate = OptionDelegate<T>(
                parser = this,
                errorName = errorName,
                help = help,
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
    fun positional(name: String, help: String) = positional(name, help = help) { this }

    /**
     * Creates a Delegate for a single positional argument which returns the argument's transformed value.
     */
    fun <T> positional(
            name: String,
            help: String,
            transform: String.() -> T
    ): Delegate<T> {
        return object : WrappingDelegate<List<T>, T>(positionalList(name, 1..1, help = help, transform = transform)) {
            override fun wrap(u: List<T>): T = u[0]

            override fun unwrap(w: T): List<T> = listOf(w)
        }
    }

    /**
     * Creates a Delegate for a sequence of positional arguments which returns a List containing the arguments.
     */
    fun positionalList(
            name: String,
            sizeRange: IntRange = 1..Int.MAX_VALUE,
            help: String
    ) = positionalList(name, sizeRange, help = help) { this }

    /**
     * Creates a Delegate for a sequence of positional arguments which returns a List containing the transformed
     * arguments.
     */
    fun <T> positionalList(
            name: String,
            sizeRange: IntRange = 1..Int.MAX_VALUE,
            help: String,
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

        return PositionalDelegate<T>(this, name, sizeRange, help = help, transform = transform).apply {
            positionalDelegates.add(this)
        }
    }

    internal abstract class WrappingDelegate<U, W>(private val inner: Delegate<U>) : Delegate<W> {

        abstract fun wrap(u: U): W
        abstract fun unwrap(w: W): U

        override val value: W
            get() = wrap(inner.value)

        override val errorName: String
            get() = inner.errorName

        override val help: String
            get() = inner.help

        override fun default(value: W): Delegate<W> =
                apply { inner.default(unwrap(value)) }

        override fun addValidtator(validator: Delegate<W>.() -> Unit): Delegate<W> =
                apply { validator(this) }
    }

    interface Delegate<T> {
        /** The value associated with this delegate */
        val value: T

        /** The name used to refer to this delegate's value in error messages */
        val errorName: String

        /** The user-visible help text for this delegate */
        val help: String

        /** Allows this object to act as a property delegate */
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

        // Fluent setters:

        /** Set default value */
        fun default(value: T): Delegate<T>

        /** Add validation logic. Validator should throw a [SystemExitException] on failure. */
        fun addValidtator(validator: Delegate<T>.() -> Unit): Delegate<T>
    }

    internal abstract class ParsingDelegate<T>(
            val parser: ArgParser,
            override val errorName: String,
            override val help: String) : Delegate<T> {

        protected var holder: Holder<T>? = null

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

        override fun addValidtator(validator: Delegate<T>.() -> Unit): Delegate<T> = apply {
            validators.add(validator)
        }

        override val value: T
            get() {
                parser.force()
                // preValidate ensures that this is non-null
                return holder!!.value
            }

        fun preValidate() {
            if (holder == null)
                throw MissingValueException(errorName)
        }

        fun validate() {
            for (validator in validators) validator()
        }

        abstract fun toHelpFormatterValue(): HelpFormatter.Value

        private val validators = mutableListOf<Delegate<T>.() -> Unit>()
    }

    private class OptionDelegate<T>(
            parser: ArgParser,
            errorName: String,
            help: String,
            val optionNames: List<String>,
            val usageArgument: String?,
            val isRepeating: Boolean,
            val handler: OptionArgumentIterator<T>.() -> T) : ParsingDelegate<T>(parser, errorName, help) {

        fun parseOption(name: String, firstArg: String?, index: Int, args: Array<out String>): Int {
            val input = OptionArgumentIterator(holder, name, firstArg, index, args)
            holder = Holder(handler(input))
            return input.consumed
        }

        override fun toHelpFormatterValue(): HelpFormatter.Value {
            return HelpFormatter.Value(
                    isRequired = (holder == null),
                    isRepeating = isRepeating,
                    usages = if (usageArgument != null)
                        optionNames.map { "$it $usageArgument" }
                    else optionNames,
                    isPositional = false,
                    help = help)
        }
    }

    private class PositionalDelegate<T>(
            parser: ArgParser,
            errorName: String,
            val sizeRange: IntRange,
            help: String,
            val transform: String.() -> T) : ParsingDelegate<List<T>>(parser, errorName, help) {

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
         * Advances to the next argument available to this option and returns it.
         *
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
                throw MissingRequiredPositionalArgumentException(delegate.errorName)
            }
            delegate.parseArguments(args.subList(index, index + chunkSize))
            lastValueName = delegate.errorName
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

        internal fun errorNameForOptionNames(names: Array<out String>): String {
            return optionNameToArgName(selectRepresentativeOptionName(names))
        }

        private fun optionNameToArgName(name: String) =
                LEADING_HYPHENS.replace(name, "").toUpperCase().replace('-', '_')
    }

    init {
        if (helpFormatter != null) {
            option<Unit>("-h", "--help",
                    errorName = "HELP", // This should never be used, but we need to say something
                    help = "show this help message and exit",
                    usageArgument = null,
                    isRepeating = false) {
                throw ShowHelpException(helpFormatter, delegates)
            }.default(Unit)
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
    override fun format(
            progName: String?,
            columns: Int,
            values: List<HelpFormatter.Value>
    ): String {
        val sb = StringBuilder()
        appendUsage(sb, columns, progName, values)

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
        appendSection(sb, columns, "required", required)
        appendSection(sb, columns, "optional", optional)
        appendSection(sb, columns, "positional", positional)

        if (!epilogue.isNullOrEmpty()) {
            sb.append("\n")
            // we just checked that epilogue is non-null
            sb.append(epilogue!!.wrapText(columns))
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun appendSection(sb: StringBuilder, columns: Int, name: String, values: List<HelpFormatter.Value>) {
        // TODO: make these configurable or smarter?
        val helpPos = columns * 3 / 10
        val indent = "  "
        val indentWidth = indent.codePointWidth()

        if (!values.isEmpty()) {
            sb.append("\n")
            sb.append("$name arguments:\n")
            for (value in values) {
                val left = value.usages.map { it.replace(' ', '\u00a0') }.joinToString(", ").wrapText(helpPos - indentWidth).prependIndent(indent)
                val right = value.help.wrapText(columns - helpPos - 2 * indentWidth).prependIndent(indent)
                sb.append(columnize(left, right, minWidths = intArrayOf(helpPos)))
                sb.append("\n")
            }
        }
    }

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
        private val delegates: List<ArgParser.ParsingDelegate<*>>
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
 */
open class OptionMissingRequiredArgumentException(val optName: String) :
        SystemExitException("option '$optName' is missing a required argument", 2)

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

// TODO: move this to com.xenomachina.common

/**
 * a `Holder<T>?` can be used where one needs to be able to distinguish between having a T or not having a T, even
 * when T is a nullable type.
 *
 * @property value the value being held
 */
data class Holder<T>(val value: T)

/**
 * Dereferences the [Holder] if non-null, otherwise returns the result of calling [fallback].
 */
fun <T> Holder<T>?.orElse(fallback: () -> T): T {
    if (this == null) {
        return fallback()
    } else {
        return value
    }
}

// TODO: move all declarations below this point to com.xenomachina.text

/**
 * Performs the given [operation] on each line in this [String].
 */
internal inline fun String.forEachLine(operation: (String) -> Unit) {
    var index = 0
    while (true) {
        val nextNewline = indexOf("\n", index)
        if (nextNewline < 0) break
        operation(substring(index, nextNewline + 1))
        index = nextNewline + 1
    }
    operation(substring(index))
}

/**
 * Performs the given [operation] on each Unicode codepoint in this [String].
 */
internal inline fun String.forEachCodePoint(operation: (Int) -> Unit) {
    val length = this.length
    var offset = 0
    while (offset < length) {
        val codePoint = this.codePointAt(offset)
        operation(codePoint)
        offset += Character.charCount(codePoint)
    }
}

internal fun StringBuilder.clear() {
    this.setLength(0)
}

internal val SPACE_WIDTH = 1

internal fun String.padTo(width: Int): String {
    val sb = StringBuilder()
    var lineWidth = 0
    forEachCodePoint {
        if (it == '\n'.toInt()) {
            while (lineWidth < width) {
                sb.append(" ")
                lineWidth += SPACE_WIDTH
            }
            sb.append("\n")
            lineWidth = 0
        } else {
            sb.appendCodePoint(it)
            lineWidth += codePointWidth(it)
        }
    }
    return sb.toString()
}

internal val NBSP_CODEPOINT = 0xa0

internal fun String.wrapText(maxWidth: Int): String {
    val sb = StringBuilder()
    val word = StringBuilder()
    var lineWidth = 0
    var wordWidth = 0
    fun handleSpace() {
        if (wordWidth > 0) {
            if (lineWidth > 0) {
                sb.append(" ")
                lineWidth += SPACE_WIDTH
            }
            sb.append(word)
            lineWidth += wordWidth
            word.clear()
            wordWidth = 0
        }
    }
    forEachCodePoint {
        if (Character.isSpaceChar(it) && it != NBSP_CODEPOINT) {
            // space
            handleSpace()
        } else {
            // non-space
            val codepoint = if (it == NBSP_CODEPOINT) ' '.toInt() else it
            val charWidth = codePointWidth(codepoint).toInt()
            if (lineWidth > 0 && lineWidth + SPACE_WIDTH + wordWidth + charWidth > maxWidth) {
                sb.append("\n")
                lineWidth = 0
            }
            if (lineWidth == 0 && lineWidth + SPACE_WIDTH + wordWidth + charWidth > maxWidth) {
                // Eep! Word would be longer than line. Need to break it.
                sb.append(word)
                word.clear()
                wordWidth = 0
                sb.append("\n")
                lineWidth = 0
            }
            word.appendCodePoint(codepoint)
            wordWidth += charWidth
        }
    }
    handleSpace()

    return sb.toString()
}

/**
 * Returns an estimated cell width of a Unicode code point when displayed on a monospace terminal.
 * Possible return values are -1, 0, 1 or 2. Control characters (other than null) and Del return -1.
 *
 * This function is based on the public domain [wcwidth.c](https://www.cl.cam.ac.uk/~mgk25/ucs/wcwidth.c)
 * written by Markus Kuhn.
 */
internal fun codePointWidth(ucs: Int): Byte {
    // 8-bit control characters
    if (ucs == 0) return 0
    if (ucs < 32 || (ucs >= 0x7f && ucs < 0xa0)) return -1

    // Non-spacing characters. This is simulating the binary search of
    // `uniset +cat=Me +cat=Mn +cat=Cf -00AD +1160-11FF +200B`.
    if (ucs != 0x00AD) { // soft hyphen
        val category = Character.getType(ucs).toByte()
        if (category == Character.ENCLOSING_MARK || // "Me"
                category == Character.NON_SPACING_MARK || // "Mn"
                category == Character.FORMAT || // "Cf"
                (0x1160 <= ucs && ucs <= 0x11FF) || // Hangul Jungseong & Jongseong
                ucs == 0x200B) // zero width space
            return 0
    }

    // If we arrive here, ucs is not a combining or C0/C1 control character.
    return if (ucs >= 0x1100 && (ucs <= 0x115f || // Hangul Jamo init. consonants
            ucs == 0x2329 || ucs == 0x232a ||
            (ucs >= 0x2e80 && ucs <= 0xa4cf && ucs != 0x303f) || // CJK ... Yi
            (ucs >= 0xac00 && ucs <= 0xd7a3) || // Hangul Syllables
            (ucs >= 0xf900 && ucs <= 0xfaff) || // CJK Compatibility Ideographs
            (ucs >= 0xfe10 && ucs <= 0xfe19) || // Vertical forms
            (ucs >= 0xfe30 && ucs <= 0xfe6f) || // CJK Compatibility Forms
            (ucs >= 0xff00 && ucs <= 0xff60) || // Fullwidth Forms
            (ucs >= 0xffe0 && ucs <= 0xffe6) ||
            (ucs >= 0x20000 && ucs <= 0x2fffd) ||
            (ucs >= 0x30000 && ucs <= 0x3fffd)))
        2 else 1
}

internal fun String.codePointWidth(): Int {
    var result = 0
    forEachCodePoint {
        result += codePointWidth(it)
    }
    return result
}

internal fun String.trimNewline(): String {
    if (endsWith('\n')) {
        return substring(0, length - 1)
    } else {
        return this
    }
}

internal fun columnize(vararg s: String, minWidths: IntArray? = null): String {
    val columns = Array(s.size) { mutableListOf<String>() }
    val widths = Array(s.size) { 0 }
    for (i in 0..s.size - 1) {
        if (minWidths != null && i < minWidths.size) {
            widths[i] = minWidths[i]
        }
        s[i].forEachLine {
            val cell = it.trimNewline()
            columns[i].add(cell)
            widths[i] = widths[i].coerceAtLeast(cell.codePointWidth())
        }
    }
    val height = columns.maxBy { it.size }?.size ?: 0
    val sb = StringBuilder()
    for (j in 0..height - 1) {
        var lineWidth = 0
        var columnStart = 0
        for (i in 0..columns.size - 1) {
            columns[i].getOrNull(j)?.let { cell ->
                for (k in 1..columnStart - lineWidth) sb.append(" ")
                lineWidth = columnStart
                sb.append(cell)
                lineWidth += cell.codePointWidth()
            }
            columnStart += widths[i]
        }
        sb.append("\n")
    }
    return sb.toString()
}
