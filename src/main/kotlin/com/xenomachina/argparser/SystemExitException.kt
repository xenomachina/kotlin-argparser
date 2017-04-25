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

import java.io.OutputStreamWriter
import java.io.Writer
import kotlin.system.exitProcess

/**
 * An exception that wants the process to terminate with a specific status code, and also (optionally) wants to display
 * a message to [System.out] or [System.err].
 *
 * @property returnCode the return code that this process should exit with
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
        val writer = OutputStreamWriter(if (returnCode == 0) System.out else System.err)
        printUserMessage(writer, progName, columns)
        writer.flush()
        exitProcess(returnCode)
    }

    /**
     * Prints a message for the user to the specified `Writer`.
     *
     * @param writer where to write message for the user
     * @param progName the name of this program as invoked, or null if not known
     * @param columns the number of columns to wrap at, or 0 if not to wrap at all
     */
    open fun printUserMessage(writer: Writer, progName: String?, columns: Int) {
        val leader = if (progName == null) "" else "$progName: "
        writer.write("$leader$message\n")
    }
}

/**
 * Calls [SystemExitException.printAndExit] on any `SystemExitException` that
 * is caught.
 *
 * @param progName the name of the program, or null if not known
 * @param columns the number of columns to wrap any caught
 * `SystemExitException` to.  Specify null for reasonable defaults, or 0 to not
 * wrap at all.
 * @param body the code that may throw a `SystemExitException`
 */
fun <R> mainBody(progName: String? = null, columns: Int? = null, body: () -> R): R {
    try {
        return body()
    } catch (e: SystemExitException) {
        e.printAndExit(progName, columns ?: System.getenv("COLUMNS")?.toInt() ?: 80)
    }
}
