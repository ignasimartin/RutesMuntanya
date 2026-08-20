package com.exemple.rutesmuntanya

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.OpenableColumns
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.exemple.rutesmuntanya.databinding.ActivityMainBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var map: MapView

    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var headingArrowOverlay: HeadingArrowOverlay? = null
    private var routeArrowsOverlay: RouteArrowsOverlay? = null
    private val gradientSegments = ArrayList<Polyline>()
    private var routeBoundingBox: BoundingBox? = null

    // Perfil d'altitud i selecció de punts
    private var routePoints: List<GeoPoint>? = null
    private var routeHasElevation = false
    private var selectionMarker: Marker? = null

    // Sensor d'orientació (brúixola)
    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var currentHeading = 0f

    // Capes de mapa
    private val topoSource: ITileSource = XYTileSource(
        "OpenTopoMap", 0, 17, 256, ".png",
        arrayOf(
            "https://a.tile.opentopomap.org/",
            "https://b.tile.opentopomap.org/",
            "https://c.tile.opentopomap.org/"
        ),
        "© OpenTopoMap (CC-BY-SA), dades © OpenStreetMap contributors"
    )
    private val satelliteSource: ITileSource by lazy { createEsriSatelliteSource() }
    private var usingSatellite = false

    private val downloadZoomMin = 12
    private val downloadZoomMax = 16

    private var progressDialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null

    private val openGpxLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { loadGpx(it) }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) enableMyLocation()
            else toast("Sense permís d'ubicació no puc mostrar on ets.")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        map = binding.map
        map.setTileSource(topoSource)
        map.setMultiTouchControls(true)
        // Amaga els botons +/- de zoom.
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.controller.setZoom(14.0)
        map.controller.setCenter(GeoPoint(42.5766, 1.6014))

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        binding.elevationProfile.onPointSelected = { index -> onProfilePointSelected(index) }

        setupButtons()
        ensureLocationPermission()
    }

    private fun setupButtons() {
        binding.btnLoad.setOnClickListener {
            openGpxLauncher.launch(
                arrayOf(
                    "application/gpx+xml",
                    "application/xml",
                    "text/xml",
                    "application/octet-stream",
                    "*/*"
                )
            )
        }
        binding.btnLayer.setOnClickListener { toggleLayer() }
        binding.btnProfile.setOnClickListener { toggleProfile() }
        binding.btnDownload.setOnClickListener { confirmDownload() }
        binding.btnCenter.setOnClickListener { centerOnMe() }
    }

    // ---------------- Perfil d'altitud ----------------

    private fun toggleProfile() {
        if (!routeHasElevation) {
            toast("Carrega una ruta amb dades d'altitud per veure'n el perfil.")
            return
        }
        binding.elevationProfile.visibility =
            if (binding.elevationProfile.visibility == TextView.VISIBLE) TextView.GONE
            else TextView.VISIBLE
    }

    private fun onProfilePointSelected(index: Int) {
        val pts = routePoints ?: return
        if (index < 0 || index >= pts.size) return
        val gp = pts[index]
        val marker = selectionMarker ?: createSelectionMarker().also {
            selectionMarker = it
            map.overlays.add(it)
        }
        marker.position = gp
        reAddTopOverlays()
        map.controller.setCenter(gp)
        map.invalidate()
    }

    private fun createSelectionMarker(): Marker {
        val marker = Marker(map)
        marker.icon = BitmapDrawable(
            resources,
            Graphics.selectionDot(resources.displayMetrics.density)
        )
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.setInfoWindow(null)
        marker.setOnMarkerClickListener { _, _ -> true } // sense finestreta emergent
        return marker
    }

    // ---------------- Capes ----------------

    private fun toggleLayer() {
        usingSatellite = !usingSatellite
        map.setTileSource(if (usingSatellite) satelliteSource else topoSource)
        binding.btnLayer.text = if (usingSatellite) getString(R.string.layer_topo)
        else getString(R.string.layer_satellite)
        map.invalidate()
    }

    private fun createEsriSatelliteSource(): OnlineTileSourceBase {
        return object : OnlineTileSourceBase(
            "EsriWorldImagery", 0, 19, 256, "",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                return baseUrl +
                        MapTileIndex.getZoom(pMapTileIndex) + "/" +
                        MapTileIndex.getY(pMapTileIndex) + "/" +
                        MapTileIndex.getX(pMapTileIndex)
            }
        }
    }

    // ---------------- Ubicació + orientació ----------------

    private fun ensureLocationPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) enableMyLocation()
        else permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun enableMyLocation() {
        if (myLocationOverlay != null) return
        val density = resources.displayMetrics.density
        val dot = Graphics.blueDot(density)

        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        // Punt blau tant aturat com en moviment (sense figura humana).
        overlay.setPersonIcon(dot)
        overlay.setDirectionIcon(dot)
        overlay.setPersonAnchor(0.5f, 0.5f)
        overlay.setDirectionAnchor(0.5f, 0.5f)
        overlay.enableMyLocation()
        map.overlays.add(overlay)
        myLocationOverlay = overlay

        // Fletxa d'orientació per sobre del punt.
        val arrowOverlay = HeadingArrowOverlay(Graphics.facingArrow(density)) {
            myLocationOverlay?.myLocation
        }
        map.overlays.add(arrowOverlay)
        headingArrowOverlay = arrowOverlay

        map.invalidate()
    }

    private fun centerOnMe() {
        val loc = myLocationOverlay?.myLocation
        if (loc != null) {
            map.controller.animateTo(loc)
            map.controller.setZoom(16.0)
        } else {
            toast("Encara no tinc la teva ubicació. Comprova que el GPS està activat.")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        azimuth = (azimuth + 360f) % 360f
        // Suavitzat circular per evitar tremolors.
        val diff = ((azimuth - currentHeading + 540f) % 360f) - 180f
        currentHeading = (currentHeading + 0.15f * diff + 360f) % 360f
        headingArrowOverlay?.headingDeg = currentHeading
        if (abs(diff) > 1f) map.postInvalidate()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ---------------- Càrrega de GPX ----------------

    private fun loadGpx(uri: Uri) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) {
                    toast("No s'ha pogut obrir el fitxer.")
                    return
                }
                val result = GpxParser.parse(input)
                if (result.points.size < 2) {
                    toast("El GPX no conté cap traça vàlida.")
                    return
                }
                val displayName = result.name ?: queryDisplayName(uri) ?: "Ruta"
                binding.txtRouteName.text = displayName
                binding.txtRouteName.visibility = TextView.VISIBLE

                drawRoute(result)
                showStats(result)
            }
        } catch (e: Exception) {
            toast("No s'ha pogut llegir el GPX: ${e.message}")
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = c.getString(idx)
                }
            }
        return name?.removeSuffix(".gpx")?.removeSuffix(".GPX")
    }

    private fun drawRoute(result: GpxParser.GpxResult) {
        // Neteja el que hi hagués abans.
        gradientSegments.forEach { map.overlays.remove(it) }
        gradientSegments.clear()
        routeArrowsOverlay?.let { map.overlays.remove(it) }
        routeArrowsOverlay = null
        selectionMarker?.let { map.overlays.remove(it) }
        selectionMarker = null

        val pts = result.points
        val eles = result.elevations
        val n = pts.size
        routePoints = pts

        // --- Track amb gradient de color segons pendent ---
        val maxSegments = 600
        val step = max(1, ceil((n - 1).toDouble() / maxSegments).toInt())
        var i = 0
        while (i < n - 1) {
            val j = min(i + step, n - 1)
            val dist = segmentDistance(pts, i, j)
            val eleI = eles.getOrElse(i) { Double.NaN }
            val eleJ = eles.getOrElse(j) { Double.NaN }
            val color = if (eleI.isNaN() || eleJ.isNaN() || dist < 0.5)
                Graphics.neutralColor()
            else
                Graphics.slopeColor((eleJ - eleI) / dist)

            val seg = Polyline(map)
            seg.setPoints(pts.subList(i, j + 1))
            seg.outlinePaint.color = color
            seg.outlinePaint.strokeWidth = 12f
            seg.outlinePaint.strokeCap = Paint.Cap.ROUND
            map.overlays.add(seg)
            gradientSegments.add(seg)
            i = j
        }

        // --- Fletxes de sentit ---
        val idx = pickArrowIndices(pts)
        val density = resources.displayMetrics.density
        val arrows = RouteArrowsOverlay(pts, idx, density)
        map.overlays.add(arrows)
        routeArrowsOverlay = arrows

        // El punt i la fletxa d'ubicació sempre a dalt de tot.
        reAddTopOverlays()

        // --- Perfil d'altitud ---
        routeHasElevation = eles.any { !it.isNaN() }
        if (routeHasElevation) {
            binding.elevationProfile.setData(pts, eles)
        } else {
            binding.elevationProfile.visibility = android.view.View.GONE
        }
        binding.elevationProfile.clearSelection()

        val bb = BoundingBox.fromGeoPoints(pts)
        routeBoundingBox = bb
        map.post { map.zoomToBoundingBox(bb, true, 80) }
        map.invalidate()
    }

    /** Torna a posar el punt d'ubicació i la fletxa al capdamunt de la pila d'overlays. */
    private fun reAddTopOverlays() {
        myLocationOverlay?.let { map.overlays.remove(it); map.overlays.add(it) }
        headingArrowOverlay?.let { map.overlays.remove(it); map.overlays.add(it) }
    }

    private fun segmentDistance(pts: List<GeoPoint>, from: Int, to: Int): Double {
        var d = 0.0
        for (k in from until to) d += pts[k].distanceToAsDouble(pts[k + 1])
        return d
    }

    /** Tria índexs per posar una fletxa cada ~120 m, amb un màxim raonable. */
    private fun pickArrowIndices(pts: List<GeoPoint>): List<Int> {
        val intervalMeters = 120.0
        val maxArrows = 500
        val indices = ArrayList<Int>()
        var accumulated = 0.0
        var nextAt = intervalMeters
        for (k in 0 until pts.size - 1) {
            accumulated += pts[k].distanceToAsDouble(pts[k + 1])
            if (accumulated >= nextAt) {
                indices.add(k)
                nextAt += intervalMeters
            }
        }
        if (indices.size > maxArrows) {
            val stp = ceil(indices.size.toDouble() / maxArrows).toInt()
            return indices.filterIndexed { pos, _ -> pos % stp == 0 }
        }
        return indices
    }

    private fun showStats(result: GpxParser.GpxResult) {
        val km = result.totalDistanceMeters / 1000.0
        val sb = StringBuilder()
        sb.append("Distància: %.2f km".format(km))
        if (!result.minElevation.isNaN() && !result.maxElevation.isNaN()) {
            sb.append("   ·   ↑ %d m   ↓ %d m".format(
                result.elevationGainMeters.roundToInt(),
                result.elevationLossMeters.roundToInt()
            ))
            sb.append("\nAltitud: %d – %d m".format(
                result.minElevation.roundToInt(),
                result.maxElevation.roundToInt()
            ))
        } else {
            sb.append("\n(El GPX no inclou dades d'altitud)")
        }
        binding.txtStats.text = sb.toString()
        binding.txtStats.visibility = TextView.VISIBLE
    }

    // ---------------- Descàrrega offline ----------------

    private fun confirmDownload() {
        val bb = routeBoundingBox
        if (bb == null) {
            toast("Primer carrega una ruta GPX.")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Baixar mapes offline")
            .setMessage(
                "Es descarregaran les tessel·les del mapa topogràfic i del satèl·lit al " +
                        "voltant de la ruta (zoom $downloadZoomMin–$downloadZoomMax) perquè " +
                        "puguis fer-les servir sense cobertura.\n\nFes-ho amb WiFi: pot trigar " +
                        "una estona i ocupar espai."
            )
            .setPositiveButton("Baixa") { _, _ -> startDownloadBoth(bb) }
            .setNegativeButton("Cancel·la", null)
            .show()
    }

    private fun startDownloadBoth(bb: BoundingBox) {
        showProgressDialog()
        downloadSource(topoSource, bb, "Topogràfic") {
            downloadSource(satelliteSource, bb, "Satèl·lit") {
                map.setTileSource(if (usingSatellite) satelliteSource else topoSource)
                dismissProgressDialog()
                toast("Mapes desats. Ja pots fer servir aquesta zona sense cobertura.")
            }
        }
    }

    private fun downloadSource(
        source: ITileSource,
        bb: BoundingBox,
        label: String,
        onDone: () -> Unit
    ) {
        map.setTileSource(source)
        val cacheManager = CacheManager(map)
        cacheManager.downloadAreaAsync(
            this, bb, downloadZoomMin, downloadZoomMax,
            object : CacheManager.CacheManagerCallback {
                override fun onTaskComplete() = onDone()

                override fun onTaskFailed(errors: Int) {
                    runOnUiThread { toast("$label: $errors tessel·les no s'han pogut baixar.") }
                    onDone()
                }

                override fun updateProgress(
                    progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int
                ) {
                    runOnUiThread {
                        progressBar?.progress = progress
                        val max = progressBar?.max ?: 0
                        progressText?.text = "$label — zoom $currentZoomLevel · $progress / $max"
                    }
                }

                override fun downloadStarted() {}

                override fun setPossibleTilesInArea(total: Int) {
                    runOnUiThread {
                        progressBar?.max = if (total > 0) total else 100
                        progressBar?.progress = 0
                    }
                }
            }
        )
    }

    private fun showProgressDialog() {
        val padding = (24 * resources.displayMetrics.density).toInt()
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        val text = TextView(this).apply { text = "Preparant la descàrrega…" }
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
        }
        container.addView(text)
        container.addView(bar)
        progressText = text
        progressBar = bar

        progressDialog = AlertDialog.Builder(this)
            .setTitle("Baixant mapes")
            .setView(container)
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
        progressBar = null
        progressText = null
    }

    // ---------------- Utilitats i cicle de vida ----------------

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    override fun onResume() {
        super.onResume()
        map.onResume()
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        sensorManager?.unregisterListener(this)
    }
}
