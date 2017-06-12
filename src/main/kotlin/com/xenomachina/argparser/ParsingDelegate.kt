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

internal abstract class ParsingDelegate<T>(
        override val parser: ArgParser,
        override val errorName: String,
        override val help: String) : ArgParser.Delegate<T>() {

    protected var holder: Holder<T>? = null

    override fun addValidator(validator: ArgParser.Delegate<T>.() -> Unit): ArgParser.Delegate<T> = apply {
        validators.add(validator)
    }

    override val hasValidators: Boolean
        get() = validators.isNotEmpty()

    override val value: T
        get() {
            parser.force()
            // checkHasValue should have ensured that this is non-null
            return holder!!.value
        }

    override val hasValue: Boolean
        get() = holder != null

    override fun validate() {
        for (validator in validators) validator()
    }

    private val validators = mutableListOf<ArgParser.Delegate<T>.() -> Unit>()
}
