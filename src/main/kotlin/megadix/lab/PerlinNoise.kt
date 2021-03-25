package megadix.lab

import megadix.lab.PerlinVariant.*
import org.openrndr.application
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.perlinHermite
import org.openrndr.extra.noise.perlinLinear
import org.openrndr.extra.noise.perlinQuintic
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.parameters.OptionParameter
import org.openrndr.math.map

enum class PerlinVariant { Linear, Quintic, Hermite }

/**
 * Testing various 1D perlin noise types.
 */
fun main() = application {

    configure {
        width = 800
        height = 600
    }

    program {
        val gui = GUI()

        val settings = object {
            @OptionParameter("Perlin variant")
            var variant = Linear

            @IntParameter("Random Seed", 0, 100)
            var randomSeed: Int = 0

            @DoubleParameter("amp", 0.001, 100.0)
            var amp: Double = 1.0

        };

        gui.add(settings, "Settings")
        extend(gui)

        val halfHeight = height / 2.0

        extend {
            for (i in 0..width) {
                val x = map(
                    0.0, width.toDouble(),
                    0.0, 1.0,
                    i.toDouble()
                ) * settings.amp

                val y = when (settings.variant) {
                    Linear -> perlinLinear(settings.randomSeed, x)
                    Quintic -> perlinQuintic(settings.randomSeed, x)
                    Hermite -> perlinHermite(settings.randomSeed, x)
                } * height + halfHeight

                drawer.point(i.toDouble(), y)
            }
        }
    }
}