package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Satellite
import com.example.data.SatelliteRepository
import com.example.util.SatCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.*

class SatFinderViewModel(
    application: Application,
    private val repository: SatelliteRepository
) : AndroidViewModel(application), SensorEventListener, LocationListener {

    private val context = application.applicationContext

    // Room Satellites State
    val satellitesState: StateFlow<List<Satellite>> = repository.allSatellites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedSatellite = MutableStateFlow<Satellite?>(null)
    val selectedSatellite = _selectedSatellite.asStateFlow()

    // Screen selection (Tabs: 1-List, 2-Pointing, 3-Settings)
    private val _currentTab = MutableStateFlow(0)
    val currentTab = _currentTab.asStateFlow()

    // Coordinates (Paris default)
    private val _userLatitude = MutableStateFlow(48.8566)
    val userLatitude = _userLatitude.asStateFlow()

    private val _userLongitude = MutableStateFlow(2.3522)
    val userLongitude = _userLongitude.asStateFlow()

    private val _isGpsActive = MutableStateFlow(false)
    val isGpsActive = _isGpsActive.asStateFlow()

    // Phone Sensors
    private val _phoneAzimuth = MutableStateFlow(0.0)
    val phoneAzimuth = _phoneAzimuth.asStateFlow()

    private val _phoneElevation = MutableStateFlow(0.0)
    val phoneElevation = _phoneElevation.asStateFlow()

    // Config & Settings
    private val _azimuthOffset = MutableStateFlow(0.0)
    val azimuthOffset = _azimuthOffset.asStateFlow()

    private val _elevationOffset = MutableStateFlow(0.0)
    val elevationOffset = _elevationOffset.asStateFlow()

    private val _language = MutableStateFlow("fr") // "fr" or "en"
    val language = _language.asStateFlow()

    // Alignment triggers
    private val _isAligned = MutableStateFlow(false)
    val isAligned = _isAligned.asStateFlow()

    // Calculation result for the selected satellite
    private val _alignmentData = MutableStateFlow<SatCalculator.CalculationResult?>(null)
    val alignmentData = _alignmentData.asStateFlow()

    // Low-Pass Filter configuration (alpha = 0.97)
    private val alpha = 0.97
    private var filteredSin: Double? = null
    private var filteredCos: Double? = null
    private var filteredEl: Double? = null

    // System Managers
    private var sensorManager: SensorManager? = null
    private var locationManager: LocationManager? = null
    private var vibrator: Vibrator? = null

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private var hasAccelerometer = false
    private var hasMagnetometer = false

    private var toneGenerator: ToneGenerator? = null

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            toneGenerator = null
        }

        // Auto-select first satellite when data is loaded
        viewModelScope.launch {
            satellitesState.collect { list ->
                if (_selectedSatellite.value == null && list.isNotEmpty()) {
                    _selectedSatellite.value = list.first()
                    recalculate()
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectSatellite(satellite: Satellite) {
        _selectedSatellite.value = satellite
        recalculate()
    }

    fun addManualSatellite(name: String, longitude: Double) {
        viewModelScope.launch {
            val sat = Satellite(name = name, longitude = longitude, isCustom = true)
            repository.insert(sat)
            // Immediately select the newly created satellite
            _selectedSatellite.value = sat
            recalculate()
        }
    }

    fun deleteSatellite(satellite: Satellite) {
        viewModelScope.launch {
            repository.delete(satellite)
            if (_selectedSatellite.value?.id == satellite.id) {
                _selectedSatellite.value = satellitesState.value.firstOrNull { it.id != satellite.id }
                recalculate()
            }
        }
    }

    fun updateManualPosition(lat: Double, lon: Double) {
        _userLatitude.value = lat
        _userLongitude.value = lon
        _isGpsActive.value = false // turned off true GPS status for manual override
        recalculate()
    }

    fun updateCalibrationOffsets(azOffset: Double, elOffset: Double) {
        _azimuthOffset.value = azOffset
        _elevationOffset.value = elOffset
        recalculate()
    }

    fun setLanguage(lang: String) {
        _language.value = lang
    }

    // Mathematical Recalculation
    fun recalculate() {
        val sat = _selectedSatellite.value ?: return
        val result = SatCalculator.calculate(
            _userLatitude.value,
            _userLongitude.value,
            sat.longitude
        )
        _alignmentData.value = result
        checkAlignment()
    }

    // Alignment checker
    private fun checkAlignment() {
        val calc = _alignmentData.value ?: return
        
        // Minimal difference with angle wrap-around
        var diffAz = calc.azimuth - (_phoneAzimuth.value + _azimuthOffset.value)
        diffAz = (diffAz + 180.0) % 360.0 - 180.0
        if (diffAz < -180.0) diffAz += 360.0

        val diffEl = calc.elevation - (_phoneElevation.value + _elevationOffset.value)
        
        val totalDistance = sqrt(diffAz * diffAz + diffEl * diffEl)
        val aligned = totalDistance < 0.5

        if (aligned && !_isAligned.value) {
            triggerFeedback()
        }
        _isAligned.value = aligned
    }

    private fun triggerFeedback() {
        // Trigger Vibration
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(400)
            }
        } catch (e: Exception) {
            // Safe fallback inside sandboxes
        }

        // Play Beep Tone
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        } catch (e: Exception) {
            // Fail gracefully
        }
    }

    // Life-cycle bound registration
    @SuppressLint("MissingPermission")
    fun startTracking() {
        // Registers sensors listeners
        sensorManager?.registerListener(
            this,
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI
        )
        sensorManager?.registerListener(
            this,
            sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_UI
        )

        // Register GPS listener if permission available
        try {
            val hasGps = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val hasNetwork = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            
            if (hasGps) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L,
                    10f,
                    this
                )
                locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { onLocationChanged(it) }
            } else if (hasNetwork) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    10f,
                    this
                )
                locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { onLocationChanged(it) }
            }
        } catch (e: SecurityException) {
            // Permission not granted yet, will prompt in view
        }
    }

    fun stopTracking() {
        sensorManager?.unregisterListener(this)
        locationManager?.removeUpdates(this)
    }

    // --- SensorEventListener Interfaces ---
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            hasAccelerometer = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            hasMagnetometer = true
        }

        if (hasAccelerometer && hasMagnetometer) {
            val rotationMatrix = FloatArray(9)
            val inclinationMatrix = FloatArray(9)
            val success = SensorManager.getRotationMatrix(
                rotationMatrix,
                inclinationMatrix,
                accelerometerReading,
                magnetometerReading
            )
            
            if (success) {
                val orientationAngles = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                
                // AZIMUTH (from -PI to PI radians)
                var rawAzDeg = Math.toDegrees(orientationAngles[0].toDouble())
                if (rawAzDeg < 0.0) rawAzDeg += 360.0

                // Filter Azimuth using Sine and Cosine components
                val rawAzRad = Math.toRadians(rawAzDeg)
                val s = sin(rawAzRad)
                val c = cos(rawAzRad)
                
                filteredSin = filteredSin?.let { it * alpha + s * (1.0 - alpha) } ?: s
                filteredCos = filteredCos?.let { it * alpha + c * (1.0 - alpha) } ?: c
                
                var finalAz = Math.toDegrees(atan2(filteredSin!!, filteredCos!!))
                if (finalAz < 0.0) finalAz += 360.0
                _phoneAzimuth.value = finalAz

                // ELEVATION
                // User requirement: Elevation_tel = arcsin(accelerometre_Z)
                // Filtered to low-pass α = 0.97
                val rawZComp = accelerometerReading[2].toDouble()
                val gravityFactor = 9.80665 // Standard Gravity (m/s^2)
                val normalizedZ = (rawZComp / gravityFactor).coerceIn(-1.0, 1.0)
                val rawElDeg = Math.toDegrees(asin(normalizedZ))
                
                // Filter Elevation
                filteredEl = filteredEl?.let { it * alpha + rawElDeg * (1.0 - alpha) } ?: rawElDeg
                _phoneElevation.value = filteredEl!!

                // Real-time alignment recomputes
                recalculate()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- LocationListener Interfaces ---
    override fun onLocationChanged(location: Location) {
        _userLatitude.value = location.latitude
        _userLongitude.value = location.longitude
        _isGpsActive.value = true
        recalculate()
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    override fun onCleared() {
        super.onCleared()
        stopTracking()
        toneGenerator?.release()
    }
}

class SatFinderViewModelFactory(
    private val application: Application,
    private val repository: SatelliteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SatFinderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SatFinderViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
