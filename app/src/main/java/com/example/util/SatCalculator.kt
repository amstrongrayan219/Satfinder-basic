package com.example.util

import kotlin.math.*

object SatCalculator {
    const val R = 6371.0 // Earth radius in km
    const val h = 35786.0 // Geostationary altitude in km

    data class CalculationResult(
        val azimuth: Double,
        val elevation: Double,
        val polarization: Double
    )

    /**
     * Calculations based on User latitude/longitude and Satellite longitude in degrees.
     */
    fun calculate(userLatDeg: Double, userLonDeg: Double, satLonDeg: Double): CalculationResult {
        // φ = latitude utilisateur en radians
        val phi = Math.toRadians(userLatDeg)
        // λ = longitude utilisateur en radians
        val lambda = Math.toRadians(userLonDeg)
        // λs = longitude satellite en radians
        val lambdaS = Math.toRadians(satLonDeg)
        
        // B = λs - λ
        val B = lambdaS - lambda

        // --- AZIMUT ---
        // Az = 180 + atan(tan(B) / sin(φ))
        val tanB = tan(B)
        val sinPhi = sin(phi)
        
        // Prevent division by zero if on the equator exactly (sin(phi) = 0)
        val atanTerm = if (abs(sinPhi) < 1e-9) {
            // Equator handling
            if (B >= 0) {
                PI / 2.0
            } else {
                -PI / 2.0
            }
        } else {
            atan(tanB / sinPhi)
        }
        
        // Convert atan to degrees
        var Az = 180.0 + Math.toDegrees(atanTerm)
        
        // Si φ < 0 → Az = Az - 180
        if (phi < 0) {
            Az -= 180.0
        }
        
        // Si B < 0 → Az = Az + 360
        if (B < 0) {
            Az += 360.0
        }
        
        // Ensure within [0, 360) range
        Az = (Az % 360.0 + 360.0) % 360.0

        // --- ÉLÉVATION ---
        // r = r = sqrt(R²+(R+h)²- 2·R·(R+h)·cos(φ)·cos(B))
        // El = atan(((R+h)·cos(φ)·cos(B) - R) / r)
        val cosPhi = cos(phi)
        val cosB = cos(B)
        val R_plus_h = R + h
        val r = sqrt((R * R) + (R_plus_h * R_plus_h) - (2.0 * R * R_plus_h * cosPhi * cosB))
        
        val numerator = (R_plus_h * cosPhi * cosB) - R
        val ElRad = atan(numerator / r)
        val El = Math.toDegrees(ElRad)

        // --- POLARISATION LNB ---
        // Pol = atan(sin(B) / tan(φ))
        val sinB = sin(B)
        val tanPhi = tan(phi)
        
        val PolRad = if (abs(tanPhi) < 1e-9) {
            0.0 // Equator limit
        } else {
            atan(sinB / tanPhi)
        }
        val Pol = Math.toDegrees(PolRad)

        return CalculationResult(
            azimuth = Az,
            elevation = El,
            polarization = Pol
        )
    }
}
