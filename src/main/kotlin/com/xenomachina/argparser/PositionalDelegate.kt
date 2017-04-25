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

internal class PositionalDelegate<T>(
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

    override fun registerLeaf(root: ArgParser.Delegate<*>) {
        assert(holder == null)
        val hasDefault = root.hasValue
        if (hasDefault && sizeRange.first != 1) {
            throw IllegalStateException("default value can only be applied to positional that requires a minimum of 1 arguments")
        }
        // TODO: this feels like a bit of a kludge. Consider making .default only work on positional and not
        // postionalList by having them return different types?
        parser.registerPositional(this, hasDefault)
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
