package com.aegis.services.vpn

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.aegis.AegisApplication
import com.aegis.network.ThreatIntelClient
import com.aegis.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class AegisVpnService : VpnService() {

    @Inject
    lateinit var threatIntelClient: ThreatIntelClient

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private val isRunning = AtomicBoolean(false)

    private var blockedDomains = mutableSetOf(
        "phishing-site.com", "scam-link.net", "fake-login.com",
        "steal-info.org", "malware-download.ru", "spyware.cc",
        "identity-theft.net", "ransomware.biz", "fraud-alert.info"
    )

    private var dnsBlocklist = mutableSetOf(
        "185.220.101.0", "185.220.102.0", "45.33.32.0",
        "104.16.0.0", "104.17.0.0"
    )

    override fun onCreate() {
        super.onCreate()
        updateBlocklistsFromCloud()
    }

    private fun updateBlocklistsFromCloud() {
        scope.launch {
            val data = threatIntelClient.getBlocklist()
            if (data != null) {
                blockedDomains.addAll(data.domains)
                dnsBlocklist.addAll(data.ips)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning.get()) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Builder().apply {
            setSession("AEGIS Protection")
            setConfigureIntent(pendingIntent)
            addAddress("10.0.0.2", 32)
            addRoute("0.0.0.0", 0)
            addDnsServer("8.8.8.8")
            addDnsServer("1.1.1.1")
            setBlocking(true)
        }

        try {
            vpnInterface?.close()
            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                isRunning.set(true)
                scope.launch {
                    processPackets()
                }
            }
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private suspend fun processPackets() {
        val input = FileInputStream(vpnInterface?.fileDescriptor)
        val output = FileOutputStream(vpnInterface?.fileDescriptor)
        val packet = ByteBuffer.allocate(32767)

        while (isRunning.get()) {
            try {
                packet.clear()
                val length = input.channel.read(packet)
                if (length > 0) {
                    packet.flip()
                    val processed = filterPacket(packet)
                    if (processed) {
                        output.channel.write(packet)
                    }
                }
            } catch (e: Exception) {
                break
            }
        }
    }

    private fun filterPacket(buffer: ByteBuffer): Boolean {
        if (buffer.remaining() < 20) return true
        val version = buffer.get(0).toInt() shr 4 and 0x0F
        return when (version) {
            4 -> filterIPv4(buffer)
            6 -> filterIPv6(buffer)
            else -> true
        }
    }

    private fun filterIPv4(buffer: ByteBuffer): Boolean {
        val destIp = "${buffer.get(16).toInt() and 0xFF}." +
                "${buffer.get(17).toInt() and 0xFF}." +
                "${buffer.get(18).toInt() and 0xFF}." +
                "${buffer.get(19).toInt() and 0xFF}"
        return destIp !in dnsBlocklist
    }

    private fun filterIPv6(buffer: ByteBuffer): Boolean = true

    private fun stopVpn() {
        isRunning.set(false)
        vpnInterface?.close()
        vpnInterface = null
        stopSelf()
    }

    fun updateBlocklist(domains: Set<String>, ips: Set<String>) {
        blockedDomains.toMutableSet().apply {
            addAll(domains)
        }
        dnsBlocklist.toMutableSet().apply {
            addAll(ips)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        stopVpn()
    }
}
