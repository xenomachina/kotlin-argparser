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

/**
 * Formats help for an [ArgParser].
 */
interface HelpFormatter {
    /**
     * Formats a help message.
     *
     * @param progName name of the program as it should appear in usage information, or null if
     * program name is unknown.
     * @param columns width of display help should be formatted for, measured in character cells, or 0 for infinite
     * width.
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
