package megadix.lab

import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment
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
            Vector2(width / 20.0, height / 20.0),
            Vector2(width / 5.0, height / 10.0),
            Vector2(width / 2.0, height / 2.0),
            Vector2(width - width / 5.0, height - height / 10.0),
            Vector2(width - width / 20.0, height - height / 20.0)
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

            drawer.stroke = ColorRGBa.YELLOW
            drawer.lineSegment(points[0], points[1])
            drawer.lineSegment(points[1], points[3])


            val segments = listOf(
                Segment(
                    points[0], // start
                    points[1], // control 1
                    points[2],  // end
                ),
                Segment(
                    points[3], // start
                    points[1], // control 1
                    points[4],  // end
                ),
                )

            drawer.stroke = ColorRGBa.GREEN
            drawer.segments(segments)

            points.forEach { drawer.circle(it, 5.0) }
        }
    }
}