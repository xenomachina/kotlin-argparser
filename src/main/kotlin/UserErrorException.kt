// Copyright © 2016 Laurence Gonsalves
//
// This file is part of kotlin-optionparser, a library which can be found at
// http://github.com/xenomachina/kotlin-optionparser
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

package com.xenomachina.optionparser

import org.apache.commons.lang3.StringEscapeUtils
import kotlin.system.exitProcess

open class UserErrorException(val progName: String, message: String, val returnCode: Int) : java.lang.Exception(message) {
    fun printAndExit(): Nothing {
        System.err.println("$progName: $message")
        exitProcess(returnCode)
    }
}

/**
 * Indicates that an unrecognized option was supplied.
 */
class UnrecognizedOptionException(progName: String, val optionName: String) :
        UserErrorException(progName, "unrecognized option '$optionName'", 2)

/**
 * Indicates that a required argument (that is, one with no default value) was not supplied.
 */
class MissingArgumentException(progName: String, val argName: String) :
        UserErrorException(progName, "missing $argName", 2)

/**
 * Indicates that the value of a supplied argument is invalid.
 */
class InvalidArgumentException(progName: String, val argName: String, val argValue: String) :
        UserErrorException(progName, "invalid $argName: ‘${StringEscapeUtils.escapeJava(argValue)}’", 2)
