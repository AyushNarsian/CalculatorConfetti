package com.example.calculator

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calculator.databinding.ActivityMainBinding
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentExpression: String = ""
    private var lastNumeric: Boolean = false
    private var stateError: Boolean = false
    private var lastDot: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initUI() {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(0.5, 0.3)
        )
        binding.confetti.setOnClickListener {
            binding.konfettiView.start(party)
        }
    }

    fun numberAction(view: View) {
        if (stateError) {
            binding.resulttxt.text = ""
            stateError = false
        }
        val button = view as Button
        currentExpression += button.text
        binding.resulttxt.text = currentExpression
        lastNumeric = true
    }

    fun operatorAction(view: View) {
        if (lastNumeric && !stateError) {
            val button = view as Button
            currentExpression += button.text
            binding.resulttxt.text = currentExpression
            lastNumeric = false
            lastDot = false
        }
    }

    fun AllClearAction(view: View) {
        currentExpression = ""
        binding.resulttxt.text = ""
        lastNumeric = false
        stateError = false
        lastDot = false
    }

    fun BackspaceAction(view: View) {
        if (currentExpression.isNotEmpty()) {
            currentExpression = currentExpression.substring(0, currentExpression.length - 1)
            binding.resulttxt.text = currentExpression
        }
    }

    fun onEqual(view: View) {
        if (lastNumeric && !stateError) {
            try {
                val expression = currentExpression.replace("x", "*").replace("÷", "/")
                val result = eval(expression)
                binding.resulttxt.text = result.toString()
                currentExpression = result.toString()
                lastDot = true // Result contains a dot
            } catch (e: Exception) {
                binding.resulttxt.text = "Error"
                stateError = true
                lastNumeric = false
            }
        }
    }

    private fun eval(expression: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < expression.length) expression[pos].toInt() else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.toInt()) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expression.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.toInt()) -> x += parseTerm()
                        eat('-'.toInt()) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.toInt()) -> x *= parseFactor()
                        eat('/'.toInt()) -> x /= parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.toInt())) return parseFactor()
                if (eat('-'.toInt())) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.toInt())) {
                    x = parseExpression()
                    eat(')'.toInt())
                } else if ((ch in '0'.toInt()..'9'.toInt()) || ch == '.'.toInt()) {
                    while ((ch in '0'.toInt()..'9'.toInt()) || ch == '.'.toInt()) nextChar()
                    x = expression.substring(startPos, pos).toDouble()
                } else if (ch in 'a'.toInt()..'z'.toInt()) {
                    while (ch in 'a'.toInt()..'z'.toInt()) nextChar()
                    val func = expression.substring(startPos, pos)
                    x = parseFactor()
                    if (func == "sqrt") x = Math.sqrt(x)
                    else if (func == "sin") x = Math.sin(Math.toRadians(x))
                    else if (func == "cos") x = Math.cos(Math.toRadians(x))
                    else if (func == "tan") x = Math.tan(Math.toRadians(x))
                    else throw RuntimeException("Unknown function: $func")
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }

                if (eat('^'.toInt())) x = Math.pow(x, parseFactor())

                return x
            }
        }.parse()
    }
}
