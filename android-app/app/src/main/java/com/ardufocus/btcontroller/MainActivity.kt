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
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private lateinit var statusText: TextView
    private lateinit var positionValueText: TextView
    private lateinit var backwardButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var backward1Button: Button
    private lateinit var backward5Button: Button
    private lateinit var backward10Button: Button
    private lateinit var backward50Button: Button
    private lateinit var backward100Button: Button
    private lateinit var backward500Button: Button
    private lateinit var forward1Button: Button
    private lateinit var forward5Button: Button
    private lateinit var forward10Button: Button
    private lateinit var forward50Button: Button
    private lateinit var forward100Button: Button
    private lateinit var forward500Button: Button
    private lateinit var stopButton: ImageButton
    private lateinit var posButton: ImageButton
    private lateinit var logText: TextView
    private lateinit var logScroll: ScrollView

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null
    private var readerThread: Thread? = null
    private var connectedDeviceName: String? = null
    private var pendingPosRetries = 0
    private var pendingInfoRetries = 0
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
        statusText = findViewById(R.id.statusText)
        positionValueText = findViewById(R.id.positionValueText)
        backwardButton = findViewById(R.id.backwardButton)
        forwardButton = findViewById(R.id.forwardButton)
        backward1Button = findViewById(R.id.backward1Button)
        backward5Button = findViewById(R.id.backward5Button)
        backward10Button = findViewById(R.id.backward10Button)
        backward50Button = findViewById(R.id.backward50Button)
        backward100Button = findViewById(R.id.backward100Button)
        backward500Button = findViewById(R.id.backward500Button)
        forward1Button = findViewById(R.id.forward1Button)
        forward5Button = findViewById(R.id.forward5Button)
        forward10Button = findViewById(R.id.forward10Button)
        forward50Button = findViewById(R.id.forward50Button)
        forward100Button = findViewById(R.id.forward100Button)
        forward500Button = findViewById(R.id.forward500Button)
        stopButton = findViewById(R.id.stopButton)
        posButton = findViewById(R.id.posButton)
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

        backwardButton.setOnClickListener { sendMovementCommand("BWD", null) }
        forwardButton.setOnClickListener { sendMovementCommand("FWD", null) }
        backward1Button.setOnClickListener { sendMovementCommand("BWD", 1) }
        backward5Button.setOnClickListener { sendMovementCommand("BWD", 5) }
        backward10Button.setOnClickListener { sendMovementCommand("BWD", 10) }
        backward50Button.setOnClickListener { sendMovementCommand("BWD", 50) }
        backward100Button.setOnClickListener { sendMovementCommand("BWD", 100) }
        backward500Button.setOnClickListener { sendMovementCommand("BWD", 500) }
        forward1Button.setOnClickListener { sendMovementCommand("FWD", 1) }
        forward5Button.setOnClickListener { sendMovementCommand("FWD", 5) }
        forward10Button.setOnClickListener { sendMovementCommand("FWD", 10) }
        forward50Button.setOnClickListener { sendMovementCommand("FWD", 50) }
        forward100Button.setOnClickListener { sendMovementCommand("FWD", 100) }
        forward500Button.setOnClickListener { sendMovementCommand("FWD", 500) }
        stopButton.setOnClickListener { sendCommand("STOP") }
        posButton.setOnClickListener { requestPosition() }

        ensureBluetoothPermissionAndLoadDevices()
    }

    override fun onResume() {
        super.onResume()
        ensureBluetoothPermissionAndLoadDevices()
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

        setStatus(if (socket == null) "Disconnesso" else "Connesso: ${connectedDeviceName.orEmpty()}")
    }

    private fun findPreferredDeviceIndex(items: List<DeviceItem>): Int {
        val preferredNames = listOf("JDY-31-SPP", "JDY-34-SPP", "HC-05", "HC-06")

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

    private fun sendCommand(command: String) {
        val out = output
        if (out == null) {
            appendLog("APP", "Non connesso")
            return
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
        pendingPosRetries = 5
        sendCommand("POS")
    }

    private fun handleIncomingLine(line: String) {
        val normalized = line.trim()

        if (normalized.startsWith("POS ")) {
            positionValueText.text = normalized.removePrefix("POS ").trim()
            pendingPosRetries = 0
            return
        }

        if (normalized.startsWith("INFO ")) {
            pendingInfoRetries = 0
            return
        }

        if ((normalized == "ERR UNKNOWN") || (normalized == "ERR_UNKNOWN")) {
            if (pendingPosRetries > 1) {
                pendingPosRetries -= 1
                sendCommand("POS")
                return
            }

            if (pendingInfoRetries > 1) {
                pendingInfoRetries -= 1
                sendCommand("INFO")
            } else {
                pendingPosRetries = 0
                pendingInfoRetries = 0
            }
        }
    }

    private fun requestInfo() {
        pendingInfoRetries = 5
        sendCommand("INFO")
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
        pendingPosRetries = 0
        pendingInfoRetries = 0
        positionValueText.text = "--"
        connectButton.text = "Connetti"
        uiHandler.removeCallbacks(periodicPosRefresh)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setBusy(false)
        setStatus("Disconnesso")
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
        logScroll.post { logScroll.fullScroll(android.view.View.FOCUS_DOWN) }
    }
}
