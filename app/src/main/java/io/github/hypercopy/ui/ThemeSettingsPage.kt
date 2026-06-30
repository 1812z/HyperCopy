package io.github.hypercopy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ThemeSettingsPage(
    colorMode: AppColorMode,
    onColorModeChange: (AppColorMode) -> Unit,
    bottomContentPadding: Dp = 16.dp,
) {
    val strings = LocalAppStrings.current
    val colorModeOptions = colorModeOptions(strings)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = strings.theme,
                style = MiuixTheme.textStyles.title1,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item { SmallTitle(text = strings.appearance) }
        item {
            Card {
                OverlayDropdownPreference(
                    title = strings.colorMode,
                    summary = strings.colorModeSummary,
                    items = colorModeOptions.map { it.label },
                    selectedIndex = colorModeOptions.indexOfFirst { it.value == colorMode }.coerceAtLeast(0),
                    startAction = { SettingsIcon(imageVector = MiuixIcons.Tune) },
                    insideMargin = ThemeSettingsItemMargin,
                    onSelectedIndexChange = { onColorModeChange(colorModeOptions[it].value) },
                )
            }
        }
    }
}

private data class ColorModeOption(val label: String, val value: AppColorMode)

private fun colorModeOptions(strings: UiStrings) = listOf(
    ColorModeOption(strings.colorModeSystem, AppColorMode.System),
    ColorModeOption(strings.colorModeDark, AppColorMode.Dark),
    ColorModeOption(strings.colorModeLight, AppColorMode.Light),
)

private val ThemeSettingsItemMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
