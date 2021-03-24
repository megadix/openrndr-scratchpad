package megadix

import org.openrndr.PresentationMode.MANUAL
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa.Companion.BLACK
import org.openrndr.color.ColorRGBa.Companion.PINK
import org.openrndr.color.ColorRGBa.Companion.WHITE
import org.openrndr.color.ColorRGBa.Companion.fromHex
import org.openrndr.color.mix
import org.openrndr.draw.loadFont
import org.openrndr.extra.noise.perlinLinear
import org.openrndr.math.map
import org.openrndr.shape.contour
import kotlin.math.*
import kotlin.random.Random

fun main() = application {
    val TWO_PI = PI * 2.0
    val palette = arrayOf(
        fromHex("#dd1c1a"),
        fromHex("#fff1d0"),
        fromHex("#f0c808"),
        fromHex("#086788"),
        fromHex("#06aed5"),
    )

    val randomSeed = 100
    val random = Random(randomSeed)

    val minThetaNoiseScale = 31.0
    val maxThetaNoiseScale = 31.3
    val thetaNoiseAmp = 0.8
    val radiusNoiseAmp = 200.0

    val num = 360

    configure {
        width = 1080
        height = 600
    }

    program {
        val font = loadFont("data/fonts/default.otf", 32.0)
        drawer.fontMap = font

        val centerX = width / 2.0
        val centerY = height / 2.0
        val radius = min(width, height) * 0.25

        drawer.strokeWeight = 1.0

        window.presentationMode = MANUAL
        window.requestDraw()

        extend {
            drawer.clear(BLACK)

            drawer.stroke = PINK
            drawer.fill = null

            val thetaNoiseScale = map(
                0.0, width.toDouble(),
                minThetaNoiseScale, maxThetaNoiseScale,
//                mouse.position.x
                31.21
            ).coerceAtLeast(minThetaNoiseScale).coerceAtMost(maxThetaNoiseScale)

            val thetaIncr = TWO_PI / num
            var theta = 0.0

            drawer.strokeWeight = 1.0

            while (theta < PI) {
                val thetaNoise = perlinLinear(randomSeed, theta * thetaNoiseScale) * thetaNoiseAmp
                val thetaScattered = theta + thetaNoise

                val thetaOpposite = theta + PI
                val thetaOppositeNoise = perlinLinear(randomSeed, thetaOpposite * thetaNoiseScale) * thetaNoiseAmp
                val thetaOppositeScattered = thetaOpposite + thetaOppositeNoise

                val radiusNoise =
                    abs(
                        perlinLinear(randomSeed, theta * thetaNoiseScale)
                                * radiusNoiseAmp
                    )//.coerceAtLeast(radius / 2)
                val radiusScattered = radius + radiusNoise

                val radiusOppositeNoise =
                    abs(
                        perlinLinear(
                            randomSeed,
                            thetaOpposite * thetaNoiseScale
                        ) * radiusNoiseAmp
                    )//.coerceAtLeast(radius / 2)
                val radiusOppositeScattered = radius + radiusOppositeNoise

                // start point
                val x0 = radiusScattered * cos(thetaScattered) + centerX
                val y0 = radiusScattered * sin(thetaScattered) + centerY

                // anchor
                val x1 = radius * cos(theta) + centerX
                val y1 = radius * sin(theta) + centerY

                // opposite anchor
                val x2 = radius * cos(thetaOpposite) + centerX
                val y2 = radius * sin(thetaOpposite) + centerY

                // end point
                val x3 = radiusOppositeScattered * cos(thetaOppositeScattered) + centerX
                val y3 = radiusOppositeScattered * sin(thetaOppositeScattered) + centerY

                val alpha = perlinLinear(randomSeed, theta * 0.01)
                    .coerceAtLeast(0.2)
//                    .coerceAtMost(1.0)

//                val colorIndex = random.nextDouble(0.0, palette.size.toDouble())
                val colorIndex = abs(perlinLinear(randomSeed, theta * 8) * palette.size.toDouble())
                val floor = floor(colorIndex)
                val ceil = ceil(colorIndex).coerceAtMost(palette.size.toDouble() - 1.toDouble())

                val color1 = palette[floor.toInt()]
                val color2 = palette[ceil.toInt()]
                val color = mix(color1, color2, ceil - floor).opacify(alpha)
                drawer.stroke = color

                val c = contour {
                    moveTo(x0, y0)
                    continueTo(x1, y1, 2.0)
                    continueTo(x2, y2, 2.0)
                    continueTo(x3, y3, 0.2)
                }

                drawer.contour(c)

                theta += thetaIncr
            }

        }
    }

}

private fun Program.debug(text: String) {
    drawer.pushStyle()

    drawer.fill = WHITE
    drawer.stroke = null
    drawer.text(text, 10.0, 10.0)
    drawer.popStyle()
}