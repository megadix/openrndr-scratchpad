package megadix.lab

import org.openrndr.application
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.perlinLinear
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.map

fun main() = application {

    configure {
        width = 800
        height = 600
    }

    oliveProgram {
        val gui = GUI()

        val settings = object {
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
                )
                val y = (
                        perlinLinear(settings.randomSeed, x * settings.amp)) * halfHeight + halfHeight
                drawer.point(i.toDouble(), y)
            }
        }
    }
}