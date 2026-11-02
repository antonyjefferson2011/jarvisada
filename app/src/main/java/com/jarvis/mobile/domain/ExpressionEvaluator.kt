package com.jarvis.mobile.domain

class ExpressionEvaluator {
    fun evaluate(source: String): Double {
        val normalized = source
            .lowercase()
            .replace("vírgula", ".")
            .replace(',', '.')
            .replace("vezes", "*")
            .replace("×", "*")
            .replace("multiplicado por", "*")
            .replace("dividido por", "/")
            .replace("÷", "/")
            .replace("mais", "+")
            .replace("menos", "-")
            .replace(Regex("\\bx\\b"), "*")
            .replace(Regex("\\bpor\\b"), "/")
            .replace(Regex("\\s+"), "")

        require(normalized.isNotBlank()) { "Digite uma expressão." }
        require(normalized.all { it.isDigit() || it in ".()+-*/" }) {
            "A expressão contém algo que a calculadora não reconhece."
        }
        return Parser(normalized).parse()
    }

    private class Parser(private val source: String) {
        private var index = 0

        fun parse(): Double {
            val result = parseExpression()
            require(index == source.length) { "Confira a expressão e tente novamente." }
            require(result.isFinite()) { "O resultado não é um número válido." }
            return result
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                value = when {
                    consume('+') -> value + parseTerm()
                    consume('-') -> value - parseTerm()
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                value = when {
                    consume('*') -> value * parseFactor()
                    consume('/') -> {
                        val divisor = parseFactor()
                        require(divisor != 0.0) { "Não é possível dividir por zero." }
                        value / divisor
                    }
                    else -> return value
                }
            }
        }

        private fun parseFactor(): Double {
            if (consume('+')) return parseFactor()
            if (consume('-')) return -parseFactor()
            if (consume('(')) {
                val value = parseExpression()
                require(consume(')')) { "Falta fechar um parêntese." }
                return value
            }
            val start = index
            var decimalSeen = false
            while (index < source.length) {
                val char = source[index]
                if (char.isDigit()) {
                    index++
                } else if (char == '.' && !decimalSeen) {
                    decimalSeen = true
                    index++
                } else {
                    break
                }
            }
            require(index > start && source.substring(start, index) != ".") {
                "Esperava um número."
            }
            return source.substring(start, index).toDouble()
        }

        private fun consume(char: Char): Boolean {
            if (index < source.length && source[index] == char) {
                index++
                return true
            }
            return false
        }
    }
}
