package io.github.hypercopy.ui

import top.yukonga.miuix.kmp.theme.ColorSchemeMode

fun colorSchemeModeOf(colorMode: AppColorMode): ColorSchemeMode = when (colorMode) {
    AppColorMode.System -> ColorSchemeMode.System
    AppColorMode.Light -> ColorSchemeMode.Light
    AppColorMode.Dark -> ColorSchemeMode.Dark
}
