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
 * Defines rules for POSIX-style argument and option naming.
 */
internal object PosixNaming {
    fun identifierToOptionName(identifier: String): String {
        return when (identifier.length) {
            1 -> "-" + identifier
            else -> "--" + identifier.camelCaseToUnderscored()
        }
    }

    fun String.camelCaseToUnderscored(): String {
        return replace('_', '-')
                .replace(Regex("(\\p{javaLowerCase})(\\p{javaUpperCase})")) { m ->
                    m.groups[1]!!.value + "-" + m.groups[2]!!.value.toLowerCase()
                }
    }

    fun identifierToArgName(identifier: String): String {
        return identifier.camelCaseToUnderscored().toUpperCase()
    }

    fun selectRepresentativeOptionName(names: Array<out String>): String {
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

    fun optionNameToArgName(name: String) =
            LEADING_HYPHENS_REGEX.replace(name, "").toUpperCase().replace('-', '_')
}
