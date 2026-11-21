package com.minerrelay.app.mining

import java.math.BigInteger

sealed class AddressValidationResult {
    object Valid : AddressValidationResult()
    data class DangerousSeedPhraseDetected(
        val wordCount: Int,
        val message: String
    ) : AddressValidationResult()
    data class DangerousPrivateKeyDetected(
        val message: String
    ) : AddressValidationResult()
    data class InvalidFormat(
        val reason: String
    ) : AddressValidationResult()
}

/**
 * Validateur strict d'adresses publiques Bitcoin mainnet :
 * - Base58Check (`1...` P2PKH, `3...` P2SH) avec vérification complète du checksum double SHA-256
 * - Bech32 / Bech32m (`bc1q...`, `bc1p...`) selon BIP-173 et BIP-350
 * - Blocage explicite des phrases secrètes (12-24 mots) et clés privées (WIF, hex, xprv)
 */
object SecurityValidator {

    private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private const val BECH32_CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private const val BECH32_CONST = 1
    private const val BECH32M_CONST = 0x2bc830a3

    fun validatePublicAddress(cryptoSymbol: String, rawInput: String): AddressValidationResult =
        validateBitcoinMainnetAddress(rawInput)

    fun validateBitcoinMainnetAddress(rawInput: String): AddressValidationResult {
        val trimmed = rawInput.trim()

        if (trimmed.isEmpty()) {
            return AddressValidationResult.InvalidFormat("L'adresse Bitcoin ne peut pas être vide.")
        }

        // 1. Détection de phrase secrète (Seed Phrase 12-24 mots)
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size in 11..25) {
            return AddressValidationResult.DangerousSeedPhraseDetected(
                wordCount = words.size,
                message = "ALERTE DE SÉCURITÉ : Vous avez saisi une phrase secrète (${words.size} mots). " +
                    "Ne saisissez JAMAIS votre phrase secrète ni votre clé privée. Seule votre adresse publique de réception est demandée."
            )
        }

        // 2. Détection de clés privées (hex, xprv/yprv/zprv, WIF)
        if ((trimmed.startsWith("0x") && trimmed.length == 66 && trimmed.drop(2).all { it in "0123456789abcdefABCDEF" }) ||
            (trimmed.length == 64 && trimmed.all { it in "0123456789abcdefABCDEF" })
        ) {
            return AddressValidationResult.DangerousPrivateKeyDetected(
                "DANGER : Ceci ressemble à une clé privée hexadécimale. N'utilisez jamais de clé privée ici !"
            )
        }
        if (trimmed.startsWith("xprv") || trimmed.startsWith("yprv") || trimmed.startsWith("zprv") || trimmed.startsWith("tprv")) {
            return AddressValidationResult.DangerousPrivateKeyDetected(
                "DANGER : Clé privée étendue (xprv/zprv) détectée. Saisissez uniquement votre adresse publique Bitcoin."
            )
        }
        if ((trimmed.startsWith("5") && trimmed.length == 51) ||
            ((trimmed.startsWith("K") || trimmed.startsWith("L")) && trimmed.length == 52)
        ) {
            return AddressValidationResult.DangerousPrivateKeyDetected(
                "DANGER : Clé privée WIF détectée (5.../K.../L...). Utilisez uniquement votre adresse publique de réception."
            )
        }

        // 3. Adresse SegWit / Taproot Bech32 ou Bech32m (bc1...)
        if (trimmed.startsWith("bc1", ignoreCase = true)) {
            return validateBech32Address(trimmed)
        }

        // 4. Adresse Legacy Base58Check (1... ou 3...)
        if (trimmed.startsWith("1") || trimmed.startsWith("3")) {
            return validateBase58CheckAddress(trimmed)
        }

