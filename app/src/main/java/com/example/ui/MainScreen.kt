package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Satellite
import com.example.ui.theme.*
import com.example.util.Translation
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SatFinderViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    // Handle permissions
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fineGranted || coarseGranted
        if (hasLocationPermission) {
            viewModel.startTracking()
        }
    }

    // Start/Stop sensor tracking of phone along screen lifecyclees
    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.startTracking()
        }
        onDispose {
            viewModel.stopTracking()
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showLocationEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Translation.getString("app_title", language),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ElectricBlue
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSpaceBlack
                ),
                actions = {
                    // Quick stats / current location indicator
                    val isGpsActive by viewModel.isGpsActive.collectAsStateWithLifecycle()
                    val lat by viewModel.userLatitude.collectAsStateWithLifecycle()
                    val lon by viewModel.userLongitude.collectAsStateWithLifecycle()
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardDarkBlue)
                            .clickable { showLocationEditDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isGpsActive) Icons.Filled.MyLocation else Icons.Filled.Place,
                            contentDescription = "Pos",
                            tint = if (isGpsActive) EmeraldAliged else StarGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format("%.3f, %.3f", lat, lon),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = OffWhite
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DeepSpaceBlack,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Filled.FormatListBulleted, contentDescription = "List") },
                    label = { Text(Translation.getString("tab_list", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricBlue,
                        selectedTextColor = ElectricBlue,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText,
                        indicatorColor = SurfaceDarkBlue
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Filled.Explore, contentDescription = "Pointer") },
                    label = { Text(Translation.getString("tab_pointer", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricBlue,
                        selectedTextColor = ElectricBlue,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText,
                        indicatorColor = SurfaceDarkBlue
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text(Translation.getString("tab_settings", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricBlue,
                        selectedTextColor = ElectricBlue,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText,
                        indicatorColor = SurfaceDarkBlue
                    )
                )
            }
        },
        containerColor = DeepSpaceBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasLocationPermission) {
                // Request Permission UI block
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "GPS Lock",
                        tint = ElectricBlue,
                        modifier = Modifier
                            .size(72.dp)
                            .padding(bottom = 16.dp)
                    )
                    Text(
                        text = Translation.getString("location_perm_request", language),
                        textAlign = TextAlign.Center,
                        color = OffWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text(Translation.getString("location_perm_btn", language), color = Color.Black)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {
                        // Allow bypass permission and set manual position
                        hasLocationPermission = true
                        showLocationEditDialog = true
                    }) {
                        Text(Translation.getString("set_manual_pos", language), color = StarGold)
                    }
                }
            } else {
                // Active state
                when (currentTab) {
                    0 -> SatellitesListScreen(
                        viewModel = viewModel,
                        language = language,
                        onAddManualClicked = { showAddDialog = true }
                    )
                    1 -> PointingScreen(
                        viewModel = viewModel,
                        language = language
                    )
                    2 -> SettingsScreen(
                        viewModel = viewModel,
                        language = language
                    )
                }
            }

            // Dialog for adding manual satellite
            if (showAddDialog) {
                AddManualSatelliteDialog(
                    language = language,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, lon ->
                        viewModel.addManualSatellite(name, lon)
                        showAddDialog = false
                    }
                )
            }

            // Dialog for editing user location manually
            if (showLocationEditDialog) {
                EditUserLocationDialog(
                    viewModel = viewModel,
                    language = language,
                    onDismiss = { showLocationEditDialog = false }
                )
            }
        }
    }
}

