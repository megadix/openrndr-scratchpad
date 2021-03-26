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
import org.openrndr.extra.midi.MidiEvent
import org.openrndr.extra.midi.MidiTransceiver
import org.openrndr.extra.noise.perlinQuintic
import org.openrndr.ffmpeg.MP4Profile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.mod
import org.openrndr.shape.contour
import org.openrndr.text.writer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.system.exitProcess

fun main() = application {
    val TWO_PI = PI * 2.0

    // parameters

    val static = false
    val debug = false
    val goFullscreen = false
    val recordVideo = false

    val minNum = 3
    val maxNum = 1800
    var num = 360
    val radiusSize = 0.5

    val randomSeed = 100

    val minThetaNoiseScale = 10.0
    val maxThetaNoiseScale = 30.0

    val minAnimationDuration = 100L
    val maxAnimationDuration = 10000L
    var animationDuration = 2000L
    val easing = Easing.SineInOut

    // controls how much angles can vary
    val minThetaNoiseAmp = 0.0
    val maxThetaNoiseAmp = PI * 2.0
    var thetaNoiseAmp = PI / 2.0

    // controls how much radius can vary
    val minRadiusNoiseAmp = 0.0
    val maxRadiusNoiseAmp = 500.0
    var radiusNoiseAmp = 100.0

    val minCenterNoiseAmp = 0.0
    val maxCenterNoiseAmp = 200.0
    var centerNoiseAmp = 30.0

    // NOTE: the following are MY default values for MIDI, plu-in your own to make it work or disable MIDI
    val midiEnabled = true

    val midiCc1 = 70
    val midiCc2 = 71
    val midiCc3 = 72
    val midiCc4 = 73
    val midiCc5 = 74
    val midiCc6 = 75
    val midiCc7 = 76
    val midiCc8 = 77

    val pad1note = 40
    val pad2note = 41
    val pad3note = 42
    val pad4note = 43
    val pad5note = 36
    val pad6note = 37
    val pad7note = 38
    val pad8note = 39

    val midiController: MidiTransceiver? = if (midiEnabled)
        MidiTransceiver.fromDeviceVendor(
            "MPK mini 3",
            "Unknown vendor"
        )
    else null

    /* ----------------------------------------
     * Main application
     * ---------------------------------------- */

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
            listOf("264653", "2a9d8f", "e9c46a", "f4a261", "e76f51"),
            listOf("000000", "14213d", "fca311", "e5e5e5", "ffffff"),
            listOf("d9ed92", "b5e48c", "99d98c", "76c893", "52b69a", "34a0a4", "168aad", "1a759f", "1e6091", "184e77"),
            listOf("ff0a54", "ff477e", "ff5c8a", "ff7096", "ff85a1", "ff99ac", "fbb1bd", "f9bec7", "f7cad0", "fae0e4"),
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

        if (midiEnabled) {
            fun newValue(evt: MidiEvent, min: Number, max: Number): Double {
                return map(
                    0.0, 127.0,
                    min.toDouble(), max.toDouble(),
                    evt.value.toDouble(),
                    true
                )
            }

            /*
             * Control Change (CC)
             */
            midiController!!.controlChanged.listen { evt ->
                when (evt.control) {
                    // num of lines using control change 1
                    midiCc1 -> num = newValue(evt, minNum, maxNum).toInt()
                    // thetaNoiseAmp using control change 5
                    midiCc5 -> thetaNoiseAmp = newValue(evt, maxThetaNoiseAmp, minThetaNoiseAmp)
                    // radiusNoiseAmp using control change 6
                    midiCc6 -> radiusNoiseAmp = newValue(evt, minRadiusNoiseAmp, maxRadiusNoiseAmp)
                    // centerNoiseAmp using control change 7
                    midiCc7 -> centerNoiseAmp = newValue(evt, minCenterNoiseAmp, maxCenterNoiseAmp)
                }
            }

            /*
             * Control Change (CC)
             */

            val paletteNotesMap = mapOf(
                pad1note to 0,
                pad2note to 1,
                pad3note to 2,
                pad4note to 3,
                pad5note to 4,
                pad6note to 5,
                pad7note to 6,
                pad8note to 7,
            )

            midiController.noteOn.listen { evt ->
                curPalette = paletteNotesMap.getOrDefault(evt.note, curPalette)
            }

            ended.listen {
                exitProcess(0)
            }

        } else {
            // cycle palettes using spacebar
            keyboard.keyDown.listen {
                if (it.key == KEY_SPACEBAR) {
                    curPalette = (curPalette + 1) % palettes.size
                }
            }
        }

        if (recordVideo) {
            extend(ScreenRecorder()) {
                profile = MP4Profile()
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
                animation.noiseParam,
                true
            )

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
