package com.jarvis.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExpressionEvaluatorTest {
    private val evaluator = ExpressionEvaluator()

    @Test
    fun respectsOperatorPrecedence() {
        assertEquals(14.0, evaluator.evaluate("2 + 3 * 4"), 0.000001)
    }

    @Test
    fun evaluatesParenthesesAndPortugueseOperators() {
        assertEquals(20.0, evaluator.evaluate("(2 mais 3) vezes 4"), 0.000001)
    }

    @Test
    fun acceptsSpokenMultiplicationSymbol() {
        assertEquals(126.0, evaluator.evaluate("18 × 7"), 0.000001)
    }

    @Test
    fun rejectsDivisionByZero() {
        assertThrows(IllegalArgumentException::class.java) {
            evaluator.evaluate("5 / 0")
        }
    }

    @Test
    fun rejectsNonMathematicalInput() {
        assertThrows(IllegalArgumentException::class.java) {
            evaluator.evaluate("abrir câmera")
        }
    }
}
