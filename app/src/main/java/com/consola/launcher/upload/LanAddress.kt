package com.consola.launcher.upload

import java.net.Inet4Address
import java.net.NetworkInterface

/** La IP de la tele en la red de casa: la que el celular puede alcanzar para subir videos. */
object LanAddress {

    fun current(): String? {
        val candidates = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .mapNotNull { it.hostAddress }
        return pick(candidates)
    }

    /** Puro: elige entre las IPs de la tele. Prefiere 192.168.x, la red de casa más común. */
    fun pick(candidates: List<String>): String? =
        candidates.filter(::isPrivateLan).minByOrNull { ip ->
            when {
                ip.startsWith("192.168.") -> 0
                ip.startsWith("10.") -> 1
                else -> 2
            }
        }

    private fun isPrivateLan(ip: String): Boolean {
        val parts = ip.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        return when {
            parts[0] == 192 && parts[1] == 168 -> true
            parts[0] == 10 -> true
            parts[0] == 172 && parts[1] in 16..31 -> true
            else -> false
        }
    }
}
