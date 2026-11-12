package com.pedro.widgets.widget.calendar

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.pedro.widgets.data.model.CalendarViewMode
import com.pedro.widgets.data.model.CalendarWidgetConfig
import com.pedro.widgets.data.storage.WidgetPreferences
import com.pedro.widgets.ui.theme.WidgetsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalendarConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cancelamento padrão se o usuário sair sem salvar
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Rejeita IDs de widgets que não pertencem a este aplicativo
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && !isOwnedWidget(appWidgetId)) {
            finish()
            return
        }

        setContent {
            WidgetsTheme {
                CalendarConfigScreen(
                    widgetId = appWidgetId,
                    onSave = { config ->
                        val scope = CoroutineScope(Dispatchers.IO)
                        scope.launch {
                            val prefs = WidgetPreferences(this@CalendarConfigActivity)
                            prefs.saveCalendarConfig(config)

                            // Atualizar widget via Glance
                            try {
                                val glanceManager = GlanceAppWidgetManager(this@CalendarConfigActivity)
                                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                                CalendarWidget().update(this@CalendarConfigActivity, glanceId)
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

    private fun isOwnedWidget(widgetId: Int): Boolean {
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId) ?: return false
        return info.provider?.packageName == packageName
    }
}

private val presetColors = listOf(
    0xFF1C1B1FL to "Preto Carvão",
    0xFF121212L to "Preto Puro",
    0xFF2D3142L to "Grafite",
    0xFF1B263BL to "Azul Marinho",
    0xFF2B2D42L to "Roxo Noturno",
    0xFF354F52L to "Verde Floresta",
    0xFF4A4E69L to "Cinza Lavanda",
    0xFFF5F5F5L to "Branco Gelo"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarConfigScreen(
    widgetId: Int,
    onSave: (CalendarWidgetConfig) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { WidgetPreferences(context) }

    var useDynamicColors by remember { mutableStateOf(true) }
    var backgroundColor by remember { mutableStateOf(0xFF1C1B1FL) }
    var backgroundAlpha by remember { mutableFloatStateOf(0.85f) }
    var cornerRadius by remember { mutableIntStateOf(24) }
    var borderWidth by remember { mutableIntStateOf(1) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.AGENDA_LIST) }
    var maxEvents by remember { mutableIntStateOf(5) }
    var showPastEvents by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    // Carregar configurações salvas previamente
    LaunchedEffect(widgetId) {
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val existing = prefs.getCalendarConfig(widgetId)
            useDynamicColors = existing.useDynamicColors
            backgroundColor = existing.backgroundColor
            backgroundAlpha = existing.backgroundAlpha
            cornerRadius = existing.cornerRadiusDp
            borderWidth = existing.borderWidthDp
            viewMode = existing.viewMode
            maxEvents = existing.maxEvents
            showPastEvents = existing.showPastEventsToday
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar Agenda", fontWeight = FontWeight.Bold) },
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
                            val config = CalendarWidgetConfig(
                                widgetId = widgetId,
                                useDynamicColors = useDynamicColors,
                                backgroundColor = backgroundColor,
                                backgroundAlpha = backgroundAlpha,
                                cornerRadiusDp = cornerRadius,
                                borderWidthDp = borderWidth,
                                borderColor = 0x33FFFFFFL,
                                viewMode = viewMode,
                                maxEvents = maxEvents,
                                showPastEventsToday = showPastEvents
                            )
                            onSave(config)
                        },
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
            // Permissão de Agenda
            if (!hasPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Acesso à Agenda necessário",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Para ler os compromissos da sua conta Google local.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) }
                        ) {
                            Text("Autorizar")
                        }
                    }
                }
            }

            // Pré-visualização em tempo real
            Text(
                text = "PRÉ-VISUALIZAÇÃO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            LiveCalendarPreview(
                useDynamicColors = useDynamicColors,
                backgroundColor = backgroundColor,
                backgroundAlpha = backgroundAlpha,
                cornerRadius = cornerRadius,
                borderWidth = borderWidth,
                viewMode = viewMode
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Seção 1: Tema & Cores
            Text(
                text = "APARÊNCIA & CORES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Switch Dynamic Color (Material You)
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
                            text = "Paleta do Sistema (Material You)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Acompanha automaticamente as cores dos ícones e do papel de parede do seu Android.",
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

            // Opacidade do Fundo
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Opacidade do Fundo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(backgroundAlpha * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = backgroundAlpha,
                    onValueChange = { backgroundAlpha = it },
                    valueRange = 0.0f..1.0f
                )
            }

            // Seleção de Cores manuais (se Dynamic Colors desligado)
            AnimatedVisibility(visible = !useDynamicColors) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Cor de Fundo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(presetColors) { (hex, name) ->
                            val isSelected = backgroundColor == hex
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { backgroundColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = name,
                                        tint = if (hex == 0xFFF5F5F5L) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Seção 2: Formato & Bordas
            Text(
                text = "FORMATO & BORDAS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Raio dos cantos
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
                    valueRange = 0f..32f,
                    steps = 15
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Seção 3: Modo de Exibição
            Text(
                text = "MODO DE EXIBIÇÃO DA AGENDA",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = viewMode == CalendarViewMode.AGENDA_LIST,
                    onClick = { viewMode = CalendarViewMode.AGENDA_LIST },
                    label = { Text("Lista de Compromissos") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ViewAgenda,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = viewMode == CalendarViewMode.NEXT_EVENT,
                    onClick = { viewMode = CalendarViewMode.NEXT_EVENT },
                    label = { Text("Próximo Evento") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LiveCalendarPreview(
    useDynamicColors: Boolean,
    backgroundColor: Long,
    backgroundAlpha: Float,
    cornerRadius: Int,
    borderWidth: Int,
    viewMode: CalendarViewMode
) {
    val bgCol = if (useDynamicColors) {
        MaterialTheme.colorScheme.surface.copy(alpha = backgroundAlpha)
    } else {
        Color(backgroundColor).copy(alpha = backgroundAlpha)
    }

    val primaryTextColor = if (useDynamicColors) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.White
    }

    val secondaryTextColor = if (useDynamicColors) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFFB0B0B0)
    }

    val accentColor = if (useDynamicColors) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFD0BCFF)
    }

    // Fundo simulado de tela de celular (com papel de parede gradiente escuro)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius.dp))
                .background(bgCol)
                .border(
                    width = borderWidth.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(cornerRadius.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Quarta-feira",
                            color = primaryTextColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "11 de Novembro",
                            color = secondaryTextColor,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (viewMode == CalendarViewMode.NEXT_EVENT) {
                    Text(
                        text = "PRÓXIMO COMPROMISSO",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reunião de Alinhamento de Produto",
                        color = primaryTextColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hoje • 14:30 - 15:30",
                        color = secondaryTextColor,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "📍 Google Meet",
                        color = secondaryTextColor,
                        fontSize = 11.sp
                    )
                } else {
                    // Preview Lista
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4285F4))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alinhamento de Produto",
                                color = primaryTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Hoje • 14:30 - 15:30",
                                color = secondaryTextColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34A853))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Jantar de Aniversário",
                                color = primaryTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Amanhã • 20:00",
                                color = secondaryTextColor,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
