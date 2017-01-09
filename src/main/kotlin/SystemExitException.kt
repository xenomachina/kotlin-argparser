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

import java.io.PrintStream
import kotlin.system.exitProcess

/**
 * An exception that wants the process to terminate with a specific status code, and also (optionally) wants to display
 * a message to [System.out] or [System.err].
 */
open class SystemExitException(message: String, val returnCode: Int) : Exception(message) {
    /**
     * Prints a message for the user to either `System.err` or `System.out`, and then exits with the appropriate
     * return code.
     *
     * @param progName the name of this program as invoked, or null if not known
     * @param columns the number of columns to wrap at, or 0 if not to wrap at all
     */
    fun printAndExit(progName: String? = null, columns: Int = 0): Nothing {
        val stream = if (returnCode == 0) System.out else System.err
        printUserMessage(stream, progName, columns)
        exitProcess(returnCode)
    }

    /**
     * Prints a message for the user to the specified `PrintStream`.
     *
     * @param progName the name of this program as invoked, or null if not known
     * @param columns the number of columns to wrap at, or 0 if not to wrap at all
     */
    open fun printUserMessage(stream: PrintStream, progName: String?, columns: Int) {
        val leader = if (progName == null) "" else "$progName: "
        stream.println("$leader$message")
    }
}

/**
 * Indicates that an unrecognized option was supplied.
 */
open class UnrecognizedOptionException(val optionName: String) :
        SystemExitException("unrecognized option '$optionName'", 2)

/**
 * Indicates that a value is missing after parsing has completed.
 */
open class MissingValueException(val valueName: String) :
        SystemExitException("missing $valueName", 2)

/**
 * Indicates that the value of a supplied argument is invalid.
 */
open class InvalidArgumentException(message: String) : SystemExitException(message, 2)

/**
 * Indicates that a required option argument was not supplied.
 */
open class OptionMissingRequiredArgumentException(val optName: String) :
        SystemExitException("option '$optName' is missing a required argument", 2)

/**
 * Indicates that a required positional argument was not supplied.
 */
open class MissingRequiredPositionalArgumentException(val valueName: String) :
        SystemExitException("missing $valueName operand", 2)

/**
 * Indicates that an argument was forced upon an option that does not take one.
 *
 * That is, "--foo=bar" where "--foo" takes no arguments.
 */
open class UnexpectedOptionArgumentException(val optName: String) :
        SystemExitException("option '$optName' doesn't allow an argument", 2)

/**
 * Indicates that there is an unhandled positional argument.
 */
open class UnexpectedPositionalArgumentException(val valueName: String?) :
        SystemExitException("unexpected argument${if (valueName == null) "" else " after $valueName"}", 2)

/**
 * Like [kotlin.run], but calls [SystemExitException.printAndExit] on any
 * `SystemExitException` that is caught.
 *
 * @param progName the name of the program, or null if not known
 * @param columns the number of columns to wrap any caught
 * `SystemExitException` to.  Specify null for reasonable defaults, or 0 to not
 * wrap at all.
 * @param f the main body. If a `SystemExitException` is caught its
 * `printAndExit` method will be invoked.
 */
fun <T, R> T.runMain(progName: String? = null, columns: Int?, f: T.() -> R): R {
    try {
        return f()
    } catch (e: SystemExitException) {
        e.printAndExit(progName, columns ?: System.getenv("COLUMNS")?.toInt() ?: 80)
    }
}
