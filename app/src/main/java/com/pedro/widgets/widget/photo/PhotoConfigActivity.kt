package com.pedro.widgets.widget.photo

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import coil.compose.AsyncImage
import com.pedro.widgets.data.model.PhotoShape
import com.pedro.widgets.data.model.PhotoWidgetConfig
import com.pedro.widgets.data.storage.PhotoStorage
import com.pedro.widgets.data.storage.WidgetPreferences
import com.pedro.widgets.ui.theme.WidgetsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PhotoConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setContent {
            WidgetsTheme {
                PhotoConfigScreen(
                    widgetId = appWidgetId,
                    onSave = { config, urisToProcess ->
                        val scope = CoroutineScope(Dispatchers.IO)
                        scope.launch {
                            val storage = PhotoStorage(this@PhotoConfigActivity)
                            val prefs = WidgetPreferences(this@PhotoConfigActivity)

                            val finalPaths = if (urisToProcess.isNotEmpty()) {
                                storage.savePhotosForWidget(appWidgetId, urisToProcess)
                            } else {
                                config.photoPaths
                            }

                            val updatedConfig = config.copy(photoPaths = finalPaths)
                            prefs.savePhotoConfig(updatedConfig)

                            // Agendar alarme de rotação de fotos
                            PhotoWidgetReceiver.schedulePhotoRotation(
                                this@PhotoConfigActivity,
                                appWidgetId,
                                updatedConfig.rotationIntervalMinutes
                            )

                            try {
                                val glanceManager = GlanceAppWidgetManager(this@PhotoConfigActivity)
                                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                                PhotoWidget().update(this@PhotoConfigActivity, glanceId)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            val resultValue = Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            setResult(Activity.RESULT_OK, resultValue)
                            finish()
                        }
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }
}

private val rotationOptions = listOf(
    0 to "Ao desbloquear / Carregar",
    15 to "15 min",
    30 to "30 min",
    60 to "1 hora",
    360 to "6 horas",
    1440 to "Diário (24h)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoConfigScreen(
    widgetId: Int,
    onSave: (PhotoWidgetConfig, List<Uri>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { WidgetPreferences(context) }

    val selectedUris = remember { mutableStateListOf<Uri>() }
    val existingPaths = remember { mutableStateListOf<String>() }

    var shape by remember { mutableStateOf(PhotoShape.ROUNDED_RECTANGLE) }
    var cornerRadius by remember { mutableIntStateOf(24) }
    var padding by remember { mutableIntStateOf(0) }
    var useDynamicColors by remember { mutableStateOf(false) }
    var frameColor by remember { mutableStateOf(0xFF1C1B1FL) }
    var changeOnUnlock by remember { mutableStateOf(true) }
    var rotationIntervalMinutes by remember { mutableIntStateOf(0) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris.clear()
            selectedUris.addAll(uris)
            existingPaths.clear()
        }
    }

    LaunchedEffect(widgetId) {
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val existing = prefs.getPhotoConfig(widgetId)
            shape = existing.shape
            cornerRadius = existing.cornerRadiusDp
            padding = existing.paddingDp
            useDynamicColors = existing.useDynamicColors
            frameColor = existing.frameColor
            changeOnUnlock = existing.changeOnUnlock
            rotationIntervalMinutes = existing.rotationIntervalMinutes
            existingPaths.addAll(existing.photoPaths)
        }
    }

    val totalPhotos = selectedUris.size + existingPaths.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar Porta-Retratos", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val config = PhotoWidgetConfig(
                                widgetId = widgetId,
                                photoPaths = existingPaths.toList(),
                                currentIndex = 0,
                                shape = shape,
                                cornerRadiusDp = cornerRadius,
                                borderWidthDp = 0,
                                borderColor = 0xFFFFFFFFL,
                                useDynamicColors = useDynamicColors,
                                paddingDp = padding,
                                frameColor = frameColor,
                                changeOnUnlock = changeOnUnlock,
                                rotationIntervalMinutes = rotationIntervalMinutes
                            )
                            onSave(config, selectedUris.toList())
                        },
                        enabled = selectedUris.isNotEmpty() || existingPaths.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salvar Widget")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PRÉ-VISUALIZAÇÃO (Sem badge de contador sobre a foto)
            Text(
                text = "PRÉ-VISUALIZAÇÃO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            LivePhotoPreview(
                firstUri = selectedUris.firstOrNull(),
                firstExistingPath = existingPaths.firstOrNull(),
                shape = shape,
                cornerRadius = cornerRadius,
                padding = padding,
                useDynamicColors = useDynamicColors,
                frameColor = frameColor
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // SEÇÃO 1: FOTOS
            Text(
                text = "SELEÇÃO DE FOTOS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (totalPhotos == 0)
                        "Escolher Fotos da Galeria"
                    else "Trocar / Adicionar Fotos ($totalPhotos selecionadas)"
                )
            }

            // Lista horizontal de miniaturas
            if (selectedUris.isNotEmpty() || existingPaths.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(selectedUris) { index, uri ->
                        PhotoThumbnailItem(
                            model = uri,
                            onRemove = { selectedUris.removeAt(index) }
                        )
                    }
                    itemsIndexed(existingPaths) { index, path ->
                        PhotoThumbnailItem(
                            model = File(path),
                            onRemove = { existingPaths.removeAt(index) }
                        )
                    }
                }
            }

            // SEÇÃO 2: ROTAÇÃO AUTOMÁTICA (se mais de 1 foto selecionada)
            AnimatedVisibility(visible = totalPhotos > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "ROTAÇÃO AUTOMÁTICA DE FOTOS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    // Switch: Mudar ao desbloquear
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mudar ao desbloquear a tela",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Cada vez que você ligar ou desbloquear o celular, uma nova foto é exibida.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = changeOnUnlock,
                                onCheckedChange = { changeOnUnlock = it }
                            )
                        }
                    }

                    // Intervalo de tempo fixo
                    Text(
                        text = "Tempo de exibição de cada foto:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(rotationOptions) { _, (minutes, label) ->
                            FilterChip(
                                selected = rotationIntervalMinutes == minutes,
                                onClick = { rotationIntervalMinutes = minutes },
                                label = { Text(label) },
                                leadingIcon = if (rotationIntervalMinutes == minutes) {
                                    { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // SEÇÃO 3: FORMATOS E FORMAS
            Text(
                text = "FORMATO DO RETRATO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = shape == PhotoShape.ROUNDED_RECTANGLE,
                    onClick = { shape = PhotoShape.ROUNDED_RECTANGLE },
                    label = { Text("Retângulo") },
                    leadingIcon = { Icon(Icons.Default.CropSquare, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = shape == PhotoShape.SQUIRCLE,
                    onClick = { shape = PhotoShape.SQUIRCLE },
                    label = { Text("Squircle") },
                    leadingIcon = { Icon(Icons.Default.FormatShapes, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = shape == PhotoShape.CIRCLE_OVAL,
                    onClick = { shape = PhotoShape.CIRCLE_OVAL },
                    label = { Text("Círculo") },
                    leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Slider de curvatura se for retângulo
            if (shape == PhotoShape.ROUNDED_RECTANGLE) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Arredondamento dos Cantos",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${cornerRadius}dp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = cornerRadius.toFloat(),
                        onValueChange = { cornerRadius = it.toInt() },
                        valueRange = 0f..40f,
                        steps = 20
                    )
                }
            }

            // Moldura externa (Passe-partout)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Moldura Externa (Passe-partout)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${padding}dp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = padding.toFloat(),
                    onValueChange = { padding = it.toInt() },
                    valueRange = 0f..24f,
                    steps = 12
                )
            }

            // Material You para a moldura
            if (padding > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Moldura com Cor do Sistema",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "A cor da borda externa acompanha o Material You.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useDynamicColors,
                            onCheckedChange = { useDynamicColors = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PhotoThumbnailItem(
    model: Any,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.DarkGray)
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(Color(0x99000000), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remover",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun LivePhotoPreview(
    firstUri: Uri?,
    firstExistingPath: String?,
    shape: PhotoShape,
    cornerRadius: Int,
    padding: Int,
    useDynamicColors: Boolean,
    frameColor: Long
) {
    val radiusDp = when (shape) {
        PhotoShape.ROUNDED_RECTANGLE -> cornerRadius.dp
        PhotoShape.CIRCLE_OVAL -> 999.dp
        PhotoShape.SQUIRCLE -> 36.dp
    }

    val frameBg = if (useDynamicColors) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Color(frameColor)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(radiusDp))
                .background(frameBg)
                .padding(padding.dp),
            contentAlignment = Alignment.Center
        ) {
            val innerRadius = if (padding > 0) (radiusDp - padding.dp).coerceAtLeast(0.dp) else radiusDp

            if (firstUri != null || firstExistingPath != null) {
                AsyncImage(
                    model = firstUri ?: File(firstExistingPath ?: ""),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(innerRadius))
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "📷", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toque abaixo para escolher",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
