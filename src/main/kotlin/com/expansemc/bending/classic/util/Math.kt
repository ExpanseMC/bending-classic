package com.expansemc.bending.classic.util

import com.expansemc.bending.api.ray.FastRaycast
import com.expansemc.bending.api.util.step
import org.bukkit.Location
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin

fun getSphereDirections(thetaMin: Double, thetaMax: Double, angleTheta: Double, anglePhi: Double): Array<Vector> {
    val directions = ArrayList<Vector>()

    for (theta: Double in thetaMin..thetaMax step angleTheta) {
        val sinTheta: Double = sin(Math.toRadians(theta))
        val cosTheta: Double = cos(Math.toRadians(theta))

        val deltaPhi: Double = anglePhi / sinTheta

        for (phi: Double in 0.0..360.0 step deltaPhi) {
            val sinPhi: Double = sin(Math.toRadians(phi))
            val cosPhi: Double = cos(Math.toRadians(phi))

            directions += Vector(
                cosPhi * sinTheta,
                sinPhi * sinTheta,
                cosTheta
            )
        }
    }

    return directions.toTypedArray()
}

internal val ZERO_VECTOR = Vector(0, 0, 0)

fun createSphereRaycasts(
    origin: Location,
    directions: Array<Vector>,
    range: Double,
    speed: Double,
    targetDirection: Vector = ZERO_VECTOR,
    maxAngle: Double = 0.0
): List<FastRaycast> = directions.mapNotNull {
    if (maxAngle > 0.0 && it.angle(targetDirection) > maxAngle) {
        null
    } else {
        FastRaycast(
            origin = origin,
            direction = it,
            range = range,
            speed = speed,
            checkDiagonals = true
        )
    }
}