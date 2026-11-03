package io.github.ntgfr.doomscrollkiller.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ntgfr.doomscrollkiller.R
import io.github.ntgfr.doomscrollkiller.debug.DebugStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Developer mode: the latest captures of the feed pages, to copy and share when updating the rules. */
@Composable
fun CapturesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val captures by DebugStore.captures.collectAsStateWithLifecycle()
    var selected by remember { mutableIntStateOf(0) }
    val current = captures.getOrNull(selected) ?: captures.firstOrNull()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                }
                Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.titleLarge)
            }
            if (current == null) {
                Text(
                    stringResource(R.string.debug_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
                return@Column
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                captures.forEachIndexed { i, capture ->
                    FilterChip(
                        selected = capture === current,
                        onClick = { selected = i },
                        label = { Text(timeFormat.format(Date(capture.timeMillis))) },
                    )
                }
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("Page capture", current.content))
                    Toast.makeText(context, R.string.debug_copied, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.debug_copy)) }
                OutlinedButton(onClick = {
                    DebugStore.clear()
                    selected = 0
                }) { Text(stringResource(R.string.debug_clear)) }
            }
            // LazyColumn only draws the visible lines: captures can be long.
            val lines = remember(current) { current.content.lines() }
            LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                items(lines) { line ->
                    Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 13.sp)
                }
            }
        }
    }
}
