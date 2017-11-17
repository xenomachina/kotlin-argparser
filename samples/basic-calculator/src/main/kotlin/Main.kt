import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.HelpFormatter
import com.xenomachina.argparser.MissingValueException
import com.xenomachina.argparser.ShowHelpException
import com.xenomachina.argparser.UnrecognizedOptionException

enum class Operation {
    SUM, MUL;
}

object Calculator {

    fun calculate(option: Operation, numbers: List<Int>) = when (option) {
        Operation.SUM -> numbers.fold(0, { x, y -> x + y })
        Operation.MUL -> numbers.fold(1, { x, y -> x * y })
    }
}

object CalculatorCLIHelper : HelpFormatter {

    override fun format(progName: String?, columns: Int, values: List<HelpFormatter.Value>): String {

        val headline = "$progName help command"

        val description = "$progName is a basic calculator that operates via Command Line Interface (CLI)"

        val commandsFormatted = values.map {
            val required = when (it.isRequired) {
                true -> "(required)"
                else -> "(optional)"
            }

            return "${it.usages} $required \n"
        }

        val commands = """
            commands:

            $commandsFormatted
        """.trimIndent()

        return """
            $headline

            $description

            $commands
        """.trimIndent()
    }
}

class CalculatorCLI(parser: ArgParser) {

    val operation: Operation by parser.mapping(
            "--sum" to Operation.SUM,
            "--mul" to Operation.MUL,
            help = "The operation to calculate")

    val numbers: List<Int> by parser.adding(
            "-n",
            help = "A number to calculate") { toInt() }

    val enablePrettyPrint: Boolean by parser.flagging(
            "-p", "--pretty",
            help = "Display operation and numbers along with the result")
}

fun main(args: Array<String>) {
    try {
        val cli = CalculatorCLI(ArgParser(
                args = args,
                mode = ArgParser.Mode.GNU,
                helpFormatter = CalculatorCLIHelper))

        val operation = cli.operation
        val numbers = cli.numbers
        val enablePrettyPrint = cli.enablePrettyPrint

        val result = Calculator.calculate(operation, numbers)

        when (enablePrettyPrint) {
            true -> println("The $operation of $numbers is equal to $result")
            else -> println(result)
        }
    } catch (ex: MissingValueException) {
        println("Missing operation parameter. Options: ${ex.valueName}")
    } catch (ex: UnrecognizedOptionException) {
        println("Invalid parameter: ${ex.optName}")
    } catch (ex: NumberFormatException) {
        println("Invalid number: ${ex.message!!.replaceFirst("For input string: ", "")}")
    } catch (ex: ShowHelpException) {
        ex.printUserMessage(System.out.writer(), "basic-calculator", 0)
    }
}
