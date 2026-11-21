package com.minerrelay.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icônes vectorielles légères remplaçant `material-icons-extended`
 * pour réduire drastiquement la taille de l'APK release.
 */
object AppIcons {
    private fun materialVector(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply(block).build()

    val Stop: ImageVector by lazy {
        materialVector("Stop") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 6f)
                horizontalLineTo(18f)
                verticalLineTo(18f)
                horizontalLineTo(6f)
                close()
            }
        }
    }

    val Speed: ImageVector by lazy {
        materialVector("Speed") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.38f, 8.57f)
                lineTo(19.14f, 10.51f)
                curveTo(19.69f, 11.83f, 20f, 13.28f, 20f, 14.8f)
                curveTo(20f, 15.96f, 19.72f, 17.05f, 19.22f, 18.02f)
                horizontalLineTo(4.78f)
                curveTo(4.28f, 17.05f, 4f, 15.96f, 4f, 14.8f)
                curveTo(4f, 10.49f, 7.58f, 7f, 12f, 7f)
                curveTo(13.52f, 7f, 14.94f, 7.41f, 16.16f, 8.13f)
                lineTo(17.95f, 6.72f)
                curveTo(16.22f, 5.63f, 14.19f, 5f, 12f, 5f)
                curveTo(6.48f, 5f, 2f, 9.48f, 2f, 15f)
                curveTo(2f, 16.82f, 2.49f, 18.53f, 3.35f, 20f)
                horizontalLineTo(20.65f)
                curveTo(21.51f, 18.53f, 22f, 16.82f, 22f, 15f)
                curveTo(22f, 12.63f, 21.41f, 10.41f, 20.38f, 8.57f)
                close()
                moveTo(10.59f, 16.41f)
                curveTo(11.37f, 17.19f, 12.63f, 17.19f, 13.41f, 16.41f)
                lineTo(18f, 9f)
                lineTo(10.59f, 13.59f)
                curveTo(9.81f, 14.37f, 9.81f, 15.63f, 10.59f, 16.41f)
                close()
            }
        }
    }

    val Wallet: ImageVector by lazy {
        materialVector("Wallet") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 18f)
                verticalLineTo(19f)
                curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
                horizontalLineTo(5f)
                curveTo(3.89f, 21f, 3f, 20.1f, 3f, 19f)
                verticalLineTo(5f)
                curveTo(3f, 3.9f, 3.89f, 3f, 5f, 3f)
                horizontalLineTo(19f)
                curveTo(20.1f, 3f, 21f, 3.9f, 21f, 5f)
                verticalLineTo(6f)
                horizontalLineTo(12f)
                curveTo(10.89f, 6f, 10f, 6.9f, 10f, 8f)
                verticalLineTo(16f)
                curveTo(10f, 17.1f, 10.89f, 18f, 12f, 18f)
                horizontalLineTo(21f)
                close()
                moveTo(12f, 16f)
                horizontalLineTo(22f)
                verticalLineTo(8f)
                horizontalLineTo(12f)
                verticalLineTo(16f)
                close()
                moveTo(16f, 13.5f)
                curveTo(15.17f, 13.5f, 14.5f, 12.83f, 14.5f, 12f)
                curveTo(14.5f, 11.17f, 15.17f, 10.5f, 16f, 10.5f)
                curveTo(16.83f, 10.5f, 17.5f, 11.17f, 17.5f, 12f)
                curveTo(17.5f, 12.83f, 16.83f, 13.5f, 16f, 13.5f)
                close()
            }
        }
    }

    val ShowChart: ImageVector by lazy {
        materialVector("ShowChart") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3.5f, 18.49f)
                lineTo(9.5f, 12.48f)
                lineTo(13.5f, 16.48f)
                lineTo(22f, 6.92f)
                lineTo(20.59f, 5.51f)
                lineTo(13.5f, 13.48f)
                lineTo(9.5f, 9.48f)
                lineTo(2f, 16.99f)
                close()
            }
        }
    }

    val TrendingUp: ImageVector by lazy {
        materialVector("TrendingUp") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 6f)
                lineTo(18.29f, 8.29f)
                lineTo(13.41f, 13.17f)
                lineTo(9.41f, 9.17f)
                lineTo(2f, 16.59f)
                lineTo(3.41f, 18f)
                lineTo(9.41f, 12f)
                lineTo(13.41f, 16f)
                lineTo(19.71f, 9.71f)
                lineTo(22f, 12f)
                verticalLineTo(6f)
                close()
            }
        }
    }

    val History: ImageVector by lazy {
        materialVector("History") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13f, 3f)
                curveTo(8.03f, 3f, 4f, 7.03f, 4f, 12f)
                horizontalLineTo(1f)
                lineTo(4.89f, 15.89f)
                lineTo(4.96f, 16.03f)
                lineTo(9f, 12f)
                horizontalLineTo(6f)
                curveTo(6f, 8.13f, 9.13f, 5f, 13f, 5f)
                curveTo(16.87f, 5f, 20f, 8.13f, 20f, 12f)
                curveTo(20f, 15.87f, 16.87f, 19f, 13f, 19f)
                curveTo(11.07f, 19f, 9.32f, 18.21f, 8.06f, 16.94f)
                lineTo(6.64f, 18.36f)
                curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
                curveTo(17.97f, 21f, 22f, 16.97f, 22f, 12f)
                curveTo(22f, 7.03f, 17.97f, 3f, 13f, 3f)
                close()
                moveTo(12f, 8f)
                verticalLineTo(13f)
                lineTo(16.28f, 15.54f)
                lineTo(17f, 14.33f)
                lineTo(13.5f, 12.25f)
                verticalLineTo(8f)
                close()
            }
        }
    }

    val BugReport: ImageVector by lazy {
        materialVector("BugReport") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 8f)
                horizontalLineTo(17.19f)
                curveTo(16.74f, 7.22f, 16.12f, 6.55f, 15.37f, 6.04f)
                lineTo(17f, 4.41f)
                lineTo(15.59f, 3f)
                lineTo(13.42f, 5.17f)
                curveTo(12.96f, 5.06f, 12.49f, 5f, 12f, 5f)
                curveTo(11.51f, 5f, 11.04f, 5.06f, 10.59f, 5.17f)
                lineTo(8.41f, 3f)
                lineTo(7f, 4.41f)
                lineTo(8.62f, 6.04f)
                curveTo(7.88f, 6.55f, 7.26f, 7.22f, 6.81f, 8f)
                horizontalLineTo(4f)
                verticalLineTo(10f)
                horizontalLineTo(6.09f)
                curveTo(6.04f, 10.33f, 6f, 10.66f, 6f, 11f)
                verticalLineTo(12f)
                horizontalLineTo(4f)
                verticalLineTo(14f)
                horizontalLineTo(6f)
                verticalLineTo(15f)
                curveTo(6f, 15.34f, 6.04f, 15.67f, 6.09f, 16f)
                horizontalLineTo(4f)
                verticalLineTo(18f)
                horizontalLineTo(6.81f)
                curveTo(7.85f, 19.79f, 9.78f, 21f, 12f, 21f)
                curveTo(14.22f, 21f, 16.15f, 19.79f, 17.19f, 18f)
                horizontalLineTo(20f)
                verticalLineTo(16f)
                horizontalLineTo(17.91f)
                curveTo(17.96f, 15.67f, 18f, 15.34f, 18f, 15f)
                verticalLineTo(14f)
                horizontalLineTo(20f)
                verticalLineTo(12f)
                horizontalLineTo(18f)
                verticalLineTo(11f)
                curveTo(18f, 10.66f, 17.96f, 10.33f, 17.91f, 10f)
                horizontalLineTo(20f)
                verticalLineTo(8f)
                close()
                moveTo(14f, 16f)
                horizontalLineTo(10f)
                verticalLineTo(14f)
                horizontalLineTo(14f)
                verticalLineTo(16f)
                close()
                moveTo(14f, 12f)
                horizontalLineTo(10f)
                verticalLineTo(10f)
                horizontalLineTo(14f)
                verticalLineTo(12f)
                close()
            }
        }
    }

    val Key: ImageVector by lazy {
        materialVector("Key") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.65f, 10f)
                curveTo(11.83f, 7.67f, 9.61f, 6f, 7f, 6f)
                curveTo(3.69f, 6f, 1f, 8.69f, 1f, 12f)
                curveTo(1f, 15.31f, 3.69f, 18f, 7f, 18f)
                curveTo(9.61f, 18f, 11.83f, 16.33f, 12.65f, 14f)
                horizontalLineTo(17f)
                verticalLineTo(18f)
                horizontalLineTo(21f)
                verticalLineTo(14f)
                horizontalLineTo(23f)
                verticalLineTo(10f)
                horizontalLineTo(12.65f)
                close()
                moveTo(7f, 14f)
                curveTo(5.9f, 14f, 5f, 13.1f, 5f, 12f)
                curveTo(5f, 10.9f, 5.9f, 10f, 7f, 10f)
                curveTo(8.1f, 10f, 9f, 10.9f, 9f, 12f)
                curveTo(9f, 13.1f, 8.1f, 14f, 7f, 14f)
                close()
            }
        }
    }

    val Visibility: ImageVector by lazy {
        materialVector("Visibility") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 4.5f)
                curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
                curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
                curveTo(17f, 19.5f, 21.27f, 16.39f, 23f, 12f)
                curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
                close()
                moveTo(12f, 17f)
                curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
                curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
                curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
                curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f)
                close()
                moveTo(12f, 9f)
                curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
                curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
                curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
                curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
                close()
            }
        }
    }

    val VisibilityOff: ImageVector by lazy {
        materialVector("VisibilityOff") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 7f)
                curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
                curveTo(17f, 12.65f, 16.87f, 13.26f, 16.64f, 13.83f)
                lineTo(19.56f, 16.75f)
                curveTo(21.07f, 15.49f, 22.26f, 13.86f, 23f, 12f)
                curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
                curveTo(10.6f, 4.5f, 9.26f, 4.75f, 8.02f, 5.2f)
                lineTo(10.17f, 7.36f)
                curveTo(10.74f, 7.13f, 11.35f, 7f, 12f, 7f)
                close()
                moveTo(2f, 4.27f)
                lineTo(4.28f, 6.55f)
                lineTo(4.74f, 7.01f)
                curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1f, 12f)
                curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
                curveTo(13.55f, 19.5f, 15.03f, 19.2f, 16.38f, 18.66f)
                lineTo(16.8f, 19.08f)
                lineTo(19.73f, 22f)
                lineTo(21f, 20.73f)
                lineTo(3.27f, 3f)
                close()
            }
        }
    }

    val ContentCopy: ImageVector by lazy {
        materialVector("ContentCopy") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 1f)
                horizontalLineTo(4f)
                curveTo(2.9f, 1f, 2f, 1.9f, 2f, 3f)
                verticalLineTo(17f)
                horizontalLineTo(4f)
                verticalLineTo(3f)
                horizontalLineTo(16f)
                verticalLineTo(1f)
                close()
                moveTo(19f, 5f)
                horizontalLineTo(8f)
                curveTo(6.9f, 5f, 6f, 5.9f, 6f, 7f)
                verticalLineTo(21f)
                curveTo(6f, 22.1f, 6.9f, 23f, 8f, 23f)
                horizontalLineTo(19f)
                curveTo(20.1f, 23f, 21f, 22.1f, 21f, 21f)
                verticalLineTo(7f)
                curveTo(21f, 5.9f, 20.1f, 5f, 19f, 5f)
                close()
                moveTo(19f, 21f)
                horizontalLineTo(8f)
                verticalLineTo(7f)
                horizontalLineTo(19f)
                verticalLineTo(21f)
                close()
            }
        }
    }

    val ContentPaste: ImageVector by lazy {
        materialVector("ContentPaste") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 2f)
                horizontalLineTo(14.82f)
                curveTo(14.4f, 0.84f, 13.3f, 0f, 12f, 0f)
                curveTo(10.7f, 0f, 9.6f, 0.84f, 9.18f, 2f)
                horizontalLineTo(5f)
                curveTo(3.9f, 2f, 3f, 2.9f, 3f, 4f)
                verticalLineTo(20f)
                curveTo(3f, 21.1f, 3.9f, 22f, 5f, 22f)
                horizontalLineTo(19f)
                curveTo(20.1f, 22f, 21f, 21.1f, 21f, 20f)
                verticalLineTo(4f)
                curveTo(21f, 2.9f, 20.1f, 2f, 19f, 2f)
                close()
                moveTo(12f, 2f)
                curveTo(12.55f, 2f, 13f, 2.45f, 13f, 3f)
                curveTo(13f, 3.55f, 12.55f, 4f, 12f, 4f)
                curveTo(11.45f, 4f, 11f, 3.55f, 11f, 3f)
                curveTo(11f, 2.45f, 11.45f, 2f, 12f, 2f)
                close()
                moveTo(19f, 20f)
                horizontalLineTo(5f)
                verticalLineTo(4f)
                horizontalLineTo(7f)
                verticalLineTo(7f)
                horizontalLineTo(17f)
                verticalLineTo(4f)
                horizontalLineTo(19f)
                verticalLineTo(20f)
                close()
            }
        }
    }

    val Save: ImageVector by lazy {
        materialVector("Save") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17f, 3f)
                horizontalLineTo(5f)
                curveTo(3.89f, 3f, 3f, 3.9f, 3f, 5f)
                verticalLineTo(19f)
                curveTo(3f, 20.1f, 3.89f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                verticalLineTo(7f)
                close()
                moveTo(12f, 19f)
                curveTo(10.34f, 19f, 9f, 17.66f, 9f, 16f)
                curveTo(9f, 14.34f, 10.34f, 13f, 12f, 13f)
                curveTo(13.66f, 13f, 15f, 14.34f, 15f, 16f)
                curveTo(15f, 17.66f, 13.66f, 19f, 12f, 19f)
                close()
                moveTo(15f, 9f)
                horizontalLineTo(5f)
                verticalLineTo(5f)
                horizontalLineTo(15f)
                close()
            }
        }
    }

    val Security: ImageVector by lazy {
        materialVector("Security") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 1f)
                lineTo(3f, 5f)
                verticalLineTo(11f)
                curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
                curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
                verticalLineTo(5f)
                close()
            }
        }
    }

    val AutoAwesome: ImageVector by lazy {
        materialVector("AutoAwesome") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 9f)
                lineTo(20.25f, 6.25f)
                lineTo(23f, 5f)
                lineTo(20.25f, 3.75f)
                lineTo(19f, 1f)
                lineTo(17.75f, 3.75f)
                lineTo(15f, 5f)
                lineTo(17.75f, 6.25f)
                close()
                moveTo(11.5f, 9.5f)
                lineTo(9f, 4f)
                lineTo(6.5f, 9.5f)
                lineTo(1f, 12f)
                lineTo(6.5f, 14.5f)
                lineTo(9f, 20f)
                lineTo(11.5f, 14.5f)
                lineTo(17f, 12f)
                close()
            }
        }
    }

    val Wifi: ImageVector by lazy {
        materialVector("Wifi") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(1f, 9f)
                lineTo(3f, 11f)
                curveTo(7.97f, 6.03f, 16.03f, 6.03f, 21f, 11f)
                lineTo(23f, 9f)
                curveTo(16.93f, 2.93f, 7.08f, 2.93f, 1f, 9f)
                close()
                moveTo(9f, 17f)
                lineTo(12f, 20f)
                lineTo(15f, 17f)
                curveTo(13.35f, 15.34f, 10.66f, 15.34f, 9f, 17f)
                close()
                moveTo(5f, 13f)
                lineTo(7f, 15f)
                curveTo(9.76f, 12.24f, 14.24f, 12.24f, 17f, 15f)
                lineTo(19f, 13f)
                curveTo(15.14f, 9.14f, 8.87f, 9.14f, 5f, 13f)
                close()
            }
        }
    }

    val BatteryCharging: ImageVector by lazy {
        materialVector("BatteryCharging") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15.67f, 4f)
                horizontalLineTo(14f)
                verticalLineTo(2f)
                horizontalLineTo(10f)
                verticalLineTo(4f)
                horizontalLineTo(8.33f)
                curveTo(7.6f, 4f, 7f, 4.6f, 7f, 5.33f)
                verticalLineTo(20.67f)
                curveTo(7f, 21.4f, 7.6f, 22f, 8.33f, 22f)
                horizontalLineTo(15.67f)
                curveTo(16.4f, 22f, 17f, 21.4f, 17f, 20.67f)
                verticalLineTo(5.33f)
                curveTo(17f, 4.6f, 16.4f, 4f, 15.67f, 4f)
                close()
                moveTo(11f, 20f)
                verticalLineTo(14.5f)
                horizontalLineTo(9f)
                lineTo(13f, 7f)
                verticalLineTo(12.5f)
                horizontalLineTo(15f)
                close()
            }
        }
    }

    val Dns: ImageVector by lazy {
        materialVector("Dns") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 13f)
                horizontalLineTo(4f)
                curveTo(3.45f, 13f, 3f, 13.45f, 3f, 14f)
                verticalLineTo(20f)
                curveTo(3f, 20.55f, 3.45f, 21f, 4f, 21f)
                horizontalLineTo(20f)
                curveTo(20.55f, 21f, 21f, 20.55f, 21f, 20f)
                verticalLineTo(14f)
                curveTo(21f, 13.45f, 20.55f, 13f, 20f, 13f)
                close()
                moveTo(7f, 19f)
                curveTo(5.9f, 19f, 5f, 18.1f, 5f, 17f)
                curveTo(5f, 15.9f, 5.9f, 15f, 7f, 15f)
                curveTo(8.1f, 15f, 9f, 15.9f, 9f, 17f)
                curveTo(9f, 18.1f, 8.1f, 19f, 7f, 19f)
                close()
                moveTo(20f, 3f)
                horizontalLineTo(4f)
                curveTo(3.45f, 3f, 3f, 3.45f, 3f, 4f)
                verticalLineTo(10f)
                curveTo(3f, 10.55f, 3.45f, 11f, 4f, 11f)
                horizontalLineTo(20f)
                curveTo(20.55f, 11f, 21f, 10.55f, 21f, 10f)
                verticalLineTo(4f)
                curveTo(21f, 3.45f, 20.55f, 3f, 20f, 3f)
                close()
                moveTo(7f, 9f)
                curveTo(5.9f, 9f, 5f, 8.1f, 5f, 7f)
                curveTo(5f, 5.9f, 5.9f, 5f, 7f, 5f)
                curveTo(8.1f, 5f, 9f, 5.9f, 9f, 7f)
                curveTo(9f, 8.1f, 8.1f, 9f, 7f, 9f)
                close()
            }
        }
    }

    val HelpOutline: ImageVector by lazy {
        materialVector("HelpOutline") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 18f)
                horizontalLineTo(13f)
                verticalLineTo(16f)
                horizontalLineTo(11f)
                close()
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 20f)
                curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
                curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
                curveTo(16.41f, 4f, 20f, 7.59f, 20f, 12f)
                curveTo(20f, 16.41f, 16.41f, 20f, 12f, 20f)
                close()
                moveTo(12f, 6f)
                curveTo(9.79f, 6f, 8f, 7.79f, 8f, 10f)
                horizontalLineTo(10f)
                curveTo(10f, 8.9f, 10.9f, 8f, 12f, 8f)
                curveTo(13.1f, 8f, 14f, 8.9f, 14f, 10f)
                curveTo(14f, 12f, 11f, 11.75f, 11f, 15f)
                horizontalLineTo(13f)
                curveTo(13f, 12.75f, 16f, 12.5f, 16f, 10f)
                curveTo(16f, 7.79f, 14.21f, 6f, 12f, 6f)
                close()
            }
        }
    }

    val DeleteSweep: ImageVector by lazy {
        materialVector("DeleteSweep") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15f, 16f)
                horizontalLineTo(19f)
                verticalLineTo(18f)
                horizontalLineTo(15f)
                close()
                moveTo(15f, 8f)
                horizontalLineTo(22f)
                verticalLineTo(10f)
                horizontalLineTo(15f)
                close()
                moveTo(15f, 12f)
                horizontalLineTo(21f)
                verticalLineTo(14f)
                horizontalLineTo(15f)
                close()
                moveTo(3f, 18f)
                curveTo(3f, 19.1f, 3.9f, 20f, 5f, 20f)
                horizontalLineTo(11f)
                curveTo(12.1f, 20f, 13f, 19.1f, 13f, 18f)
                verticalLineTo(8f)
                horizontalLineTo(3f)
                close()
                moveTo(14f, 5f)
                horizontalLineTo(11f)
                lineTo(10f, 4f)
                horizontalLineTo(6f)
                lineTo(5f, 5f)
                horizontalLineTo(2f)
                verticalLineTo(7f)
                horizontalLineTo(14f)
                close()
            }
        }
    }
}