        return AddressValidationResult.InvalidFormat(
            "Adresse Bitcoin invalide : formats acceptés 1... (P2PKH), 3... (P2SH) ou bc1... (Bech32/Bech32m)."
        )
    }

    private fun validateBase58CheckAddress(address: String): AddressValidationResult {
        if (address.length !in 26..35) {
            return AddressValidationResult.InvalidFormat("Longueur d'adresse Base58 invalide (${address.length} caractères).")
        }

        val decoded = decodeBase58(address)
            ?: return AddressValidationResult.InvalidFormat("Caractères Base58 invalides dans l'adresse Bitcoin.")

        if (decoded.size != 25) {
            return AddressValidationResult.InvalidFormat("Charge Base58Check invalide (${decoded.size} octets au lieu de 25).")
        }

        val version = decoded[0].toInt() and 0xFF
        if (address.startsWith("1") && version != 0x00) {
            return AddressValidationResult.InvalidFormat("Préfixe de version Base58 incohérent pour une adresse 1...")
        }
        if (address.startsWith("3") && version != 0x05) {
            return AddressValidationResult.InvalidFormat("Préfixe de version Base58 incohérent pour une adresse 3...")
        }

        val payload = decoded.copyOfRange(0, 21)
        val expectedChecksum = BitcoinCrypto.doubleSha256(payload).copyOfRange(0, 4)
        val actualChecksum = decoded.copyOfRange(21, 25)
        if (!expectedChecksum.contentEquals(actualChecksum)) {
            return AddressValidationResult.InvalidFormat("Checksum Base58Check invalide : vérifiez l'exactitude de l'adresse.")
        }

        return AddressValidationResult.Valid
    }

    private fun decodeBase58(input: String): ByteArray? {
        var value = BigInteger.ZERO
        val base = BigInteger.valueOf(58)
        for (ch in input) {
            val digit = BASE58_ALPHABET.indexOf(ch)
            if (digit < 0) return null
            value = value.multiply(base).add(BigInteger.valueOf(digit.toLong()))
        }
        var bytes = value.toByteArray()
        if (bytes.isNotEmpty() && bytes[0] == 0.toByte()) {
            bytes = bytes.copyOfRange(1, bytes.size)
        }
        val leadingZeros = input.takeWhile { it == '1' }.length
        return ByteArray(leadingZeros) + bytes
    }

    private fun validateBech32Address(address: String): AddressValidationResult {
        val hasLower = address.any { it in 'a'..'z' }
        val hasUpper = address.any { it in 'A'..'Z' }
        if (hasLower && hasUpper) {
            return AddressValidationResult.InvalidFormat("Adresse Bech32 invalide : casse mixte interdite.")
        }

        val lower = address.lowercase()
        if (lower.length > 90) {
            return AddressValidationResult.InvalidFormat("Adresse Bech32 trop longue (> 90 caractères).")
        }

        val sepIndex = lower.lastIndexOf('1')
        if (sepIndex != 2 || lower.substring(0, sepIndex) != "bc") {
            return AddressValidationResult.InvalidFormat("Préfixe Bech32 invalide : seul le réseau principal 'bc1' est accepté.")
        }

        val dataPart = lower.substring(sepIndex + 1)
        if (dataPart.length < 7) {
            return AddressValidationResult.InvalidFormat("Adresse Bech32 trop courte.")
        }

        val data5Bit = IntArray(dataPart.length)
        for (i in dataPart.indices) {
            val idx = BECH32_CHARSET.indexOf(dataPart[i])
            if (idx < 0) {
                return AddressValidationResult.InvalidFormat("Caractère Bech32 invalide ('${dataPart[i]}').")
            }
            data5Bit[i] = idx
        }

        val polymod = bech32Polymod(bech32HrpExpand("bc") + data5Bit)
        val witnessVersion = data5Bit[0]
        if (witnessVersion !in 0..16) {
            return AddressValidationResult.InvalidFormat("Version witness Bech32 invalide ($witnessVersion).")
        }

        val expectedConst = if (witnessVersion == 0) BECH32_CONST else BECH32M_CONST
        if (polymod != expectedConst) {
            return AddressValidationResult.InvalidFormat(
                if (witnessVersion == 0) "Checksum Bech32 invalide." else "Checksum Bech32m invalide."
            )
        }

        val program = convertBits5To8(data5Bit.copyOfRange(1, data5Bit.size - 6))
            ?: return AddressValidationResult.InvalidFormat("Encodage programme witness Bech32 invalide.")

        if (program.size !in 2..40) {
            return AddressValidationResult.InvalidFormat("Longueur de programme witness invalide (${program.size} octets).")
        }
        if (witnessVersion == 0 && program.size != 20 && program.size != 32) {
            return AddressValidationResult.InvalidFormat("Adresse bc1q invalide : doit contenir 20 ou 32 octets.")
        }
        if (witnessVersion == 1 && program.size != 32) {
            return AddressValidationResult.InvalidFormat("Adresse Taproot bc1p invalide : doit contenir 32 octets.")
        }

        return AddressValidationResult.Valid
    }

    private fun bech32HrpExpand(hrp: String): IntArray {
        val out = IntArray(hrp.length * 2 + 1)
        for (i in hrp.indices) {
            out[i] = hrp[i].code ushr 5
            out[hrp.length + 1 + i] = hrp[i].code and 31
        }
        out[hrp.length] = 0
        return out
    }

    private fun bech32Polymod(values: IntArray): Int {
        val gen = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
        var chk = 1
        for (v in values) {
            val top = chk ushr 25
            chk = ((chk and 0x1ffffff) shl 5) xor v
            for (i in 0 until 5) {
                if (((top ushr i) and 1) != 0) {
                    chk = chk xor gen[i]
                }
            }
        }
        return chk
    }

    private fun convertBits5To8(data: IntArray): ByteArray? {
        var acc = 0
        var bits = 0
        val out = ArrayList<Byte>(data.size)
        for (value in data) {
            if (value < 0 || value ushr 5 != 0) return null
            acc = (acc shl 5) or value
            bits += 5
            while (bits >= 8) {
                bits -= 8
                out.add(((acc ushr bits) and 0xff).toByte())
            }
        }
        if (bits >= 5 || ((acc shl (8 - bits)) and 0xff) != 0) {
            return null
        }
        return out.toByteArray()
    }
}
