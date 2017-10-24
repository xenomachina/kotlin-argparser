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
 * Returns a new `DelegateProvider` with the specified default value.
 *
 * @param newDefault the default value for the resulting [ArgParser.Delegate]
 */
fun <T> ArgParser.DelegateProvider<T>.default(newDefault: T): ArgParser.DelegateProvider<T> {
    return ArgParser.DelegateProvider(ctor = ctor, default = { newDefault })
}

/**
 * Returns a new `DelegateProvider` with the specified default value from a lambda.
 *
 * @param newDefault the default value for the resulting [ArgParser.Delegate]
 */
fun <T> ArgParser.DelegateProvider<T>.default(newDefault: () -> T): ArgParser.DelegateProvider<T> {
    return ArgParser.DelegateProvider(ctor = ctor, default = newDefault)
}

/**
 * Returns a new `Delegate` with the specified default value.
 *
 * @param newDefault the default value for the resulting [ArgParser.Delegate]
 */
fun <T> ArgParser.Delegate<T>.default(defaultValue: T): ArgParser.Delegate<T> = default { defaultValue }

/**
 * Returns a new `Delegate` with the specified default value as a lambda.
 *
 * @param newDefault the default value for the resulting [ArgParser.Delegate]
 */
fun <T> ArgParser.Delegate<T>.default(defaultValue: () -> T): ArgParser.Delegate<T> {
    if (hasValidators) {
        throw IllegalStateException("Cannot add default after adding validators")
    }
    val inner = this

    return object : ArgParser.Delegate<T>() {

        override val hasValidators: Boolean
            get() = inner.hasValidators

        override fun toHelpFormatterValue(): HelpFormatter.Value = inner.toHelpFormatterValue().copy(isRequired = false)

        override fun validate() {
            inner.validate()
        }

        override val parser: ArgParser
            get() = inner.parser

        override val value: T
            get() {
                inner.parser.force()
                return if (inner.hasValue) inner.value else defaultValue()
            }

        override val hasValue: Boolean
            get() = true

        override val errorName: String
            get() = inner.errorName

        override val help: String
            get() = inner.help

        override fun addValidator(validator: ArgParser.Delegate<T>.() -> Unit): ArgParser.Delegate<T> =
            apply { inner.addValidator { validator(this@apply) } }

        override fun registerLeaf(root: ArgParser.Delegate<*>) {
            inner.registerLeaf(root)
        }
    }
}
