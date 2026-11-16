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

        // 3. Crypto-specific public address validation
        when (cryptoSymbol.uppercase()) {
            "BTC" -> {
                if (!(trimmed.startsWith("1") || trimmed.startsWith("3") || trimmed.startsWith("bc1"))) {
                    return AddressValidationResult.InvalidFormat("Une adresse Bitcoin publique commence par '1', '3' ou 'bc1'.")
                }
                if (trimmed.length < 26) {
                    return AddressValidationResult.InvalidFormat("Adresse Bitcoin incomplète.")
                }
            }
            "DOGE" -> {
                if (!trimmed.startsWith("D")) {
                    return AddressValidationResult.InvalidFormat("Une adresse Dogecoin publique valide commence par la lettre 'D'.")
                }
                if (trimmed.length < 30) {
                    return AddressValidationResult.InvalidFormat("Adresse Dogecoin trop courte.")
                }
            }
            "XMR" -> {
                // Monero standard address starts with 4, subaddress starts with 8, integrated with 4
                if (!(trimmed.startsWith("4") || trimmed.startsWith("8"))) {
                    return AddressValidationResult.InvalidFormat("Une adresse Monero publique valide commence par un '4' (principale) ou un '8' (sous-adresse).")
                }
                if (trimmed.length < 95) {
                    return AddressValidationResult.InvalidFormat("Longueur d'adresse Monero trop courte (95 à 106 caractères attendus).")
                }
            }
            "KAS" -> {
                if (!trimmed.startsWith("kaspa:")) {
                    return AddressValidationResult.InvalidFormat("Une adresse Kaspa publique commence obligatoirement par 'kaspa:'")
                }
                if (trimmed.length < 20) {
                    return AddressValidationResult.InvalidFormat("Adresse Kaspa incomplète.")
                }
            }
            "RVN" -> {
                if (!trimmed.startsWith("R")) {
                    return AddressValidationResult.InvalidFormat("Une adresse Ravencoin publique commence par la lettre 'R'.")
                }
                if (trimmed.length !in 30..36) {
                    return AddressValidationResult.InvalidFormat("Longueur d'adresse Ravencoin invalide (environ 34 caractères attendus).")
                }
            }
            "ERG" -> {
                if (!trimmed.startsWith("9")) {
                    return AddressValidationResult.InvalidFormat("Une adresse Ergo publique standard commence par '9'.")
                }
                if (trimmed.length < 40) {
                    return AddressValidationResult.InvalidFormat("Adresse Ergo trop courte.")
                }
            }
        }

        if (trimmed.length < 15) {
            return AddressValidationResult.InvalidFormat("L'adresse publique semble trop courte.")
        }

        return AddressValidationResult.Valid
    }
}
