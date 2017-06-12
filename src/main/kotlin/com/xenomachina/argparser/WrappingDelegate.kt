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

internal class WrappingDelegate<U, W>(
        private val inner: ArgParser.Delegate<U>,
        private val wrap: (U) -> W
) : ArgParser.Delegate<W>() {

    override val parser: ArgParser
        get() = inner.parser

    override val value: W
        get() = wrap(inner.value)

    override val hasValue: Boolean
        get() = inner.hasValue

    override val errorName: String
        get() = inner.errorName

    override val help: String
        get() = inner.help

    override fun validate() {
        inner.validate()
    }

    override fun toHelpFormatterValue(): HelpFormatter.Value = inner.toHelpFormatterValue()

    override fun addValidator(validator: ArgParser.Delegate<W>.() -> Unit): ArgParser.Delegate<W> =
            apply { inner.addValidator { validator(this@WrappingDelegate) } }

    override val hasValidators: Boolean
        get() = inner.hasValidators

    override fun registerLeaf(root: ArgParser.Delegate<*>) {
        inner.registerLeaf(root)
    }
}
