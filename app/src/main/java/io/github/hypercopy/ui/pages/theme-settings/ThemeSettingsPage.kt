package io.github.hypercopy.ui.pages.themesettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hypercopy.R
import io.github.hypercopy.ui.framework.AppColorMode
import io.github.hypercopy.ui.components.SettingsIcon
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
    val colorModeOptions = colorModeOptions()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.theme),
                style = MiuixTheme.textStyles.title1,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item { SmallTitle(text = stringResource(R.string.appearance)) }
        item {
            Card {
                OverlayDropdownPreference(
                    title = stringResource(R.string.color_mode),
                    summary = stringResource(R.string.color_mode_summary),
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

@Composable
private fun colorModeOptions() = listOf(
    ColorModeOption(stringResource(R.string.color_mode_system), AppColorMode.System),
    ColorModeOption(stringResource(R.string.color_mode_dark), AppColorMode.Dark),
    ColorModeOption(stringResource(R.string.color_mode_light), AppColorMode.Light),
)

private val ThemeSettingsItemMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
