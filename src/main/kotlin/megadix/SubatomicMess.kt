package megadix

import org.openrndr.Fullscreen
import org.openrndr.KEY_SPACEBAR
import org.openrndr.PresentationMode
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorRGBa.Companion.WHITE
import org.openrndr.color.ColorRGBa.Companion.fromHex
import org.openrndr.color.rgb
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.loadFont
import org.openrndr.extra.noise.perlinQuintic
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.mod
import org.openrndr.shape.contour
import org.openrndr.text.writer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun main() = application {
    val TWO_PI = PI * 2.0

    // parameters

    val static = false
    val debug = false
    val goFullscreen = false

    val num = 720
    val radiusSize = 0.5

    val randomSeed = 100

    val minThetaNoiseScale = 10.0
    val maxThetaNoiseScale = 30.0

    val animationDuration = 2000L
    val easing = Easing.SineInOut

    // controls how much angles can vary
    val thetaNoiseAmp = PI / 2.0
    // controls how much radius can vary
    val radiusNoiseAmp = 100.0
    val centerNoiseAmp = 30.0

    configure {
        if (goFullscreen) {
            fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        } else {
            width = 1080
            height = 600
        }
    }

    program {

        /*
         * Initialization
         */

        val font = loadFont("data/fonts/default.otf", 32.0)
        drawer.fontMap = font

        val centerX = width / 2.0
        val centerY = height / 2.0
        val radius = min(width, height) * radiusSize

        if (static) {
            window.presentationMode = PresentationMode.MANUAL
            window.requestDraw()
        }

        val animation = object : Animatable() {
            var noiseParam: Double = minThetaNoiseScale
        }

        val palettes = listOf(
            listOf("d00000", "d00000", "ffba08", "4cc9f0", "4cc9f0", "ff006e"),
            listOf("e63946", "e63946", "a8dadc", "1d3557", "1d3557"),
            listOf("dd1c1a", "fff1d0", "f0c808", "086788", "06aed5"),
            listOf("ff0000", "99ff00", "00ff99", "0099ff", "0000ff"),
        )

        var curPalette = 0

        val gradients = palettes.map { palette ->
            val gradient = NPointLinearStrokeGradient(palette.map(::fromHex).toTypedArray())

            gradient.points = Array(palette.size) { i ->
                // uniform distribution
                (i / (palette.size - 1.0))
            }
            gradient
        }

        // cycle palettes using spacebar
        keyboard.keyDown.listen {
            if (it.key == KEY_SPACEBAR) {
                curPalette = (curPalette + 1) % palettes.size
            }
        }

        /*
         * Main draw function
         */

        extend {
            drawer.clear(rgb(0.1))

            animation.updateAnimation()

            if (!animation.hasAnimations()) {
                animation.apply {
                    ::noiseParam.animate(maxThetaNoiseScale, animationDuration, easing)
                    ::noiseParam.complete()
                    ::noiseParam.animate(minThetaNoiseScale, animationDuration, easing)
                    ::noiseParam.complete()
                }
            }

            val thetaNoiseScale = map(
                0.0, width.toDouble(),
                minThetaNoiseScale, maxThetaNoiseScale,
                animation.noiseParam
            ).coerceAtLeast(minThetaNoiseScale).coerceAtMost(maxThetaNoiseScale)

            val thetaIncr = TWO_PI / num
            var theta = 0.0

            while (theta < PI) {
                var thetaNorm = map(
                    0.0, TWO_PI,
                    0.0, 1.0,
                    theta
                )

                /*
                 * Starting point
                 */

                // move angle of starting point by some (pseudo-)random amount
                val thetaNoiseParam = thetaNorm * thetaNoiseScale
                val thetaNoise = perlinQuintic(randomSeed, thetaNoiseParam) * thetaNoiseAmp
                val thetaScattered = theta + thetaNoise

                // move radius of starting point by some (pseudo-)random amount
                val radiusNoise = perlinQuintic(randomSeed, thetaNoiseParam) * radiusNoiseAmp + radiusNoiseAmp
                val radiusScattered = radius + radiusNoise

                /*
                 * Ending point
                 */

                // move angle of ending point by some (pseudo-)random amount
                val thetaOpposite = mod(theta + PI, TWO_PI)
                var thetaOppositeNorm = map(
                    0.0, TWO_PI,
                    0.0, 1.0,
                    thetaOpposite
                )
                val thetaOppositeNoiseParam = thetaOppositeNorm * thetaNoiseScale

                val thetaOppositeNoise = perlinQuintic(randomSeed, thetaOppositeNoiseParam) * thetaNoiseAmp
                val thetaOppositeScattered = thetaOpposite + thetaOppositeNoise

                val radiusOppositeNoise =
                    perlinQuintic(randomSeed, thetaOppositeNoiseParam) * radiusNoiseAmp + radiusNoiseAmp
                val radiusOppositeScattered = radius + radiusOppositeNoise

                val points = mutableListOf<Vector2>()

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
                        centerX + perlinQuintic(randomSeed, thetaNoiseParam) * centerNoiseAmp,
                        centerY + perlinQuintic(randomSeed, thetaOppositeNoiseParam) * centerNoiseAmp
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

                val c = contour {
                    moveTo(points[0])
                    curveTo(points[1], points[2])
                    continueTo(points[3], points[4])
                }

                val gradient = gradients[curPalette]
                gradient.rotation = theta
                drawer.shadeStyle = gradient

                val alphaMod = map(
                    -1.0, 1.0,
                    0.1, 2.0,
                    sin(seconds / 3.0)
                )
                drawer.stroke = WHITE.opacify(
                    perlinQuintic(randomSeed, thetaNoiseParam * alphaMod)
                )
                drawer.contour(c)

                if (debug) {
                    writer {
                        newLine()
//                        text("alphaMod: $alphaMod")
                        text("curPalette = $curPalette")
                    }
                }

                theta += thetaIncr
            }

        }
    }

}

