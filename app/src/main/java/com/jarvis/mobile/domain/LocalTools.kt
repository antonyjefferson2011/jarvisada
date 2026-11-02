package com.jarvis.mobile.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.text.Normalizer

private fun normalizeCommand(input: String): String =
    Normalizer.normalize(input.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .trim()

class TimeAndDateTool : AssistantTool {
    override fun execute(input: String, context: Context): String? {
        val normalized = normalizeCommand(input)
        val now = Date()
        return when {
            Regex("\\b(que horas|horario|hora agora|horas sao)\\b").containsMatchIn(normalized) ->
                "Agora são ${SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(now)}."
            Regex("\\b(que dia|data de hoje|qual a data|data atual)\\b").containsMatchIn(normalized) ->
                "Hoje é ${SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("pt", "BR")).format(now)}."
            else -> null
        }
    }
}

class CalculatorTool : AssistantTool {
    private val evaluator = ExpressionEvaluator()

    override fun execute(input: String, context: Context): String? {
        val normalized = normalizeCommand(input)
        val expression = when {
            normalized.startsWith("calcule ") -> input.substringAfter(' ').trim()
            normalized.startsWith("quanto e ") -> input.substringAfter(' ').substringAfter(' ').trim()
            normalized.startsWith("quanto eh ") -> input.substringAfter(' ').substringAfter(' ').trim()
            normalized.startsWith("resolver ") -> input.substringAfter(' ').trim()
            Regex("^[0-9\\s.,()+*/xX-]+$").matches(input.trim()) -> input.trim()
            else -> return null
        }.trim().removeSuffix("?").removeSuffix(".")
        return try {
            val answer = evaluator.evaluate(expression)
            val symbols = DecimalFormatSymbols(Locale("pt", "BR"))
            val formatted = DecimalFormat("0.##########", symbols).format(answer)
            "O resultado é $formatted."
        } catch (error: IllegalArgumentException) {
            error.message ?: "Não consegui calcular essa expressão."
        }
    }
}

class UnitConversionTool : AssistantTool {
    private data class Conversion(
        val from: String,
        val to: String,
        val convert: (Double) -> Double,
        val outputUnit: String,
    )

    private val conversions = listOf(
        Conversion("quilometros", "metros", { it * 1000.0 }, "m"),
        Conversion("km", "m", { it * 1000.0 }, "m"),
        Conversion("metros", "quilometros", { it / 1000.0 }, "km"),
        Conversion("m", "km", { it / 1000.0 }, "km"),
        Conversion("centimetros", "metros", { it / 100.0 }, "m"),
        Conversion("cm", "m", { it / 100.0 }, "m"),
        Conversion("metros", "centimetros", { it * 100.0 }, "cm"),
        Conversion("m", "cm", { it * 100.0 }, "cm"),
        Conversion("quilogramas", "gramas", { it * 1000.0 }, "g"),
        Conversion("kg", "g", { it * 1000.0 }, "g"),
        Conversion("gramas", "quilogramas", { it / 1000.0 }, "kg"),
        Conversion("g", "kg", { it / 1000.0 }, "kg"),
        Conversion("celsius", "fahrenheit", { it * 9.0 / 5.0 + 32.0 }, "°F"),
        Conversion("fahrenheit", "celsius", { (it - 32.0) * 5.0 / 9.0 }, "°C"),
        Conversion("litros", "mililitros", { it * 1000.0 }, "mL"),
        Conversion("l", "ml", { it * 1000.0 }, "mL"),
        Conversion("mililitros", "litros", { it / 1000.0 }, "L"),
        Conversion("ml", "l", { it / 1000.0 }, "L"),
    )

    override fun execute(input: String, context: Context): String? {
        val normalized = normalizeCommand(input)
        if (!normalized.startsWith("converta ") && !normalized.startsWith("converter ")) return null
        val pattern = Regex(
            """(?:converta|converter)\s+(-?\d+(?:[.,]\d+)?)\s+(.+?)\s+(?:para|em)\s+(.+)""",
        )
        val match = pattern.find(normalized) ?: return "Use: converta 5 km para m."
        val value = match.groupValues[1].replace(',', '.').toDoubleOrNull()
            ?: return "Não consegui ler o número da conversão."
        val from = match.groupValues[2].trim()
        val to = match.groupValues[3].trim()
        val conversion = conversions.firstOrNull {
            it.from == from && it.to == to
        } ?: return "Essa conversão local ainda não está disponível. Posso converter km, m, cm, kg, g, L, mL, °C e °F."
        val symbols = DecimalFormatSymbols(Locale("pt", "BR"))
        val formatted = DecimalFormat("0.##########", symbols).format(conversion.convert(value))
        return "$value $from equivalem a $formatted ${conversion.outputUnit}."
    }
}

class DeviceInfoTool : AssistantTool {
    override fun execute(input: String, context: Context): String? {
        val normalized = normalizeCommand(input)
        val asksAboutDevice = listOf(
            "informacoes do dispositivo",
            "informacoes do celular",
            "sobre este aparelho",
            "modelo do celular",
            "versao do android",
        ).any(normalized::contains)
        if (!asksAboutDevice) return null

        val batteryIntent = context.registerReceiver(
            null,
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        val batteryLevel = batteryIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) (level * 100) / scale else null
        }
        val battery = batteryLevel?.let { " Bateria: $it%." }.orEmpty()
        return "Este aparelho é ${Build.MANUFACTURER} ${Build.MODEL}, com Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}).$battery"
    }
}

