package megadix.lab

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment

/**
 * Playing with curves.
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
            drawer.lineSegment(points[2], points[3])

            val segments = listOf(
                Segment(
                    points[0],
                    points[1],
                    points[2],
                    points[3]
                ),
                )

            drawer.stroke = ColorRGBa.GREEN
            drawer.segments(segments)

            points.forEach { drawer.circle(it, 5.0) }
        }
    }
}