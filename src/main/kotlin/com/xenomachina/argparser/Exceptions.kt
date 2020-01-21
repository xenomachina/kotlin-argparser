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

/**
 * Indicates that the user requested that help should be shown (with the
 * `--help` option, for example).
 */
class ShowHelpException internal constructor(
    private val helpFormatter: HelpFormatter,
    private val delegates: List<ArgParser.Delegate<*>>
) : SystemExitException("Help was requested", 0) {
    override fun printUserMessage(writer: Writer, programName: String?, columns: Int) {
        writer.write(helpFormatter.format(programName, columns, delegates.map { it.toHelpFormatterValue() }))
    }
}

/**
 * Indicates that the user requested that the version should be shown (with the
 * `--version` option, for example).
 */
class ShowVersionException internal constructor(
    private val version: String
) : SystemExitException("version was requested", 0) {
    override fun printUserMessage(writer: Writer, programName: String?, columns: Int) {
        writer.write(version)
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
        SystemExitException(
            "option '$optName' is missing " + (
                if (argName == null) "a required argument"
                else "the required argument $argName"),
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
