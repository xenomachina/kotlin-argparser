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

import com.xenomachina.common.Holder

internal class OptionDelegate<T>(
        parser: ArgParser,
        errorName: String,
        help: String,
        val optionNames: List<String>,
        val argNames: List<String>,
        val isRepeating: Boolean,
        val handler: ArgParser.OptionInvocation<T>.() -> T) : ParsingDelegate<T>(parser, errorName, help) {
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
        val input = ArgParser.OptionInvocation(holder, name, arguments)
        holder = Holder(handler(input))
        return argNames.size
    }

    override fun toHelpFormatterValue(): HelpFormatter.Value {
        return HelpFormatter.Value(
                isRequired = (holder == null),
                isRepeating = isRepeating,
                usages = if (!argNames.isEmpty()) optionNames.map { "$it ${argNames.joinToString(" ")}" } else optionNames,
                isPositional = false,
                help = help)
    }

    override fun registerLeaf(root: ArgParser.Delegate<*>) {
        for (name in optionNames) {
            parser.registerOption(name, this)
        }
    }
}
