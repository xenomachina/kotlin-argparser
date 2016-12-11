/**
 * Copyright 2016 Laurence Gonsalves
 */

package com.xenomachina.optionparser

import kotlin.system.exitProcess

open class UserErrorException(message: String, val returnCode: Int) : java.lang.Exception(message) {
    fun printAndExit(): Nothing {
        // TODO: include program name?
        System.err.println(message)
        exitProcess(returnCode)
    }
}

class InvalidOptionException(val name: String) :
        UserErrorException("invalid option -- '$name'", 2)
