// Copyright © 2018 Tobias Berger
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

import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class DefaultTest : FunSpec({
    test("Single call on default lambda") {
        val parser = parserOf()

        var callCount = 0
        val testDelegate by parser.storing("-t") { toInt() }
                .default { ++callCount }

        callCount shouldBe 0
        testDelegate shouldBe 1
        callCount shouldBe 1
        testDelegate shouldBe 1
        callCount shouldBe 1
    }
})
