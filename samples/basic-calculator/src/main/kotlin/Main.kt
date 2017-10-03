import com.xenomachina.argparser.ArgParser

enum class Operation {
    SUM, MUL;
}

object Calculator {

    fun calculate(option: Operation, numbers: List<Int>) = when (option) {
        Operation.SUM -> numbers.reduce { sum, n -> sum.plus(n) }
        Operation.MUL -> numbers.reduce { mul, n -> mul * n }
    }
}

class CalculatorCLI(parser: ArgParser) {

    val op by parser.mapping(
            "--sum" to Operation.SUM,
            "--mul" to Operation.MUL,
            help = "Operation to calculate")

    val numbers by parser.adding("-n", help = "A number to calculate") { toInt() }

    val showResult by parser.flagging("-s", "--show-result", help = "Show the result in the console")

}

fun main(args: Array<String>) {
    val cli = CalculatorCLI(ArgParser(args))

    val operation = cli.op
    val numbers = cli.numbers

    val result = Calculator.calculate(operation, numbers)

    if (cli.showResult) {
        println("The $operation of $numbers is equal to $result")
    }
}
