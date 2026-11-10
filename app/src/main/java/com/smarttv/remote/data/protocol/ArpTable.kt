package com.smarttv.remote.data.protocol

import java.io.File

/** Best-effort lookup of a TV's MAC address (used for Wake-on-LAN). Android may block this. */
object ArpTable {
    fun macFor(ip: String): String? {
        return try {
            File("/proc/net/arp").readLines().drop(1).firstNotNullOfOrNull { line ->
                val cols = line.trim().split(Regex("\\s+"))
                if (cols.size >= 4 && cols[0] == ip && cols[3] != "00:00:00:00:00:00" && cols[3].count { it == ':' } == 5) {
                    cols[3]
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