/**
 * Adapted from 'orx-shade-styles': NPointLinearGradient
 *
 * - use radians instead of degrees for `p_rotation`
 * - change stroke color (`x_stroke)` instead of fill (`x_fill`)
 */
class NPointLinearStrokeGradient(
    colors: Array<ColorRGBa>,
    points: Array<Double> = Array(colors.size) { it / (colors.size - 1.0) },
    offset: Vector2 = Vector2.ZERO,
    rotation: Double = 0.0
) : ShadeStyle() {

    var colors: Array<ColorRGBa> by Parameter()

    // Sorted normalized values defining relative positions of colors
    var points: Array<Double> by Parameter()
    var offset: Vector2 by Parameter()
    var rotation: Double by Parameter()

    init {
        this.colors = colors
        this.points = points
        this.offset = offset
        this.rotation = rotation

        fragmentTransform = """
            vec2 coord = (c_boundsPosition.xy - 0.5 + p_offset);
            
            float cr = cos(p_rotation);
            float sr = sin(p_rotation);
            mat2 rm = mat2(cr, -sr, sr, cr);
            vec2 rc = rm * coord;
            float f = clamp(rc.y + 0.5, 0.0, 1.0);            
 
            int i = 0;
            while(i < p_points_SIZE - 1 && f >= p_points[i+1]) { i++; }
            
            vec4 color0 = p_colors[i];
            color0.rgb *= color0.a;

            vec4 color1 = p_colors[i+1]; 
            color1.rgb *= color1.a;
            
            float g = (f - p_points[i]) / (p_points[i+1] - p_points[i]);
            vec4 gradient = mix(color0, color1, clamp(g, 0.0, 1.0)); 

            vec4 fn = vec4(x_stroke.rgb, 1.0) * x_stroke.a;
            
            x_stroke = fn * gradient;
            if (x_stroke.a != 0) {
                x_stroke.rgb /= x_stroke.a;
            }

        """
    }
}
