package io.github.hypercopy.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search

@Composable
fun HyperSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    SearchBar(
        inputField = {
            InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { expanded = false },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                label = label,
                leadingIcon = {
                    Icon(
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                        imageVector = MiuixIcons.Search,
                        contentDescription = "搜索",
                    )
                },
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {}
}
