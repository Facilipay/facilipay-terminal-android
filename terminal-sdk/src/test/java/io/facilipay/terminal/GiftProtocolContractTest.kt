package io.facilipay.terminal

import java.io.File
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The library's copy of the gift protocol against `protocol/gift-intent-protocol.properties`, the
 * file Facilipay's app checks itself against too. Two-way: every shared constant here is in the
 * file with the same value, and every key in the file is here. So a change on either side fails
 * until the other matches.
 */
class GiftProtocolContractTest {

    private val file = readProtocol(File(requiredProperty("facilipay.protocolFile")))

    @Test
    fun theLibrarysConstants_matchTheProtocolFile_bothWays() {
        val library = sharedConstants(GiftProtocol::class.java)
        assertTrue(library.size >= MIN_LIBRARY_CONSTANTS - LIBRARY_ONLY.size, "found ${library.size} constants")

        val expected = file - CHECKED_SEPARATELY
        assertEquals(expected.keys.sorted(), library.keys.sorted(), "names in the file and in GiftProtocol")
        for ((name, value) in expected) {
            assertEquals(value, library[name], name)
        }
    }

    @Test
    fun thePermission_isFacilipaysPackagePlusTheSuffix() {
        assertEquals("x" + file.getValue("PERMISSION_SUFFIX"), FacilipayGift.permission("x"))
        assertEquals(
            file.getValue("DEFAULT_PACKAGE") + file.getValue("PERMISSION_SUFFIX"),
            FacilipayGift.permission(),
        )
    }

    @Test
    fun theDefaultPackage_isFacilipayOnPax() {
        assertEquals(file.getValue("DEFAULT_PACKAGE"), FacilipayGift.FACILIPAY_PACKAGE)
    }

    @Test
    fun theManifest_asksForTheRightPermissionAndCanSeeFacilipay() {
        val manifest = File(requiredProperty("facilipay.libraryManifest")).readText()
        val pkg = file.getValue("DEFAULT_PACKAGE")
        val permission = """<uses-permission android:name="$pkg${file.getValue("PERMISSION_SUFFIX")}""""
        val query = """<package android:name="$pkg""""
        assertTrue(permission in manifest, "manifest lacks $permission")
        assertTrue(query in manifest, "manifest lacks $query")
    }

    private companion object {
        /** The library's own codes, not part of the shared protocol (Facilipay passes them through). */
        val LIBRARY_ONLY = setOf(
            "CODE_APPROVED", "CODE_PARTIAL",
            "CODE_CANCELLED_ON_TERMINAL", "CODE_CANCELLED_BY_ANOTHER_REQUEST", "CODE_TIMED_OUT",
            "CODE_INSUFFICIENT_BALANCE", "CODE_CARD_EXPIRED", "CODE_CARD_NOT_FOUND", "CODE_CARD_NOT_LINKED",
        )

        /** In the file, but checked by the tests above rather than as a GiftProtocol constant. */
        val CHECKED_SEPARATELY = setOf("DEFAULT_PACKAGE")

        /** Every constant GiftProtocol has; a filter that finds fewer is broken. */
        const val MIN_LIBRARY_CONSTANTS = 64

        fun requiredProperty(name: String): String =
            System.getProperty(name) ?: fail("$name is not set: run the tests through Gradle")

        /** The public static String/int/long constants of [type], by name, minus [LIBRARY_ONLY]. */
        fun sharedConstants(type: Class<*>): Map<String, String> {
            val all = type.declaredFields
                .filter { Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers) }
                .filter { !it.isSynthetic && '$' !in it.name }
                .filter { it.type == String::class.java || it.type == Int::class.javaPrimitiveType || it.type == Long::class.javaPrimitiveType }
                .associate { it.name to it.get(null).toString() }
            assertTrue(all.size >= MIN_LIBRARY_CONSTANTS, "found ${all.size} constants in ${type.simpleName}")
            assertTrue(all.keys.containsAll(LIBRARY_ONLY), "library-only codes missing: ${LIBRARY_ONLY - all.keys}")
            return all - LIBRARY_ONLY
        }

        /**
         * Reads the protocol file strictly: `KEY=value` lines, `#` comments and blank lines. A
         * duplicate key, spaces around `=` or an empty value fail, where java.util.Properties would
         * quietly keep the last one.
         */
        fun readProtocol(file: File): Map<String, String> {
            val entries = linkedMapOf<String, String>()
            file.readLines().forEachIndexed { index, raw ->
                val line = raw.removeSuffix("\r")
                if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
                val at = "${file.name}:${index + 1}"
                val separator = line.indexOf('=')
                if (separator <= 0) fail("$at: expected KEY=value")
                val key = line.substring(0, separator)
                val value = line.substring(separator + 1)
                if (key != key.trim() || value != value.trim()) fail("$at: spaces around '='")
                if (value.isEmpty()) fail("$at: empty value for $key")
                if (entries.put(key, value) != null) fail("$at: duplicate key $key")
            }
            return entries
        }
    }
}
