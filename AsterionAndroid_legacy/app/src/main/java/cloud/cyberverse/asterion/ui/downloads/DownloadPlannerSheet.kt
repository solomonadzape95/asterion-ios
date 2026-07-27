package cloud.cyberverse.asterion.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

        LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp).padding(top = 12.dp)) {
            items(units, key = { it.id }) { unit ->
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
