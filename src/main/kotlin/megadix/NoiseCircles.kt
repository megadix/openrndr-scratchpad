package megadix

import mu.KotlinLogging
import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.midi.MidiEvent
import org.openrndr.extra.midi.MidiTransceiver
import org.openrndr.extra.noise.perlin
import org.openrndr.math.map
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.system.exitProcess

fun main() = application {
    val logger = KotlinLogging.logger {}

    // arc per second
    val minThetaSpeed = 0.0
    val maxThetaSpeed = PI * 4
    var thetaSpeed = PI / 8.0

    val randomSeed = 111
    val numCirclesX = 20
    val numCirclesY = 15

    var radiusNoiseAmp = 1.2

    val minMaxRadiusRatio = 0.2
    val maxMaxRadiusRatio = 2.0
    var maxRadiusRatio = 0.5

    var minColorNoiseAmp = 0.2
    var maxColorNoiseAmp = 5.0
    var colorNoiseAmp = 1.2

    val minDisplacementNoise = 0.0
    val maxDisplacementNoise = 1.0
    var displacementNoise = 1.0

    val midiEnabled = true
//    val midiDeviceName = "MPK mini 3"
    val midiDeviceName = "loopMIDI Port 1"
    val midiDeviceVendor = "Unknown vendor"

    val midiCCChannel = 0
    val midiPadsChannel = 9

    val midiCc1 = 70
    val midiCc2 = 71
    val midiCc3 = 72
    val midiCc4 = 73

    val palettes = listOf(
        listOf("0b090a", "161a1d", "660708", "a4161a", "ba181b", "e5383b", "b1a7a6", "d3d3d3", "f5f3f4", "ffffff"),
        listOf("335c67", "fff3b0", "e09f3e", "9e2a2b", "540b0e"),
        listOf("ff0000", "aa0000", "0ff000", "00ff00", "000ff0", "0000ff", "0000ff"),
        listOf("9b5de5", "f15bb5", "fee440", "00bbf9", "00f5d4"),
    ).map { colors ->
        colors.map { color -> ColorRGBa.fromHex(color) }
    }

    val paletteNotesMap = mapOf(
        40 to palettes[0],
        41 to palettes[1],
        42 to palettes[2],
        43 to palettes[3],
    )
    var palette = palettes[0]

    val midiController: MidiTransceiver? = if (midiEnabled)
        MidiTransceiver.fromDeviceVendor(midiDeviceName, midiDeviceVendor)
    else null

    configure {
        width = 800
        height = 600
    }

    program {
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
                logger.debug { "Received MIDI CC event: $evt" }

                if (evt.channel == midiCCChannel) {
                    when (evt.control) {
                        midiCc1 -> thetaSpeed = newValue(evt, minThetaSpeed, maxThetaSpeed)
                        midiCc2 -> displacementNoise = newValue(evt, minDisplacementNoise, maxDisplacementNoise)
                        midiCc3 -> colorNoiseAmp = newValue(evt, minColorNoiseAmp, maxColorNoiseAmp)
                        midiCc4 -> maxRadiusRatio = newValue(evt, minMaxRadiusRatio, maxMaxRadiusRatio)
                    }
                }
            }

            /*
             * MIDI Note On
             */

            midiController.noteOn.listen { evt ->
                logger.debug { "Received MIDI Note On event: $evt" }
                if (evt.channel == midiPadsChannel) {
                    palette = paletteNotesMap.getOrDefault(evt.note, palette)
                    logger.debug { "changed palette to: ${palette}" }
                }
            }

            ended.listen {
                exitProcess(0)
            }

        } else {
            // cycle random palettes using spacebar
            keyboard.keyDown.listen {
                if (it.key == KEY_SPACEBAR) {
                    palette = palettes.random()
                }
            }
        }

        var theta = 0.0

        extend {
            val distX = width.toDouble() / (numCirclesX + 1)
            val distY = height.toDouble() / (numCirclesY + 1)
            val maxRadius = min(distX, distY) * maxRadiusRatio
            val minRadius = maxRadius / 20.0

            drawer.stroke = null

            for (i in 1..numCirclesX) {
                for (j in 1..numCirclesY) {
                    val x = i * distX
                    val y = j * distY

                    val xyNoise = perlin(
                        randomSeed,
                        sin(theta * x / 300.0),
                        cos(theta * y / 300.0),
                    ) * displacementNoise * maxRadius * 2.0

                    val radiusNoise = perlin(
                        randomSeed,
                        sin(x + theta) * radiusNoiseAmp,
                        cos(y + theta) * radiusNoiseAmp
                    )
                    val radius = map(
                        -2.0, 2.0,
                        minRadius, maxRadius,
                        cos(x + theta) + sin(theta) * radiusNoise
                    )

                    val colorIdx = map(
                        -1.0, 1.0,
                        0.0, palette.size.toDouble(),
                        perlin(
                            randomSeed,
                            sin((x + theta) / PI) * colorNoiseAmp,
                            sin((y + theta) / PI) * colorNoiseAmp,
                        )
                    )

                    val color = palette[(colorIdx).toInt()]
                    with(drawer) {
                        val alpha = map(minRadius, maxRadius, 1.0, 0.2, radius)
                        fill = color.opacify(alpha)
                        circle(x + xyNoise, y + xyNoise, radius)
                    }
                }
            }

            theta += thetaSpeed * deltaTime
        }
    }
}
