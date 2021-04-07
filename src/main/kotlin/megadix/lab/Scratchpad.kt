package megadix.lab

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity.SRGB
import org.openrndr.extra.olive.oliveProgram

/**
 *  This is a template for a live program.
 *
 *  It uses oliveProgram {} instead of program {}. All code inside the
 *  oliveProgram {} can be changed while the program is running.
 */

fun main() = application {
    configure {
        width = 800
        height = 800
    }
    oliveProgram {
        extend {
            drawer.fill = ColorRGBa(
                r = 0.9727129411119433,
                g = 0.9727129411119433,
                b = 0.9727129411119433,
                a = 1.0,
                linearity = SRGB
            )
            drawer.circle(401.0, 307.0, 5.0)

            drawer.fill = ColorRGBa(
                r = 0.6778457820519057,
                g = 0.6778457820519057,
                b = 0.6778457820519057,
                a = 1.0,
                linearity = SRGB
            )
            drawer.circle(542.0, 351.0, 5.0)

            drawer.fill = ColorRGBa(
                r = 0.5743241280941181,
                g = 0.5743241280941181,
                b = 0.5743241280941181,
                a = 1.0,
                linearity = SRGB
            )
            drawer.circle(754.0, 200.0, 5.0)
        }
    }
}