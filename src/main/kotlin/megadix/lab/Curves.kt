package megadix.lab

import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2
import org.openrndr.shape.contour

/**
 * Playing with curves.
 */

fun main() = application {

    configure {
        width = 800
        height = 600
    }

    oliveProgram {
        val gui = GUI()

        val settings = object {
            @DoubleParameter("tangentScale1", 0.001, 5.0)
            var tangentScale1: Double = 1.0

            @DoubleParameter("tangentScale2", 0.001, 5.0)
            var tangentScale2: Double = 1.0

            @DoubleParameter("tangentScale3", 0.001, 5.0)
            var tangentScale3: Double = 1.0
        };

        gui.add(settings, "Settings")
        extend(gui)

        keyboard.keyDown.listen {
            if (it.key == KEY_SPACEBAR) {
                gui.visible = !gui.visible
            }

        }

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

            drawer.stroke = ColorRGBa.GREEN

            val c = contour {
                moveTo(points[0])
                curveTo(points[1], points[2])
                continueTo(points[3], points[4])
            }

            drawer.contour(c)

            points.forEach { drawer.circle(it, 5.0) }

            val controlPoints = c.segments.flatMap { it.control.asIterable() }
            drawer.stroke = ColorRGBa.RED
            controlPoints.forEach { drawer.rectangle(it, 5.0, 5.0) }
        }
    }
}