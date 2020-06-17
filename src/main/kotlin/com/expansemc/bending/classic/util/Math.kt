package com.expansemc.bending.classic.util

import com.expansemc.bending.api.ray.FastRaycast
import com.expansemc.bending.api.util.step
import org.bukkit.Location
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin

fun getSphereDirections(thetaMin: Int, thetaMax: Int, angleTheta: Int, anglePhi: Int): Array<Vector> {
    val directions = ArrayList<Vector>()

    for (theta: Int in thetaMin..thetaMax step angleTheta) {
        val thetaRad: Double = Math.toRadians(theta.toDouble())
        val sinTheta: Double = sin(thetaRad)
        val cosTheta: Double = cos(thetaRad)

        val deltaPhi: Double = anglePhi / sinTheta

        for (phi: Double in 0.0..360.0 step deltaPhi) {
            val phiRad: Double = Math.toRadians(phi)
            val sinPhi: Double = sin(phiRad)
            val cosPhi: Double = cos(phiRad)

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