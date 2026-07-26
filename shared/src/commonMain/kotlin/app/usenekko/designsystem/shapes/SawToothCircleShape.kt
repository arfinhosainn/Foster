package app.usenekko.designsystem.shapes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.usenekko.theme.NekkoTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A circular shape whose edge has uniform rectangular notches,
 * producing the jagged / saw-tooth / stamp-border effect.
 *
 * @param teethCount  Number of teeth (notches) around the circle.
 * @param toothDepth  Fraction of the radius consumed by each notch (0f–1f).
 *                    Larger values = deeper cuts.
 * @param toothWidth  Fraction of the angular step occupied by the notch gap (0f–1f).
 *                    0.5f means teeth and gaps are equally wide.
 */
class SawToothCircleShape(
    private val teethCount: Int = 30,
    private val toothDepth: Float = 0.02f,
    private val toothWidth: Float = 0.5f,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = min(cx, cy)
        val innerR = outerR * (1f - toothDepth)

        val stepAngle = (2.0 * PI / teethCount).toFloat()
        val halfTooth = stepAngle * toothWidth / 2f

        val path = Path().apply {
            for (i in 0 until teethCount) {
                val baseAngle = i * stepAngle

                // Outer arc point (tooth tip start)
                val a0 = baseAngle - halfTooth
                // Outer arc point (tooth tip end)
                val a1 = baseAngle + halfTooth
                // Inner arc point (notch start)
                val a2 = a1
                // Inner arc point (notch end) = next tooth start
                val a3 = baseAngle + stepAngle - halfTooth

                if (i == 0) {
                    moveTo(cx + outerR * cos(a0), cy + outerR * sin(a0))
                }

                // Draw along outer radius (tooth tip)
                lineTo(cx + outerR * cos(a1), cy + outerR * sin(a1))

                // Step in to inner radius (notch)
                lineTo(cx + innerR * cos(a2), cy + innerR * sin(a2))

                // Draw along inner radius (notch floor)
                lineTo(cx + innerR * cos(a3), cy + innerR * sin(a3))

                // Step back out to outer radius (next tooth start)
                lineTo(cx + outerR * cos(a3), cy + outerR * sin(a3))
            }
            close()
        }

        return Outline.Generic(path)
    }
}


@PreviewLightDark
@Composable
private fun SawToothCirclePreview() {
    NekkoTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(SawToothCircleShape())
                .background(NekkoTheme.colors.background.b2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = NekkoTheme.colors.text.tertiary,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
