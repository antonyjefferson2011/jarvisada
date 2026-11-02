package com.jarvis.mobile.domain

import android.content.Context

interface AssistantTool {
    fun execute(input: String, context: Context): String?
}

class LocalCommandRegistry(
    private val context: Context,
    voicePreferenceTool: AssistantTool,
) {
    private val tools: List<AssistantTool> = listOf(
        TimeAndDateTool(),
        CalculatorTool(),
        UnitConversionTool(),
        DeviceInfoTool(),
        OpenSettingsTool(),
        MapSearchTool(),
        voicePreferenceTool,
        LaunchAppTool(),
    )

    fun execute(input: String): String? =
        tools.firstNotNullOfOrNull { it.execute(input, context) }
}
