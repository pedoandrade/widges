package com.pedro.widgets.widget.photo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.pedro.widgets.data.model.PhotoShape
import com.pedro.widgets.data.model.PhotoWidgetConfig
import com.pedro.widgets.data.storage.PhotoStorage
import com.pedro.widgets.data.storage.WidgetPreferences
import java.io.File

class PhotoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val prefs = WidgetPreferences(context)
        val config = prefs.getPhotoConfig(appWidgetId)
        val storage = PhotoStorage(context)

        val currentPath = if (config.photoPaths.isNotEmpty()) {
            val safeIndex = config.currentIndex % config.photoPaths.size
            config.photoPaths[safeIndex]
        } else null

        val bitmap = if (currentPath != null) {
            storage.loadBitmap(currentPath, 1000, 1000)
        } else null

        provideContent {
            GlanceTheme {
                PhotoWidgetContent(
                    context = context,
                    config = config,
                    bitmap = bitmap,
                    currentPath = currentPath
                )
            }
        }
    }
}

class NextPhotoActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val prefs = WidgetPreferences(context)
        val config = prefs.getPhotoConfig(appWidgetId)
        if (config.photoPaths.size > 1) {
            val nextIndex = (config.currentIndex + 1) % config.photoPaths.size
            prefs.savePhotoConfig(config.copy(currentIndex = nextIndex))
            PhotoWidget().update(context, glanceId)
        }
    }
}

@Composable
fun PhotoWidgetContent(
    context: Context,
    config: PhotoWidgetConfig,
    bitmap: Bitmap?,
    currentPath: String?
) {
    val radiusDp = when (config.shape) {
        PhotoShape.ROUNDED_RECTANGLE -> config.cornerRadiusDp.dp
        PhotoShape.CIRCLE_OVAL -> 999.dp
        PhotoShape.SQUIRCLE -> 36.dp
    }

    val frameBgColor = if (config.useDynamicColors) {
        GlanceTheme.colors.surfaceVariant.getColor(context)
    } else {
        Color(config.frameColor)
    }

    // Ação: se houver mais de uma foto, tocar alterna para a próxima
    val clickModifier = if (config.photoPaths.size > 1) {
        GlanceModifier.clickable(actionRunCallback<NextPhotoActionCallback>())
    } else if (currentPath != null) {
        // Tentar abrir a imagem em visualizador externo
        val file = File(currentPath)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(android.net.Uri.fromFile(file), "image/*")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        GlanceModifier.clickable(actionStartActivity(viewIntent))
    } else {
        GlanceModifier
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(radiusDp)
            .background(frameBgColor)
            .padding(config.paddingDp.dp)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "Foto selecionada",
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(if (config.paddingDp > 0) (radiusDp - config.paddingDp.dp).coerceAtLeast(0.dp) else radiusDp)
            )
        } else {
            // Estado sem fotos selecionadas
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📷",
                    style = TextStyle(fontSize = 24.sp)
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "Nenhuma foto",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "Configure no app",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
