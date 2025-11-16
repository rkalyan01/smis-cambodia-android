package com.innovative.smis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        options.forEach { (key, label) ->
            val isChecked = selectedKeys.contains(key)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                        val updatedList = if (checked) {
                            selectedKeys + key
                        } else {
                            selectedKeys - key
                        }
                        onSelectionChange(updatedList)
                    },
                    enabled = enabled,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
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
