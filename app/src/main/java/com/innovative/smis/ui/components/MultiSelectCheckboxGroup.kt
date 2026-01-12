package com.innovative.smis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun MultiSelectCheckboxGroup(
    label: String,
    options: Map<String, String>,
    selectedKeys: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isRequired: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (isRequired) "$label *" else label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // OPTIMIZATION 1: Reduced padding from 8.dp to 4.dp
            modifier = Modifier.padding(bottom = 4.dp)
        )

        options.forEach { (key, optionLabel) ->
            val isChecked = selectedKeys.contains(key)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // OPTIMIZATION 2: Make the whole row clickable
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = enabled) {
                        val updatedList = if (isChecked) {
                            selectedKeys - key
                        } else {
                            selectedKeys + key
                        }
                        onSelectionChange(updatedList)
                    }
                    // OPTIMIZATION 3: Use minimal vertical padding instead of fixed height
                    .padding(vertical = 0.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = null, // null because the parent Row handles the click
                    enabled = enabled,
                    // OPTIMIZATION 4: Removed fixed size(40.dp) modifier
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(
                    text = optionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
            }
        }
    }
}