// ---------------- LIST SCREEN ----------------
@Composable
fun SatellitesListScreen(
    viewModel: SatFinderViewModel,
    language: String,
    onAddManualClicked: () -> Unit
) {
    val satellites by viewModel.satellitesState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedSat by viewModel.selectedSatellite.collectAsStateWithLifecycle()
    val userLat by viewModel.userLatitude.collectAsStateWithLifecycle()
    val userLon by viewModel.userLongitude.collectAsStateWithLifecycle()

    val filteredSatellites = remember(satellites, searchQuery) {
        satellites.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.formattedLongitude.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search bar & Manual add button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(Translation.getString("search_hint", language), color = MutedText, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = ElectricBlue) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = GridLineColor,
                    focusedLabelColor = ElectricBlue,
                    unfocusedContainerColor = SurfaceDarkBlue,
                    focusedContainerColor = SurfaceDarkBlue
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onAddManualClicked,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, StarGold, RoundedCornerShape(12.dp))
                    .background(SurfaceDarkBlue)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Sat",
                    tint = StarGold,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Selected Satellite Status Card with exact calculations
        selectedSat?.let { sat ->
            val calcResult = com.example.util.SatCalculator.calculate(userLat, userLon, sat.longitude)
            
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                border = BorderStroke(1.dp, ElectricBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = sat.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = ElectricBlue
                            )
                            Text(
                                text = Translation.getString("satellite", language) + " (${sat.formattedLongitude})",
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }
                        
                        // Active target badge
                        IconButton(
                            onClick = { viewModel.selectTab(1) },
                            modifier = Modifier
                                .background(CardDarkBlue, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Explore,
                                contentDescription = "Aim",
                                tint = StarGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = GridLineColor)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CalculationBadge(
                            title = Translation.getString("azimuth", language),
                            value = String.format("%.2f°", calcResult.azimuth),
                            color = ElectricBlue
                        )
                        CalculationBadge(
                            title = Translation.getString("elevation", language),
                            value = String.format("%.2f°", calcResult.elevation),
                            color = StarGold
                        )
                        CalculationBadge(
                            title = Translation.getString("polarization", language),
                            value = String.format("%.2f°", calcResult.polarization),
                            color = EmeraldAliged
                        )
                    }
                }
            }
        }

        // Lazy List of satellites
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredSatellites) { sat ->
                val isSelected = selectedSat?.id == sat.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CardDarkBlue else SurfaceDarkBlue)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) ElectricBlue else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.selectSatellite(sat) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = sat.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isSelected) ElectricBlue else OffWhite
                        )
                        Text(
                            text = sat.formattedLongitude,
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (sat.isCustom) {
                            IconButton(
                                onClick = { viewModel.deleteSatellite(sat) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = SignalRed.copy(alpha = 0.8f)
                                )
                            }
                        }
                        
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.selectSatellite(sat) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ElectricBlue,
                                unselectedColor = GridLineColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculationBadge(title: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DeepSpaceBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = title, fontSize = 10.sp, color = MutedText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 16.sp, color = color, fontWeight = FontWeight.Black)
    }
}

