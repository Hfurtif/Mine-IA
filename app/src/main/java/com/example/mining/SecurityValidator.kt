package com.example.mining

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

object SecurityValidator {

    /**
     * Strict check to protect beginner users from leaking their private seed phrases or private keys.
     */
    fun validatePublicAddress(cryptoSymbol: String, rawInput: String): AddressValidationResult {
        val trimmed = rawInput.trim()

        if (trimmed.isEmpty()) {
            return AddressValidationResult.InvalidFormat("L'adresse ne peut pas être vide.")
        }

        // 1. Check for Seed Phrase (12, 15, 18, 24 words separated by whitespace)
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size in 11..25) {
            return AddressValidationResult.DangerousSeedPhraseDetected(
                wordCount = words.size,
                message = "ALERTE DE SÉCURITÉ CRITIQUE : Vous avez collé une phrase secrète de récupération (Seed Phrase de ${words.size} mots) ! " +
                        "Ne partagez JAMAIS vos mots secrets. L'application ne demande STRICTEMENT que votre adresse publique de réception pour y verser vos gains de minage."
            )
        }

        // 2. Check for private keys patterns (e.g. 64-hex private key or typical private prefixes)
        if (trimmed.startsWith("0x") && trimmed.length == 66 && trimmed.drop(2).all { it in "0123456789abcdefABCDEF" }) {
            return AddressValidationResult.DangerousPrivateKeyDetected(
                "DANGER : Ceci ressemble à une clé privée hexadécimale. Une clé privée donne un accès total à vos fonds. Utilisez uniquement l'adresse publique."
            )
        }
        if (trimmed.startsWith("xprv") || trimmed.startsWith("yprv") || trimmed.startsWith("zprv")) {
            return AddressValidationResult.DangerousPrivateKeyDetected(
                "DANGER : Vous tentez d'utiliser une clé maîtresse privée étendue (xprv/zprv). Ne partagez jamais cette clé !"
            )
        }

        // 3. Validation stricte d'adresse publique Bitcoin (BTC uniquement)
        if (!(trimmed.startsWith("1") || trimmed.startsWith("3") || trimmed.startsWith("bc1", ignoreCase = true))) {
            return AddressValidationResult.InvalidFormat("Une adresse Bitcoin (BTC) publique commence par '1', '3' ou 'bc1'.")
        }
        if (trimmed.length < 26) {
            return AddressValidationResult.InvalidFormat("Adresse Bitcoin incomplète (minimum 26 caractères).")
        }

        return AddressValidationResult.Valid
    }
}
