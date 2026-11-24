package com.example.domain

import com.example.data.model.PaymentProvider

data class PaymentInstruction(
    val provider: PaymentProvider,
    val recipientLabel: String,
    val merchantCodeOrNumber: String,
    val steps: List<String>,
    val securityNotice: String
)

/**
 * Architecture extensible permettant le branchement ultérieur d'une passerelle
 * de paiement officielle (Airtel Money Open API / Moov Africa API).
 */
interface PaymentGateway {
    fun getInstructions(provider: PaymentProvider, amountFcfa: Long): PaymentInstruction
    fun validateReference(reference: String): Boolean
}

class GabonManualMobileMoneyGateway : PaymentGateway {
    override fun getInstructions(provider: PaymentProvider, amountFcfa: Long): PaymentInstruction {
        val formatted = CopyGenerator.formatFcfa(amountFcfa)
        return when (provider) {
            PaymentProvider.AIRTEL_MONEY -> PaymentInstruction(
                provider = provider,
                recipientLabel = "KOMBA BOOST GABON (Airtel Money)",
                merchantCodeOrNumber = "074 00 00 00 (Compte Marchand / Mobile Money)",
                steps = listOf(
                    "Composez *150# ou ouvrez votre application Airtel Money.",
                    "Effectuez un transfert exact de $formatted vers le compte indiqué.",
                    "Relevez l'ID / Référence de transaction reçu par SMS.",
                    "Saisissez la référence ci-dessous et joignez une capture d'écran si possible."
                ),
                securityNotice = "Ne saisissez et ne transmettez JAMAIS votre code secret (PIN) Mobile Money."
            )
            PaymentProvider.MOOV_MONEY -> PaymentInstruction(
                provider = provider,
                recipientLabel = "KOMBA BOOST GABON (Moov Money)",
                merchantCodeOrNumber = "062 00 00 00 (Mobile Money)",
                steps = listOf(
                    "Composez *555# ou ouvrez Moov Money.",
                    "Transférez exactement $formatted.",
                    "Copiez la référence SMS de confirmation.",
                    "Soumettez votre preuve : l'abonnement sera activé après vérification administrative."
                ),
                securityNotice = "Ne saisissez et ne transmettez JAMAIS votre code secret (PIN) Mobile Money."
            )
        }
    }

    override fun validateReference(reference: String): Boolean {
        val clean = reference.trim()
        return clean.length in 4..80
    }
}
