package com.pedro.widgets.data.model

enum class PhotoShape {
    ROUNDED_RECTANGLE,
    CIRCLE_OVAL,
    SQUIRCLE
}

data class PhotoWidgetConfig(
    val widgetId: Int = 0,
    val photoPaths: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val shape: PhotoShape = PhotoShape.ROUNDED_RECTANGLE,
    val cornerRadiusDp: Int = 24,
    val borderWidthDp: Int = 0,
    val borderColor: Long = 0xFFFFFFFFL,
    val useDynamicColors: Boolean = false,
    val paddingDp: Int = 0,
    val frameColor: Long = 0xFF1C1B1FL,
    val changeOnUnlock: Boolean = true,
    val rotationIntervalMinutes: Int = 0
)