class OpenSettingsTool : AssistantTool {
    override fun execute(input: String, context: Context): String? {
        val normalized = normalizeCommand(input)
        val isSettingsRequest = listOf(
            "abrir configuracoes do android",
            "abra configuracoes do android",
            "abrir configuracoes do sistema",
            "abrir ajustes do celular",
        ).any(normalized::contains)
        if (!isSettingsRequest) return null

        return try {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            "Abrindo as configurações do Android."
        } catch (_: Exception) {
            "Não foi possível abrir as configurações deste aparelho."
        }
    }
}

class MapSearchTool : AssistantTool {
    override fun execute(input: String, context: Context): String? {
        val match = Regex(
            """(?i)^(?:pesquisar|buscar|mostrar)\s+no\s+mapa\s+(.+)$""",
        ).find(input.trim()) ?: return null
        val prefix = match.groupValues[1].trim()
        if (prefix.isBlank()) return "Diga o lugar que deseja procurar no mapa."
        return try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=${Uri.encode(prefix)}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Pesquisando “$prefix” no aplicativo de mapas."
        } catch (_: Exception) {
            "Não há um aplicativo de mapas disponível neste aparelho."
        }
    }
}

class VoicePreferenceTool(
    private val currentValue: () -> Boolean,
    private val onChange: (Boolean) -> Unit,
) : AssistantTool {
    override fun execute(input: String, context: Context): String? {
        val normalized = normalizeCommand(input)
        val enabled = when {
            normalized in setOf("ativar voz", "ativar respostas por voz", "ligar voz") -> true
            normalized in setOf("desativar voz", "desativar respostas por voz", "desligar voz") -> false
            normalized in setOf("alternar voz", "alternar respostas por voz") -> !currentValue()
            else -> return null
        }
        onChange(enabled)
        return if (enabled) "Respostas por voz ativadas." else "Respostas por voz desativadas."
    }
}

class LaunchAppTool : AssistantTool {
    override fun execute(input: String, context: Context): String? {
        val normalized = normalizeCommand(input)
        val name = when {
            normalized.startsWith("abrir ") -> input.trim().substringAfter(' ').trim()
            normalized.startsWith("abra ") -> input.trim().substringAfter(' ').trim()
            else -> return null
        }
        if (normalizeCommand(name).startsWith("configuracoes") ||
            normalizeCommand(name).startsWith("ajustes")
        ) {
            return null
        }
        if (name.isBlank()) return "Diga o nome do aplicativo que deseja abrir."

        val packageManager = context.packageManager
        val launchIntent = try {
            val candidates = packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                PackageManager.MATCH_DEFAULT_ONLY,
            )
            val match = candidates.firstOrNull {
                normalizeCommand(it.loadLabel(packageManager).toString()) == normalizeCommand(name)
            } ?: candidates.firstOrNull {
                normalizeCommand(it.loadLabel(packageManager).toString())
                    .contains(normalizeCommand(name))
            }
            match?.activityInfo?.packageName?.let(packageManager::getLaunchIntentForPackage)
        } catch (_: Exception) {
            null
        }
        if (launchIntent == null) return "Não encontrei o aplicativo “$name” instalado."

        return try {
            context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Abrindo $name."
        } catch (_: Exception) {
            "O Android não permitiu abrir $name."
        }
    }
}
