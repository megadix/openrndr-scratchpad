package megadix

import org.openrndr.PresentationMode
import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa.Companion.BLACK
import org.openrndr.color.ColorRGBa.Companion.PINK
import org.openrndr.color.ColorRGBa.Companion.WHITE
import org.openrndr.color.ColorRGBa.Companion.fromHex
import org.openrndr.color.mix
import org.openrndr.draw.loadFont
import org.openrndr.extra.noise.perlinQuintic
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.contour
import kotlin.math.*

fun main() = application {
    val TWO_PI = PI * 2.0

    val debug = true
    val static = false

    val palette = arrayOf(
        fromHex("#dd1c1a"),
        fromHex("#fff1d0"),
        fromHex("#f0c808"),
        fromHex("#086788"),
        fromHex("#06aed5"),
    )

    val randomSeed = 100

    val minThetaNoiseScale = 10.0
    val maxThetaNoiseScale = 20.0
    val animationDuration = 5000L
    // controls how much angles can vary
    val thetaNoiseAmp = PI / 4.0
    // controls how much radius can vary
    val radiusNoiseAmp = 200.0
    val centerNoiseAmp = 30.0

    val tangentScale = listOf(
        1.0,
        2.3,
        1.3,
        0.2
    )

    val num = 180

    configure {
        width = 1080
        height = 600
    }

    program {
        val font = loadFont("data/fonts/default.otf", 32.0)
        drawer.fontMap = font

        val centerX = width / 2.0
        val centerY = height / 2.0
        val radius = min(width, height) / 5.0

        if (static) {
            window.presentationMode = PresentationMode.MANUAL
            window.requestDraw()
        }

        val animation = object: Animatable() {
            var noiseParam: Double = minThetaNoiseScale
        }

        extend {
            animation.updateAnimation()

            if (!animation.hasAnimations()) {
                animation.apply {
                    ::noiseParam.animate(maxThetaNoiseScale, animationDuration, Easing.SineInOut)
                    ::noiseParam.complete()
                    ::noiseParam.animate(minThetaNoiseScale, animationDuration, Easing.SineInOut)
                    ::noiseParam.complete()
                }
            }

            drawer.clear(BLACK)

            drawer.stroke = PINK
            drawer.fill = null

            val thetaNoiseScale = map(
                0.0, width.toDouble(),
                minThetaNoiseScale, maxThetaNoiseScale,
                animation.noiseParam
            ).coerceAtLeast(minThetaNoiseScale).coerceAtMost(maxThetaNoiseScale)

            val thetaIncr = TWO_PI / num
            var theta = 0.0

            drawer.strokeWeight = 1.0

            while (theta < PI) {
                /*
                 * Starting point
                 */

                // move angle of starting point by some (pseudo-)random amount
                val thetaNoise = perlinQuintic(randomSeed, theta * thetaNoiseScale) * thetaNoiseAmp
                val thetaScattered = theta + thetaNoise

                // move radius of starting point by some (pseudo-)random amount
                val radiusNoise = perlinQuintic(randomSeed, theta * thetaNoiseScale) * radiusNoiseAmp + radiusNoiseAmp
                val radiusScattered = radius + radiusNoise

                /*
                 * Ending point
                 */

                // move angle of ending point by some (pseudo-)random amount
                val thetaOpposite = theta + PI
                val thetaOppositeNoise = perlinQuintic(randomSeed, thetaOpposite * thetaNoiseScale) * thetaNoiseAmp
                val thetaOppositeScattered = thetaOpposite + thetaOppositeNoise

                val radiusOppositeNoise =
                    perlinQuintic(randomSeed, thetaOpposite * thetaNoiseScale) * radiusNoiseAmp + radiusNoiseAmp
                val radiusOppositeScattered = radius + radiusOppositeNoise

                var points = mutableListOf<Vector2>()

                // start point
                points.add(
                    Vector2(
                        radiusScattered * cos(thetaScattered) + centerX,
                        radiusScattered * sin(thetaScattered) + centerY
                    )
                )

                // anchor
                points.add(
                    Vector2(
                        radius * cos(theta) + centerX,
                        radius * sin(theta) + centerY
                    )
                )

                // center
                points.add(
                    Vector2(
                        centerX + perlinQuintic(randomSeed, thetaNoise) * centerNoiseAmp,
                        centerY + perlinQuintic(randomSeed, thetaNoise) * centerNoiseAmp
                    )
                )

                // opposite anchor
                points.add(
                    Vector2(
                        radius * cos(thetaOpposite) + centerX,
                        radius * sin(thetaOpposite) + centerY
                    )
                )

                // end point
                points.add(
                    Vector2(
                        radiusOppositeScattered * cos(thetaOppositeScattered) + centerX,
                        radiusOppositeScattered * sin(thetaOppositeScattered) + centerY
                    )
                )

                val alpha = perlinQuintic(randomSeed, theta * 0.01)
                    .coerceAtLeast(0.2)
//                    .coerceAtMost(1.0)

//                val colorIndex = random.nextDouble(0.0, palette.size.toDouble())
                val colorIndex = abs(perlinQuintic(randomSeed, theta * 8) * palette.size.toDouble())
                val floor = floor(colorIndex)
                val ceil = ceil(colorIndex).coerceAtMost(palette.size.toDouble() - 1.toDouble())

                val color1 = palette[floor.toInt()]
                val color2 = palette[ceil.toInt()]
                val color = mix(color1, color2, ceil - floor).opacify(alpha)
                drawer.stroke = color

                /*val c = contour {
                    moveTo(points[0])
                    continueTo(points[1])
                    continueTo(points[2])
                    continueTo(points[3])
                }*/

                val c = contour {
                    moveTo(points[0])
                    curveTo(points[1], points[2])
                    continueTo(points[3], points[4])
                }

                drawer.contour(c)

/*                if (debug) {
                    drawer.pushStyle()
                    drawer.stroke = GREEN
                    drawer.circle(points[0], 5.0)
                    drawer.stroke = YELLOW
                    drawer.circle(points[1], 5.0)
                    drawer.stroke = WHITE
                    drawer.circle(points[2], 5.0)
                    drawer.stroke = RED
                    drawer.circle(points[3], 5.0)
                    drawer.popStyle()
                }*/

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