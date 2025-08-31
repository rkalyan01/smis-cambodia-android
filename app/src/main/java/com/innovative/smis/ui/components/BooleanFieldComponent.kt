package com.innovative.smis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BooleanField(
    title: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    yesLabel: String = "Yes",
    noLabel: String = "No"
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Row(
                modifier = Modifier
                    .selectable(
                        selected = value,
                        onClick = { onValueChange(true) }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = value,
                    onClick = { onValueChange(true) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = yesLabel,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Row(
                modifier = Modifier
                    .selectable(
                        selected = !value,
                        onClick = { onValueChange(false) }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = !value,
                    onClick = { onValueChange(false) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = noLabel,
                    fontSize = 14.sp
                )
            }
        }
    }
}