// ---------------- POINTING SCREEN ----------------
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PointingScreen(viewModel: SatFinderViewModel, language: String) {
    val selectedSat by viewModel.selectedSatellite.collectAsStateWithLifecycle()
    val phoneAz by viewModel.phoneAzimuth.collectAsStateWithLifecycle()
    val phoneEl by viewModel.phoneElevation.collectAsStateWithLifecycle()
    val calcResult by viewModel.alignmentData.collectAsStateWithLifecycle()
    val isAligned by viewModel.isAligned.collectAsStateWithLifecycle()
    val azOffset by viewModel.azimuthOffset.collectAsStateWithLifecycle()
    val elOffset by viewModel.elevationOffset.collectAsStateWithLifecycle()

    val displayAzPhone = phoneAz + azOffset
    val displayElPhone = phoneEl + elOffset

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Satellite Info & Alignment Indicator Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDarkBlue),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, if (isAligned) EmeraldAliged else GridLineColor),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = selectedSat?.name ?: "---",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = ElectricBlue
                    )
                    Text(
                        text = if (isAligned) {
                            Translation.getString("alignment_success", language)
                        } else {
                            Translation.getString("alignment_hunting", language)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isAligned) EmeraldAliged else StarGold
                    )
                }
                
                // Audio aligned indicators
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (isAligned) EmeraldAliged else CardDarkBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAligned) Icons.Filled.Check else Icons.Filled.NetworkCheck,
                        contentDescription = "Status",
                        tint = if (isAligned) Color.Black else OffWhite
                    )
                }
            }
        }

        // animated Rotating Compass Layout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .border(2.dp, GridLineColor, CircleShape)
                    .background(SurfaceDarkBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Compas plate, rotating by negative phone azimuth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = -displayAzPhone.toFloat())
                ) {
                    // Let's draw standard letters N, O/W, S, E inside
                    Text(
                        text = Translation.getString("compass_north", language),
                        color = SignalRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                    )
                    Text(
                        text = Translation.getString("compass_south", language),
                        color = OffWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                    )
                    Text(
                        text = Translation.getString("compass_east", language),
                        color = OffWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                    )
                    Text(
                        text = Translation.getString("compass_west", language),
                        color = OffWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 6.dp)
                    )
                    
                    // Simple thin compass cross line
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = GridLineColor.copy(alpha = 0.5f),
                            start = Offset(size.width / 2, 22f),
                            end = Offset(size.width / 2, size.height - 22f),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = GridLineColor.copy(alpha = 0.5f),
                            start = Offset(22f, size.height / 2),
                            end = Offset(size.width - 22f, size.height / 2),
                            strokeWidth = 2f
                        )
                    }
                }
                
                // Fixed top indicator line
                Canvas(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.TopCenter)
                ) {
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width / 2, 0f)
                        lineTo(0f, size.height)
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(p, color = ElectricBlue)
                }
                
                // Digital angle text inside compass center
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%d°", displayAzPhone.roundToInt()),
                        color = OffWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Radar/Repère orthonormal UI Canvas
        radarOrthonormalCanvas(
            phoneAz = displayAzPhone,
            phoneEl = displayElPhone,
            targetAz = calcResult?.azimuth ?: 0.0,
            targetEl = calcResult?.elevation ?: 0.0,
            isAligned = isAligned
        )

        // Guide Helper notes bottom section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Translation.getString("instr_pointing", language),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MutedText,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(StarGold, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Satellite Target (0,0)", fontSize = 10.sp, color = MutedText)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if (isAligned) EmeraldAliged else ElectricBlue, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Your Device", fontSize = 10.sp, color = MutedText)
                }
            }
        }
    }
}

