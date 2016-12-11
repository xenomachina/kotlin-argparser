/**
 * Copyright 2016 Laurence Gonsalves
 */

package com.xenomachina.optionparser

import kotlin.system.exitProcess

open class UserErrorException(val progName: String, message: String, val returnCode: Int) : java.lang.Exception(message) {
    fun printAndExit(): Nothing {
        System.err.println("$progName: $message")
        exitProcess(returnCode)
    }
}

class InvalidOptionException(progName: String, val name: String) :
        UserErrorException(progName, "invalid option -- '$name'", 2)
