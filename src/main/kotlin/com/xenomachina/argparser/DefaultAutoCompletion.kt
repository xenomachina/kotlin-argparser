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

class DefaultAutoCompletion : AutoCompletion {
    override fun format(progName: String?, delegates: List<ArgParser.Delegate<*>>): String {
        val sb = StringBuilder()
        sb.append("_$progName()\n")
                .append("{\n")
                .append("\tlocal cur prev opts\n")
                .append("\tCOMPREPLY=()\n")
                .append("\tcur=\"\${COMP_WORDS[COMP_CWORD]}\"\n")
                .append("\tprev=\"\${COMP_WORDS[COMP_CWORD - 1]}\"\n")
                .append("\topts=\"")

        delegates.forEach {
            it.toAutoCompletion().forEach {
                sb.append("$it ")
            }
        }

        sb.append("\"\n")
                .append("\n")
                .append("\tif [[ \${cur} == -* ]] ; then\n")
                .append("\t\tCOMPREPLY=( \$(compgen -W \"\${opts}\" -- \${cur}) )\n")
                .append("\t\treturn 0\n")
                .append("\tfi\n")
                .append("}\n")
                .append("complete -F _$progName $progName")

        return sb.toString()
    }
}