@Composable
fun radarOrthonormalCanvas(
    phoneAz: Double,
    phoneEl: Double,
    targetAz: Double,
    targetEl: Double,
    isAligned: Boolean
) {
    // We compute differences with angle wrappings
    var diffAz = targetAz - phoneAz
    // Minimal wrap-around angle diff [-180, 180)
    diffAz = (diffAz + 180.0) % 360.0 - 180.0
    if (diffAz < -180.0) diffAz += 360.0

    val diffEl = targetEl - phoneEl

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(260.dp)
            .border(2.dp, GridLineColor, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SurfaceDarkBlue,
                        DeepSpaceBlack
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.width / 2f
            
            // scale: total visual width on screen spans 45 degrees
            // scale = width_pixels / 45°
            val scale = size.width / 45f

            // 1. Draw grid circles representing 10° and 20° concentric references
            val strokeThin = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            drawCircle(color = GridLineColor.copy(alpha = 0.3f), radius = 10f * scale, style = strokeThin)
            drawCircle(color = GridLineColor.copy(alpha = 0.2f), radius = 20f * scale, style = strokeThin)

            // Inner exact alignment area target (0.5 degree target zone)
            drawCircle(
                color = if (isAligned) EmeraldAliged.copy(alpha = 0.25f) else StarGold.copy(alpha = 0.15f),
                radius = 0.5f * scale,
                style = Stroke(width = 2f)
            )

            // 2. Draw standard grid crosses (x and y axis)
            drawLine(
                color = GridLineColor.copy(alpha = 0.5f),
                start = Offset(0f, cy),
                end = Offset(size.width, cy),
                strokeWidth = 1.5f
            )
            drawLine(
                color = GridLineColor.copy(alpha = 0.5f),
                start = Offset(cx, 0f),
                end = Offset(cx, size.height),
                strokeWidth = 1.5f
            )

            // 3. Draw fixed Yellow target dot at (0,0) (representing satellite)
            drawCircle(
                color = StarGold,
                radius = 12f,
                center = Offset(cx, cy)
            )
            // Precise crosshair inside the yellow dot
            drawLine(color = Color.Black, start = Offset(cx - 8f, cy), end = Offset(cx + 8f, cy), strokeWidth = 2f)
            drawLine(color = Color.Black, start = Offset(cx, cy - 8f), end = Offset(cx, cy + 8f), strokeWidth = 2f)

            // 4. Calculate coordinates for the floating blue dot
            // X_draw = User's phone azimuth. The user prompt formulas:
            // X_bleu = (Az_satellite - Az_téléphone) × échelle
            // Y_bleu = (El_satellite - El_téléphone) × échelle
            // Since dx = diffAz (which is targetAz - phoneAz), we have:
            // xVal = cx + diffAz * scale
            // To make upwards on sky represent elevation decrease, we subtract dy elevation
            // yVal = cy - diffEl * scale
            val xOffset = diffAz.toFloat() * scale
            val yOffset = -diffEl.toFloat() * scale // negative so positive elevation offset positions blue dot above yellow cross

            // Clamp coordinates so the dot is never lost outside the circular radar boundary!
            val rawOffsetLength = sqrt(xOffset * xOffset + yOffset * yOffset)
            val maxAllowedRadius = radius - 18f
            
            val (finalXOffset, finalYOffset) = if (rawOffsetLength > maxAllowedRadius) {
                val ratio = maxAllowedRadius / rawOffsetLength
                xOffset * ratio to yOffset * ratio
            } else {
                xOffset to yOffset
            }

            val finalDotPosition = Offset(cx + finalXOffset, cy + finalYOffset)

            // Aligned visual success pulse
            if (isAligned) {
                drawCircle(
                    color = EmeraldAliged.copy(alpha = pulseAlpha),
                    radius = 35f,
                    center = finalDotPosition
                )
            }

            // Draw floating phone dot (Blue normally, Green when aligned < 0.5°)
            drawCircle(
                color = if (isAligned) EmeraldAliged else ElectricBlue,
                radius = 14f,
                center = finalDotPosition
            )
            
            // Draw secondary little center circle inside the float dot
            drawCircle(
                color = Color.Black,
                radius = 4f,
                center = finalDotPosition
            )

            // Draw line connector from target to phone if reasonably close
            if (rawOffsetLength < maxAllowedRadius && rawOffsetLength > 15f) {
                drawLine(
                    color = (if (isAligned) EmeraldAliged else ElectricBlue).copy(alpha = 0.4f),
                    start = Offset(cx, cy),
                    end = finalDotPosition,
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }
        }
    }
}

