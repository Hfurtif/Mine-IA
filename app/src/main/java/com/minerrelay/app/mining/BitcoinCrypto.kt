package com.minerrelay.app.mining

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * Primitives cryptographiques Bitcoin réelles :
 * - Double SHA-256
 * - Construction de l'arbre de Merkle depuis coinbase1 + extranonce1 + extranonce2 + coinbase2 + branches
 * - Construction de l'en-tête de bloc de 80 octets
 * - Conversion nBits / difficulté -> cible 256 bits et vérification locale d'une part
 */
object BitcoinCrypto {

    /** Cible Bitcoin difficulté 1 : 0x00000000FFFF0000000000000000000000000000000000000000000000000000 */
    val DIFFICULTY_1_TARGET = BigInteger(
        "00000000FFFF0000000000000000000000000000000000000000000000000000",
        16
    )

    fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    fun doubleSha256(data: ByteArray): ByteArray =
        sha256(sha256(data))

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        require(clean.length % 2 == 0) { "Longueur hexadécimale impaire (${clean.length})" }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    fun uint32ToLittleEndian(value: Long): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte()
    )

    /**
     * Stratum v1 transmet `prevhash` sous forme de 8 mots de 32 bits inversés par bloc de 4 octets
     * par rapport à l'ordre interne attendu dans l'en-tête de 80 octets.
     */
    fun stratumPrevHashToInternal32(stratumPrevHashHex: String): ByteArray {
        val raw = hexToBytes(stratumPrevHashHex)
        require(raw.size == 32) { "prevhash doit faire 32 octets, reçu ${raw.size}" }
        val result = ByteArray(32)
        for (wordIndex in 0 until 8) {
            val base = wordIndex * 4
            result[base] = raw[base + 3]
            result[base + 1] = raw[base + 2]
            result[base + 2] = raw[base + 1]
            result[base + 3] = raw[base]
        }
        return result
    }

    /**
     * Calcule le Merkle Root (ordre interne 32 octets) à partir du job Stratum v1.
     */
    fun computeMerkleRootInternal(
        coinb1Hex: String,
        extranonce1Hex: String,
        extranonce2Hex: String,
        coinb2Hex: String,
        merkleBranchHexList: List<String>
    ): ByteArray {
        val coinbase = hexToBytes(coinb1Hex) +
            hexToBytes(extranonce1Hex) +
            hexToBytes(extranonce2Hex) +
            hexToBytes(coinb2Hex)

        var currentHash = doubleSha256(coinbase)
        for (branchHex in merkleBranchHexList) {
            val branchBytes = hexToBytes(branchHex)
            require(branchBytes.size == 32) { "Branche Merkle invalide (${branchBytes.size} octets)" }
            currentHash = doubleSha256(currentHash + branchBytes)
        }
        return currentHash
    }

    /**
     * Construit l'en-tête de bloc Bitcoin de 80 octets à partir des champs internes (little-endian).
     */
    fun buildBlockHeader80Bytes(
        version: Long,
        prevHashInternal32: ByteArray,
        merkleRootInternal32: ByteArray,
        ntime: Long,
        nbits: Long,
        nonce: Long
    ): ByteArray {
        require(prevHashInternal32.size == 32) { "prevHashInternal doit faire 32 octets" }
        require(merkleRootInternal32.size == 32) { "merkleRootInternal doit faire 32 octets" }
        val header = ByteArray(80)
        System.arraycopy(uint32ToLittleEndian(version), 0, header, 0, 4)
        System.arraycopy(prevHashInternal32, 0, header, 4, 32)
        System.arraycopy(merkleRootInternal32, 0, header, 36, 32)
        System.arraycopy(uint32ToLittleEndian(ntime), 0, header, 68, 4)
        System.arraycopy(uint32ToLittleEndian(nbits), 0, header, 72, 4)
        System.arraycopy(uint32ToLittleEndian(nonce), 0, header, 76, 4)
        return header
    }

    /**
     * Construit l'en-tête de bloc de 80 octets à partir d'un job Stratum v1.
     */
    fun buildBlockHeaderFromStratum(
        versionHex: String,
        stratumPrevHashHex: String,
        coinb1Hex: String,
        extranonce1Hex: String,
        extranonce2Hex: String,
        coinb2Hex: String,
        merkleBranchHexList: List<String>,
        ntimeHex: String,
        nbitsHex: String,
        nonce: Long
    ): ByteArray {
        val version = versionHex.trim().toLong(16)
        val ntime = ntimeHex.trim().toLong(16)
        val nbits = nbitsHex.trim().toLong(16)
        val prevHashInternal = stratumPrevHashToInternal32(stratumPrevHashHex)
        val merkleRootInternal = computeMerkleRootInternal(
            coinb1Hex = coinb1Hex,
            extranonce1Hex = extranonce1Hex,
            extranonce2Hex = extranonce2Hex,
            coinb2Hex = coinb2Hex,
            merkleBranchHexList = merkleBranchHexList
        )
        return buildBlockHeader80Bytes(
            version = version,
            prevHashInternal32 = prevHashInternal,
            merkleRootInternal32 = merkleRootInternal,
            ntime = ntime,
            nbits = nbits,
            nonce = nonce
        )
    }

    /**
     * Surcharge pratique pour les tests vectoriels utilisant l'ordre d'affichage big-endian
     * (ex. bloc genesis Bitcoin).
     */
    fun buildBlockHeaderFromDisplayHex(
        version: Long,
        prevHashDisplayHex: String,
        merkleRootDisplayHex: String,
        ntime: Long,
        nbits: Long,
        nonce: Long
    ): ByteArray {
        val prevInternal = hexToBytes(prevHashDisplayHex).reversedArray()
        val merkleInternal = hexToBytes(merkleRootDisplayHex).reversedArray()
        return buildBlockHeader80Bytes(version, prevInternal, merkleInternal, ntime, nbits, nonce)
    }

    /**
     * Retourne le hash d'en-tête au format d'affichage big-endian (64 caractères hex).
     */
    fun headerHashDisplayHex(header80: ByteArray): String {
        require(header80.size == 80) { "L'en-tête de bloc doit faire exactement 80 octets" }
        return bytesToHex(doubleSha256(header80).reversedArray())
    }

    /**
     * Convertit le champ compact `nbits` (4 octets) en cible 256 bits.
     */
    fun nbitsToTarget(nbits: Long): BigInteger {
        val exponent = ((nbits ushr 24) and 0xFF).toInt()
        val mantissa = nbits and 0x007FFFFFL
        if ((nbits and 0x00800000L) != 0L || mantissa == 0L) {
            return BigInteger.ZERO
        }
        return if (exponent <= 3) {
            BigInteger.valueOf(mantissa ushr (8 * (3 - exponent)))
        } else {
            BigInteger.valueOf(mantissa).shiftLeft(8 * (exponent - 3))
        }
    }

    /**
     * Convertit la difficulté du pool (pdiff) en cible 256 bits : target = DIFF1_TARGET / difficulty.
     */
    fun difficultyToTarget(difficulty: Double): BigInteger {
        require(difficulty > 0.0) { "La difficulté doit être strictement positive" }
        val target = BigDecimal(DIFFICULTY_1_TARGET)
            .divide(BigDecimal.valueOf(difficulty), 0, RoundingMode.FLOOR)
            .toBigInteger()
        val max256 = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)
        return target.min(max256)
    }

    /**
     * Vérifie localement si le double SHA-256 d'un en-tête de 80 octets est inférieur ou égal à `target`.
     */
    fun verifyHeaderAgainstTarget(header80: ByteArray, target: BigInteger): Boolean {
        if (header80.size != 80 || target <= BigInteger.ZERO) return false
        val digestLittleEndian = doubleSha256(header80)
        val hashValue = BigInteger(1, digestLittleEndian.reversedArray())
        return hashValue <= target
    }

    /**
     * Calcule la cible en hexadécimal sur 64 caractères (big-endian) pour l'envoi au service distant.
     */
    fun targetToHex64(target: BigInteger): String {
        val raw = target.toString(16)
        return raw.padStart(64, '0').takeLast(64)
    }
}
