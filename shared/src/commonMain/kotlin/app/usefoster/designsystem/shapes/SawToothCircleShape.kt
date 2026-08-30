package app.usefoster.designsystem.shapes

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
import app.usefoster.theme.FosterTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * A rounded-square (squircle) shape whose edge has uniform rectangular notches,
 * producing the jagged / saw-tooth / stamp-border effect.
 *
 * @param teethCount      Number of teeth (notches) around the shape.
 * @param toothDepth      Fraction of the radius consumed by each notch (0f–1f).
 *                        Larger values = deeper cuts.
 * @param toothWidth      Fraction of the angular step occupied by the notch gap (0f–1f).
 *                        0.5f means teeth and gaps are equally wide.
 * @param cornerRoundness How square the base shape is (superellipse exponent):
 *                        2f = circle/ellipse, 4f = square with clearly rounded
 *                        corners, higher values approach a sharp square.
 */
class SawToothCircleShape(
    private val teethCount: Int = 40,
    private val toothDepth: Float = 0.02f,
    private val toothWidth: Float = 0.5f,
    private val cornerRoundness: Float = 2.4f,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rx = cx
        val ry = cy
        val innerScale = 1f - toothDepth
        val n = cornerRoundness

        // Superellipse radius factor: for a given angle, how far from the
        // center the base curve is, relative to rx/ry. Exponent n = 2 gives a
        // perfect ellipse; larger n pushes the curve out toward a square with
        // rounded corners.
        fun baseFactor(angle: Float): Float {
            val c = abs(cos(angle))
            val s = abs(sin(angle))
            return (c.pow(n) + s.pow(n)).pow(-1f / n)
        }

        fun outerX(angle: Float) = cx + rx * baseFactor(angle) * cos(angle)
        fun outerY(angle: Float) = cy + ry * baseFactor(angle) * sin(angle)
        fun innerX(angle: Float) = cx + rx * baseFactor(angle) * innerScale * cos(angle)
        fun innerY(angle: Float) = cy + ry * baseFactor(angle) * innerScale * sin(angle)

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
                    moveTo(outerX(a0), outerY(a0))
                }

                // Draw along outer edge (tooth tip)
                lineTo(outerX(a1), outerY(a1))

                // Step in to inner edge (notch)
                lineTo(innerX(a2), innerY(a2))

                // Draw along inner edge (notch floor)
                lineTo(innerX(a3), innerY(a3))

                // Step back out to outer edge (next tooth start)
                lineTo(outerX(a3), outerY(a3))
            }
            close()
        }

        return Outline.Generic(path)
    }
}


@PreviewLightDark
@Composable
private fun SawToothCirclePreview() {
    FosterTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(SawToothCircleShape())
                .background(FosterTheme.colors.background.b2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = FosterTheme.colors.text.tertiary,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
