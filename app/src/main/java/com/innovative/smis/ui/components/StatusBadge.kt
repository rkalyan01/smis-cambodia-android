package com.innovative.smis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.innovative.smis.domain.model.TaskPriority
import com.innovative.smis.domain.model.TaskStatus

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PriorityBadge(
    priority: TaskPriority,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (priority) {
        TaskPriority.LOW -> MaterialTheme.colorScheme.tertiary to "Low"
        TaskPriority.MEDIUM -> Color(0xFFF57C00) to "Medium" // Orange
        TaskPriority.HIGH -> Color(0xFFE53935) to "High" // Red
        TaskPriority.URGENT -> Color(0xFFD32F2F) to "Urgent" // Dark Red
    }

    StatusBadge(
        text = text,
        color = color,
        modifier = modifier
    )
}

@Composable
fun TaskStatusBadge(
    status: TaskStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        TaskStatus.PENDING -> MaterialTheme.colorScheme.outline to "Pending"
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary to "In Progress"
        TaskStatus.COMPLETED -> Color(0xFF4CAF50) to "Completed" // Green
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.error to "Cancelled"
        TaskStatus.RESCHEDULED -> Color(0xFFFF9800) to "Rescheduled" // Orange
    }

    StatusBadge(
        text = text,
        color = color,
        modifier = modifier
    )
}

@Composable
fun InteractiveStatusChip(
    status: TaskStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val (containerColor, contentColor) = when (status) {
        TaskStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TaskStatus.COMPLETED -> Color(0xFFE8F5E8) to Color(0xFF2E7D32) // Light Green
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        TaskStatus.RESCHEDULED -> Color(0xFFFFF3E0) to Color(0xFFE65100) // Light Orange
    }

    FilterChip(
        onClick = onClick,
        label = {
            Text(
                text = status.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        },
        selected = false,
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
    )
}