package cloud.cyberverse.asterion.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.draw.clip
import cloud.cyberverse.asterion.ui.theme.PillShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class PlannerUnit(val id: String, val label: String)

@Composable
fun DownloadPlannerSheet(
    title: String,
    units: List<PlannerUnit>,
    qualityOptions: List<String> = emptyList(),
    onConfirm: (selectedIds: Set<String>, quality: String?) -> Unit,
) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var selectedQuality by remember { mutableStateOf(qualityOptions.firstOrNull()) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Row(modifier = Modifier.padding(top = 8.dp)) {
            TextButton(onClick = { selected = units.map { it.id }.toSet() }) { Text("Select all") }
            TextButton(onClick = { selected = emptySet() }) { Text("Clear") }
        }

        if (qualityOptions.isNotEmpty()) {
            Text("Quality", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                qualityOptions.forEach { quality ->
                    FilterChip(
                        selected = selectedQuality == quality,
                        onClick = { selectedQuality = quality },
                        label = { Text(quality) },
                    )
                }
            }
        }

        // Picking episode 430 of 500 by dragging a 280dp window was the worst list in the app.
        // Ranges narrow it first, and "select visible" acts on what the range shows rather than
        // forcing all-or-nothing.
        val rangeSize = 100
        var selectedRange by remember(units.size) { mutableStateOf(0) }
        val visibleUnits = remember(units, selectedRange) {
            units.drop(selectedRange * rangeSize).take(rangeSize)
        }

        if (units.size > rangeSize) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                items((0 until (units.size + rangeSize - 1) / rangeSize).toList()) { index ->
                    val isSelected = index == selectedRange
                    val start = index * rangeSize + 1
                    val end = minOf((index + 1) * rangeSize, units.size)
                    Box(
                        Modifier
                            .clip(PillShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                            )
                            .clickable { selectedRange = index }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            "$start-$end",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
            TextButton(onClick = { selected = selected + visibleUnits.map { it.id } }) {
                Text("Select these ${visibleUnits.size}")
            }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp).padding(top = 12.dp)) {
            items(visibleUnits, key = { it.id }) { unit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = unit.id in selected,
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + unit.id else selected - unit.id
                        },
                    )
                    Text(unit.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Button(
            onClick = { onConfirm(selected, selectedQuality) },
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text("Download ${selected.size}")
        }
    }
}
