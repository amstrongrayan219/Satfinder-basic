package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

class SatelliteRepository(private val satelliteDao: SatelliteDao) {
    val allSatellites: Flow<List<Satellite>> = satelliteDao.getAllSatellites()
        .onStart {
            // Safe fallback: double check seeding if databases are empty
            ensureSeeded()
        }

    suspend fun insert(satellite: Satellite) {
        satelliteDao.insertSatellite(satellite)
    }

    suspend fun delete(satellite: Satellite) {
        satelliteDao.deleteSatellite(satellite)
    }

    private suspend fun ensureSeeded() {
        if (satelliteDao.getCount() == 0) {
            val defaults = listOf(
                Satellite(name = "Eutelsat 7A", longitude = 7.0),
                Satellite(name = "Eutelsat 7B", longitude = 7.0),
                Satellite(name = "Eutelsat 16A", longitude = 16.0),
                Satellite(name = "Eutelsat 36B", longitude = 36.0),
                Satellite(name = "Eutelsat 36C", longitude = 36.0),
                Satellite(name = "Eutelsat 9A", longitude = 9.0),
                Satellite(name = "Eutelsat 21B", longitude = 21.5),
                Satellite(name = "Eutelsat 25B", longitude = 25.5),
                Satellite(name = "Intelsat 20", longitude = 68.5),
                Satellite(name = "Intelsat 17", longitude = 66.0),
                Satellite(name = "Intelsat 907", longitude = -27.5),
                Satellite(name = "Intelsat 901", longitude = -27.5),
                Satellite(name = "Intelsat 37e", longitude = -18.0),
                Satellite(name = "Nilesat 201", longitude = -7.0),
                Satellite(name = "Rascom QAF1R", longitude = 2.9),
                Satellite(name = "SES 4", longitude = -22.0),
                Satellite(name = "SES 5", longitude = 5.0),
                Satellite(name = "Hotbird 13B", longitude = 13.0),
                Satellite(name = "Hotbird 13C", longitude = 13.0),
                Satellite(name = "Astra 19.2E", longitude = 19.2),
                Satellite(name = "Astra 28.2E", longitude = 28.2),
                Satellite(name = "Arabsat 5C", longitude = 20.0),
                Satellite(name = "Arabsat 6A", longitude = 26.0),
                Satellite(name = "NSS 12", longitude = 57.0),
                Satellite(name = "Telstar 11N", longitude = -37.5)
            )
            for (sat in defaults) {
                satelliteDao.insertSatellite(sat)
            }
        }
    }
}
