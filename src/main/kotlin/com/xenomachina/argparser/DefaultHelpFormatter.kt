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

import com.xenomachina.text.NBSP_CODEPOINT
import com.xenomachina.text.term.codePointWidth
import com.xenomachina.text.term.columnize
import com.xenomachina.text.term.wrapText

/**
 * Default implementation of [HelpFormatter]. Output is modelled after that of common UNIX utilities and looks
 * something like this:
 *
 * ```
 * usage: program_name     [-h] [-n] [-I INCLUDE]... -o OUTPUT
 *                         [-v]... SOURCE... DEST
 *
 * Does something really useful.
 *
 * required arguments:
 *   -o OUTPUT,            directory in which all output should
 *   --output OUTPUT       be generated
 *
 * optional arguments:
 *   -h, --help            show this help message and exit
 *
 *   -n, --dry-run         don't do anything
 *
 *   -I INCLUDE,           search in this directory for header
 *   --include INCLUDE     files
 *
 *  -v, --verbose         increase verbosity
 *
 * positional arguments:
 *   SOURCE                source file
 *
 *   DEST                  destination file
 *
 * More info is available at http://program-name.example.com/
 * ```
 *
 * @property prologue Text that should appear near the beginning of the help, immediately after the usage summary.
 * @property epilogue Text that should appear at the end of the help.
 */
class DefaultHelpFormatter(
        val prologue: String? = null,
        val epilogue: String? = null
) : HelpFormatter {
    val indent = "  "
    val indentWidth = indent.codePointWidth()

    override fun format(
            progName: String?,
            columns: Int,
            values: List<HelpFormatter.Value>
    ): String {
        val effectiveColumns = when {
            columns < 0 -> throw IllegalArgumentException("columns must be non-negative")
            columns == 0 -> Int.MAX_VALUE
            else -> columns
        }
        val sb = StringBuilder()
        appendUsage(sb, effectiveColumns, progName, values)
        sb.append("\n")

        if (!prologue.isNullOrEmpty()) {
            sb.append("\n")
            // we just checked that prologue is non-null
            sb.append(prologue!!.wrapText(effectiveColumns))
            sb.append("\n")
        }

        val required = mutableListOf<HelpFormatter.Value>()
        val optional = mutableListOf<HelpFormatter.Value>()
        val positional = mutableListOf<HelpFormatter.Value>()

        for (value in values) {
            when {
                value.isPositional -> positional
                value.isRequired -> required
                else -> optional
            }.add(value)
        }
        val usageColumns = 2 * indentWidth - 1 + if (columns == 0) {
            values.map { usageText(it).length }.max() ?: 0
        } else {
            // Make left column as narrow as possible without wrapping any of the individual usages, though no wider than
            // half the screen.
                    values.map { usageText(it).split(" ").map { it.length }.max() ?: 0 }.max() ?: 0
                    .coerceAtMost(effectiveColumns / 2)
        }

        appendSection(sb, usageColumns, effectiveColumns, "required", required)
        appendSection(sb, usageColumns, effectiveColumns, "optional", optional)
        appendSection(sb, usageColumns, effectiveColumns, "positional", positional)

        if (!epilogue?.trim().isNullOrEmpty()) {
            sb.append("\n")
            // we just checked that epilogue is non-null
            sb.append(epilogue!!.trim().wrapText(effectiveColumns))
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun appendSection(
            sb: StringBuilder,
            usageColumns: Int,
            columns: Int,
            name: String,
            values: List<HelpFormatter.Value>
    ) {

        if (!values.isEmpty()) {
            sb.append("\n")
            sb.append("$name arguments:\n")
            for (value in values) {
                val left = usageText(value).wrapText(usageColumns - indentWidth).prependIndent(indent)
                val right = value.help.wrapText(columns - usageColumns - 2 * indentWidth).prependIndent(indent)
                sb.append(columnize(left, right, minWidths = intArrayOf(usageColumns)))
                sb.append("\n\n")
            }
        }
    }

    private fun usageText(value: HelpFormatter.Value) =
            value.usages.map { it.replace(' ', '\u00a0') }.joinToString(", ")

    private fun appendUsage(sb: StringBuilder, columns: Int, progName: String?, values: List<HelpFormatter.Value>) {
        var usageStart = USAGE_PREFIX + (if (progName != null) " $progName" else "")

        val valueSB = StringBuilder()
        for (value in values) value.run {
            if (!usages.isEmpty()) {
                val usage = usages[0].replace(' ', NBSP_CODEPOINT.toChar())
                if (isRequired) {
                    valueSB.append(" $usage")
                } else {
                    valueSB.append(" [$usage]")
                }
                if (isRepeating) {
                    valueSB.append("...")
                }
            }
        }

        if (usageStart.length > columns / 2) {
            sb.append(usageStart)
            sb.append("\n")
            val valueIndent = (USAGE_PREFIX + " " + indent).codePointWidth()
            val valueColumns = columns - valueIndent
            sb.append(valueSB.toString().wrapText(valueColumns).prependIndent(" ".repeat(valueIndent)))
        } else {
            usageStart += " "
            val valueColumns = columns - usageStart.length
            sb.append(columnize(usageStart, valueSB.toString().wrapText(valueColumns)))
        }
    }
}

private const val USAGE_PREFIX = "usage:"
