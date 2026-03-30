package com.ardufocus.btcontroller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.WindowManager
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import com.google.android.material.card.MaterialCardView
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import android.os.Handler
import android.os.Looper
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private data class DeviceItem(val label: String, val device: BluetoothDevice?) {
        override fun toString(): String = label
    }

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private lateinit var deviceSpinner: Spinner
    private lateinit var connectButton: Button
    private lateinit var navTabs: TabLayout
    private lateinit var connectionPageCard: MaterialCardView
    private lateinit var connectionDisclaimerCard: View
    private lateinit var focusPageCard: MaterialCardView
    private lateinit var telemetryPageCard: MaterialCardView
    private lateinit var statusText: TextView
    private lateinit var positionValueText: TextView
    private lateinit var telemetryPreviewText: TextView
    private lateinit var backwardButton: Button
    private lateinit var forwardButton: Button
    private lateinit var step1Button: Button
    private lateinit var step2Button: Button
    private lateinit var step5Button: Button
    private lateinit var step10Button: Button
    private lateinit var step25Button: Button
    private lateinit var step50Button: Button
    private lateinit var step100Button: Button
    private lateinit var step250Button: Button
    private lateinit var step500Button: Button
    private lateinit var stopButton: Button
    private lateinit var logText: TextView
    private lateinit var logScroll: ScrollView

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null
    private var readerThread: Thread? = null
    private var connectedDeviceName: String? = null
    private var pendingCommand: String? = null
    private var pendingCommandRetries = 0
    private var selectedStep = 50
    private val uiHandler = Handler(Looper.getMainLooper())
    private val periodicPosRefresh = object : Runnable {
        override fun run() {
            if (socket != null) {
                requestPosition()
                uiHandler.postDelayed(this, 60000)
            }
        }
    }

    private val bluetoothPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshBondedDevices()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        deviceSpinner = findViewById(R.id.deviceSpinner)
        connectButton = findViewById(R.id.connectButton)
        navTabs = findViewById(R.id.navTabs)
        connectionPageCard = findViewById(R.id.connectionPageCard)
        connectionDisclaimerCard = findViewById(R.id.connectionDisclaimerCard)
        focusPageCard = findViewById(R.id.focusPageCard)
        telemetryPageCard = findViewById(R.id.telemetryPageCard)
        statusText = findViewById(R.id.statusText)
        positionValueText = findViewById(R.id.positionValueText)
        telemetryPreviewText = findViewById(R.id.telemetryPreviewText)
        backwardButton = findViewById(R.id.backwardButton)
        forwardButton = findViewById(R.id.forwardButton)
        step1Button = findViewById(R.id.step1Button)
        step2Button = findViewById(R.id.step2Button)
        step5Button = findViewById(R.id.step5Button)
        step10Button = findViewById(R.id.step10Button)
        step25Button = findViewById(R.id.step25Button)
        step50Button = findViewById(R.id.step50Button)
        step100Button = findViewById(R.id.step100Button)
        step250Button = findViewById(R.id.step250Button)
        step500Button = findViewById(R.id.step500Button)
        stopButton = findViewById(R.id.stopButton)
        logText = findViewById(R.id.logText)
        logScroll = findViewById(R.id.logScroll)

        logText.movementMethod = ScrollingMovementMethod()

        connectButton.setOnClickListener {
            if (socket == null) {
                connectSelectedDevice()
            } else {
                disconnect()
            }
        }

        if (navTabs.tabCount == 0) {
            navTabs.addTab(navTabs.newTab().setText("Connessione"))
            navTabs.addTab(navTabs.newTab().setText("Fuoco"))
            navTabs.addTab(navTabs.newTab().setText("Telemetria"))
        }

        for (index in 0 until navTabs.tabCount) {
            val tab = navTabs.getTabAt(index)
            val label = TextView(this)
            label.text = tab?.text?.toString()?.uppercase()
            label.typeface = android.graphics.Typeface.create("sans-serif-monospace", android.graphics.Typeface.NORMAL)
            label.setTextColor(ContextCompat.getColor(this, R.color.white))
            label.letterSpacing = 0.08f
            label.textSize = 12f
            label.gravity = Gravity.CENTER
            tab?.customView = label
        }

        navTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> showPage(Page.CONNECTION, false)
                    1 -> showPage(Page.FOCUS, false)
                    else -> showPage(Page.TELEMETRY, false)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {;}
            override fun onTabReselected(tab: TabLayout.Tab) {;}
        })

        backwardButton.setOnClickListener { sendMovementCommand("BWD", selectedStep) }
        forwardButton.setOnClickListener { sendMovementCommand("FWD", selectedStep) }
        step1Button.setOnClickListener { selectStep(1) }
        step2Button.setOnClickListener { selectStep(2) }
        step5Button.setOnClickListener { selectStep(5) }
        step10Button.setOnClickListener { selectStep(10) }
        step25Button.setOnClickListener { selectStep(25) }
        step50Button.setOnClickListener { selectStep(50) }
        step100Button.setOnClickListener { selectStep(100) }
        step250Button.setOnClickListener { selectStep(250) }
        step500Button.setOnClickListener { selectStep(500) }
        stopButton.setOnClickListener { sendCommand("STOP") }

        selectStep(selectedStep)

        ensureBluetoothPermissionAndLoadDevices()
        updateDefaultPage()
    }

    override fun onResume() {
        super.onResume()
        ensureBluetoothPermissionAndLoadDevices()
        updateDefaultPage()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }

    private fun ensureBluetoothPermissionAndLoadDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val missingPermissions = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (missingPermissions.isNotEmpty()) {
                bluetoothPermissionsLauncher.launch(missingPermissions.toTypedArray())
                return
            }
        }

        refreshBondedDevices()
    }

    private enum class Page {
        CONNECTION,
        FOCUS,
        TELEMETRY
    }

    private fun showPage(page: Page, syncTab: Boolean = true) {
        connectionPageCard.visibility = if (page == Page.CONNECTION) View.VISIBLE else View.GONE
        connectionDisclaimerCard.visibility = if (page == Page.CONNECTION) View.VISIBLE else View.GONE
        focusPageCard.visibility = if (page == Page.FOCUS) View.VISIBLE else View.GONE
        telemetryPreviewText.visibility = if (page == Page.FOCUS) View.VISIBLE else View.GONE
        telemetryPageCard.visibility = if (page == Page.TELEMETRY) View.VISIBLE else View.GONE

        if (syncTab) {
            val index = when (page) {
                Page.CONNECTION -> 0
                Page.FOCUS -> 1
                Page.TELEMETRY -> 2
            }
            navTabs.getTabAt(index)?.select()
        }
    }

    private fun updateDefaultPage() {
        showPage(if (socket == null) Page.CONNECTION else Page.FOCUS)
    }

    private fun buildDeviceAdapter(items: List<DeviceItem>): ArrayAdapter<DeviceItem> {
        return object : ArrayAdapter<DeviceItem>(this, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    textSize = 16f
                    gravity = Gravity.CENTER_VERTICAL
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    setBackgroundColor(ContextCompat.getColor(context, R.color.panel_bg))
                    textSize = 16f
                    setPadding(24, 24, 24, 24)
                }
                return view
            }
        }.also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    @SuppressLint("MissingPermission")
    private fun refreshBondedDevices() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            setStatus("Bluetooth non disponibile")
            deviceSpinner.adapter = buildDeviceAdapter(listOf(DeviceItem("Nessun adattatore BT", null)))
            return
        }

        val bonded = adapter.bondedDevices.orEmpty().sortedBy { it.name ?: it.address }
        val items = if (bonded.isEmpty()) {
            listOf(DeviceItem("Nessun device associato", null))
        } else {
            bonded.map { DeviceItem("${it.name ?: "Sconosciuto"} (${it.address})", it) }
        }

        deviceSpinner.adapter = buildDeviceAdapter(items)

        if (socket == null && items.isNotEmpty()) {
            deviceSpinner.setSelection(findPreferredDeviceIndex(items))
        }

        setStatus(if (socket == null) "Disconnesso" else "Connesso: $connectedDeviceName")
    }

    private fun findPreferredDeviceIndex(items: List<DeviceItem>): Int {
        val preferredNames = listOf("ArduFocus", "HC-05", "HC-06")

        preferredNames.forEach { preferred ->
            val index = items.indexOfFirst { item ->
                item.device?.name?.contains(preferred, ignoreCase = true) == true
            }

            if (index >= 0) {
                return index
            }
        }

        return 0
    }

    @SuppressLint("MissingPermission")
    private fun connectSelectedDevice() {
        val item = deviceSpinner.selectedItem as? DeviceItem
        val device = item?.device ?: run {
            appendLog("APP", "Nessun dispositivo selezionato")
            return
        }

        val adapter = bluetoothAdapter ?: return
        setBusy(true)
        setStatus("Connessione a ${device.name ?: device.address}...")

        thread(name = "bt-connect") {
            try {
                adapter.cancelDiscovery()
                val newSocket = device.createRfcommSocketToServiceRecord(sppUuid)
                newSocket.connect()

                socket = newSocket
                output = newSocket.outputStream
                connectedDeviceName = device.name ?: device.address

                startReader(newSocket)

                runOnUiThread {
                    setBusy(false)
                    connectButton.text = "Disconnetti"
                    setStatus("Connesso: ${connectedDeviceName.orEmpty()}")
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    appendLog("APP", "Connesso a ${connectedDeviceName.orEmpty()}")
                    requestInfo()
                    requestPosition()
                    uiHandler.removeCallbacks(periodicPosRefresh)
                    uiHandler.postDelayed(periodicPosRefresh, 60000)
                    updateDefaultPage()
                }
            } catch (e: Exception) {
                socket = null
                output = null
                connectedDeviceName = null

                runOnUiThread {
                    setBusy(false)
                    connectButton.text = "Connetti"
                    setStatus("Connessione fallita")
                    appendLog("APP", "Errore connessione: ${e.message}")
                }
            }
        }
    }

    private fun startReader(activeSocket: BluetoothSocket) {
        readerThread?.interrupt()
        readerThread = thread(name = "bt-reader") {
            try {
                BufferedReader(InputStreamReader(activeSocket.inputStream)).use { reader ->
                    while (!Thread.currentThread().isInterrupted()) {
                        val line = reader.readLine() ?: break
                        runOnUiThread {
                            appendLog("RX", line)
                            handleIncomingLine(line)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                runOnUiThread {
                    if (socket === activeSocket) {
                        disconnect()
                    }
                }
            }
        }
    }

    private fun sendCommand(command: String, trackRetry: Boolean = true) {
        val out = output
        if (out == null) {
            appendLog("APP", "Non connesso")
            return
        }

        if (trackRetry) {
            pendingCommand = command
            pendingCommandRetries = 5
        }

        thread(name = "bt-send") {
            try {
                out.write((command + "\r\n").toByteArray())
                out.flush()
                runOnUiThread { appendLog("TX", command) }
            } catch (e: Exception) {
                runOnUiThread {
                    appendLog("APP", "Errore invio: ${e.message}")
                    disconnect()
                }
            }
        }
    }

    private fun requestPosition() {
        sendCommand("POS")
    }

    private fun handleIncomingLine(line: String) {
        val normalized = line.trim()

        if (normalized.startsWith("POS ")) {
            positionValueText.text = normalized.removePrefix("POS ").trim()
            pendingCommand = null
            pendingCommandRetries = 0
            return
        }

        if (normalized.startsWith("INFO ")) {
            pendingCommand = null
            pendingCommandRetries = 0
            return
        }

        if ((normalized == "OK") || normalized.startsWith("BT READY") || normalized.startsWith("BT LINK ")) {
            pendingCommand = null
            pendingCommandRetries = 0
            return
        }

        if ((normalized == "ERR UNKNOWN") || (normalized == "ERR_UNKNOWN")) {
            if ((pendingCommand != null) && (pendingCommandRetries > 1)) {
                pendingCommandRetries -= 1
                sendCommand(pendingCommand!!, false)
            }
            else {
                pendingCommand = null
                pendingCommandRetries = 0
            }
        }
    }

    private fun requestInfo() {
        sendCommand("INFO")
    }

    private fun selectStep(step: Int) {
        selectedStep = step

        val selectedBg = ContextCompat.getColor(this, R.color.step_selected)
        val selectedText = ContextCompat.getColor(this, R.color.step_selected_text)
        val normalBg = ContextCompat.getColor(this, R.color.panel_stroke)
        val normalText = ContextCompat.getColor(this, R.color.text_primary)

        val buttons = listOf(
            step1Button to 1,
            step2Button to 2,
            step5Button to 5,
            step10Button to 10,
            step25Button to 25,
            step50Button to 50,
            step100Button to 100,
            step250Button to 250,
            step500Button to 500,
        )

        buttons.forEach { (button, value) ->
            val isSelected = value == selectedStep
            button.setBackgroundColor(if (isSelected) selectedBg else normalBg)
            button.setTextColor(if (isSelected) selectedText else normalText)
        }
    }

    private fun sendMovementCommand(direction: String, steps: Int?) {
        val command = if (steps == null) direction else "$direction $steps"
        sendCommand(command)
        if (steps != null) {
            updateDisplayedPosition(direction, steps)
        }
    }

    private fun updateDisplayedPosition(direction: String, steps: Int) {
        val current = positionValueText.text?.toString()?.trim()?.toIntOrNull() ?: return
        val updated = if (direction == "FWD") current + steps else (current - steps).coerceAtLeast(0)
        positionValueText.text = updated.toString()
    }

    private fun disconnect() {
        try {
            readerThread?.interrupt()
            readerThread = null
            socket?.close()
        } catch (_: Exception) {
        }

        socket = null
        output = null
        connectedDeviceName = null
        pendingCommand = null
        pendingCommandRetries = 0
        positionValueText.text = "--"
        telemetryPreviewText.text = "--"
        connectButton.text = "Connetti"
        uiHandler.removeCallbacks(periodicPosRefresh)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setBusy(false)
        setStatus("Disconnesso")
        updateDefaultPage()
    }

    private fun setBusy(isBusy: Boolean) {
        connectButton.isEnabled = !isBusy
        deviceSpinner.isEnabled = !isBusy && socket == null
    }

    private fun setStatus(message: String) {
        statusText.text = message
    }

    private fun appendLog(prefix: String, message: String) {
        val current = logText.text?.toString().orEmpty()
        val updated = if (current.isBlank()) {
            "[$prefix] $message"
        } else {
            "$current\n[$prefix] $message"
        }

        logText.text = updated
        telemetryPreviewText.text = updated.lines().takeLast(3).joinToString("\n")
        logScroll.post { logScroll.fullScroll(android.view.View.FOCUS_DOWN) }
    }
}