// ---------------- SETTINGS SCREEN ----------------
@Composable
fun SettingsScreen(viewModel: SatFinderViewModel, language: String) {
    val azOffset by viewModel.azimuthOffset.collectAsStateWithLifecycle()
    val elOffset by viewModel.elevationOffset.collectAsStateWithLifecycle()
    val phoneAz by viewModel.phoneAzimuth.collectAsStateWithLifecycle()
    val phoneEl by viewModel.phoneElevation.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Real-Time Sensor Inspection panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Translation.getString("phone_header", language),
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Azimuth Phone (Filter)", fontSize = 11.sp, color = MutedText)
                            Text(String.format("%.1f°", phoneAz), fontSize = 20.sp, color = OffWhite, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Elevation Phone (Filter)", fontSize = 11.sp, color = MutedText)
                            Text(String.format("%.1f°", phoneEl), fontSize = 20.sp, color = OffWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = GridLineColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Azimuth Adjusted", fontSize = 11.sp, color = MutedText)
                            Text(String.format("%.1f°", phoneAz + azOffset), fontSize = 20.sp, color = StarGold, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Elevation Adjusted", fontSize = 11.sp, color = MutedText)
                            Text(String.format("%.1f°", phoneEl + elOffset), fontSize = 20.sp, color = StarGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Manual Calibration of Compass/Pitch Offsets
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Translation.getString("header_offsets", language),
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Azimuth offset slider [-90 to 90 degrees]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Translation.getString("azimuth", language) + " Offset", fontSize = 13.sp, color = OffWhite)
                        Text(String.format("%+.1f°", azOffset), fontWeight = FontWeight.Bold, color = StarGold)
                    }
                    Slider(
                        value = azOffset.toFloat(),
                        onValueChange = { viewModel.updateCalibrationOffsets(it.toDouble(), elOffset) },
                        valueRange = -90f..90f,
                        colors = SliderDefaults.colors(
                            thumbColor = StarGold,
                            activeTrackColor = StarGold,
                            inactiveTrackColor = GridLineColor
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Elevation offset slider [-45 to 45 degrees]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Translation.getString("elevation", language) + " Offset", fontSize = 13.sp, color = OffWhite)
                        Text(String.format("%+.1f°", elOffset), fontWeight = FontWeight.Bold, color = StarGold)
                    }
                    Slider(
                        value = elOffset.toFloat(),
                        onValueChange = { viewModel.updateCalibrationOffsets(azOffset, it.toDouble()) },
                        valueRange = -45f..45f,
                        colors = SliderDefaults.colors(
                            thumbColor = StarGold,
                            activeTrackColor = StarGold,
                            inactiveTrackColor = GridLineColor
                        )
                    )
                }
            }
        }

        // Language selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Translation.getString("header_lang", language),
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setLanguage("en") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (language == "en") CardDarkBlue else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (language == "en") ElectricBlue else GridLineColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("English", color = if (language == "en") ElectricBlue else OffWhite)
                        }
                        OutlinedButton(
                            onClick = { viewModel.setLanguage("fr") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (language == "fr") CardDarkBlue else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (language == "fr") ElectricBlue else GridLineColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Français", color = if (language == "fr") ElectricBlue else OffWhite)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- DIALOGS ----------------
@Composable
fun AddManualSatelliteDialog(
    language: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var longitudeStr by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDarkBlue),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Translation.getString("dialog_add_title", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ElectricBlue
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Translation.getString("field_name", language), color = MutedText) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = GridLineColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = longitudeStr,
                    onValueChange = { 
                        longitudeStr = it
                        hasError = false
                    },
                    label = { Text(Translation.getString("field_longitude", language), color = MutedText, fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hasError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = GridLineColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (hasError) {
                    Text("Veuillez entrer une longitude correcte.", color = SignalRed, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Translation.getString("btn_cancel", language), color = MutedText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val lon = longitudeStr.toDoubleOrNull()
                            if (name.isNotBlank() && lon != null && lon >= -180.0 && lon <= 180.0) {
                                onConfirm(name, lon)
                            } else {
                                hasError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text(Translation.getString("btn_add", language), color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun EditUserLocationDialog(
    viewModel: SatFinderViewModel,
    language: String,
    onDismiss: () -> Unit
) {
    val initialLat by viewModel.userLatitude.collectAsStateWithLifecycle()
    val initialLon by viewModel.userLongitude.collectAsStateWithLifecycle()

    var latStr by remember { mutableStateOf(initialLat.toString()) }
    var lonStr by remember { mutableStateOf(initialLon.toString()) }
    var error by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDarkBlue),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Translation.getString("user_pos", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ElectricBlue
                )
                
                OutlinedTextField(
                    value = latStr,
                    onValueChange = { 
                        latStr = it
                        error = false
                    },
                    label = { Text("Latitude (e.g. 48.8566)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = GridLineColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = lonStr,
                    onValueChange = { 
                        lonStr = it
                        error = false
                    },
                    label = { Text("Longitude (e.g. 2.3522)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = GridLineColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (error) {
                    Text("Valeurs invalides. Veuillez entrer des coordonnées réelles.", color = SignalRed, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Translation.getString("btn_cancel", language), color = MutedText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val latVal = latStr.toDoubleOrNull()
                            val lonVal = lonStr.toDoubleOrNull()
                            if (latVal != null && lonVal != null && latVal >= -90.0 && latVal <= 90.0 && lonVal >= -180.0 && lonVal <= 180.0) {
                                viewModel.updateManualPosition(latVal, lonVal)
                                onDismiss()
                            } else {
                                error = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Save", color = Color.Black)
                    }
                }
            }
        }
    }
}
