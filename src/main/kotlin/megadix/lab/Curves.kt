package megadix.lab

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2
import org.openrndr.shape.contour

/**
 *  This is a template for a live program.
 *
 *  It uses oliveProgram {} instead of program {}. All code inside the
 *  oliveProgram {} can be changed while the program is running.
 */

fun main() = application {

    configure {
        width = 800
        height = 600
    }

    oliveProgram {
        val points = mutableListOf(
            Vector2(0.0, 0.0),
            Vector2(width / 5.0, height / 10.0),
            Vector2(width / 2.0, height / 2.0),
            Vector2(width - width / 5.0, height - height / 10.0),
            Vector2(width.toDouble(), height.toDouble())
        )

        var dragging: Int = -1

        mouse.buttonDown.listen {
            dragging = points.indexOfFirst {
                it.distanceTo(mouse.position) < 5.0
            }
        }

        mouse.buttonUp.listen {
            dragging = -1
        }

        mouse.dragged.listen {
            if (dragging > -1) {
                points[dragging] = mouse.position
            }
        }

        extend {
            drawer.clear(ColorRGBa.BLACK)

            val tangentScale = 1.0

            drawer.stroke = ColorRGBa.GREEN

            drawer.contour(contour {
                moveTo(points[0])
                continueTo(points[1], tangentScale)
                continueTo(points[2], tangentScale)
                continueTo(points[3])
                continueTo(points[4])
            })

            points.forEach { drawer.circle(it, 5.0) }
        }
    }
}