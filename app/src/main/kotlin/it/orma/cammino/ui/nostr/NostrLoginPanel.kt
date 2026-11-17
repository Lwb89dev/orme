package it.orma.cammino.ui.nostr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.orma.cammino.nostr.LoginFailure
import it.orma.cammino.nostr.NostrAccount
import it.orma.cammino.ui.i18n.uiText

/**
 * L'accesso Nostr, uguale nell'onboarding e nelle Impostazioni: Amber (NIP-55) oppure un bunker (NIP-46).
 * Nessun campo per una chiave privata: l'app non la chiede e non la conserva.
 */
@Composable
fun NostrLoginPanel(account: NostrAccount, allowLogout: Boolean, modifier: Modifier = Modifier) {
    val identity by account.identity.collectAsStateWithLifecycle()
    val busy by account.busy.collectAsStateWithLifecycle()
    val failure by account.failure.collectAsStateWithLifecycle()
    var link by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val current = identity
        if (current != null) {
            Text(
                uiText("Accesso effettuato", "Signed in") + ": " + shortNpub(current.npub),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (allowLogout) {
                OutlinedButton(onClick = { account.logout() }, modifier = Modifier.fillMaxWidth()) {
                    Text(uiText("Esci da Nostr", "Sign out of Nostr"))
                }
            }
        } else {
            Button(onClick = { account.loginWithAmber() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(uiText("Accedi con Amber", "Sign in with Amber"))
            }
            OutlinedTextField(
                value = link,
                onValueChange = { link = it; account.clearFailure() },
                label = { Text(uiText("Link bunker://", "bunker:// link")) },
                singleLine = true,
                // Un link non è una parola: niente correzione automatica né suggerimenti.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = { account.loginWithBunker(link) },
                enabled = link.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(uiText("Accedi con il bunker", "Sign in with the bunker"))
            }
            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(uiText("In attesa del bunker…", "Waiting for the bunker…"), style = MaterialTheme.typography.bodyMedium)
                }
            }
            failure?.let {
                Text(
                    failureText(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@Composable
private fun failureText(failure: LoginFailure): String = when (failure) {
    LoginFailure.AMBER -> uiText(
        "Amber non si è aperta o la sua risposta non era valida. È installata?",
        "Amber did not open or its answer was not valid. Is it installed?",
    )
    LoginFailure.INVALID_LINK -> uiText(
        "Link non valido: deve iniziare con bunker:// e indicare un relay wss://.",
        "Invalid link: it must start with bunker:// and name a wss:// relay.",
    )
    LoginFailure.NO_ANSWER -> uiText("Il bunker non ha risposto.", "The bunker did not answer.")
    LoginFailure.REFUSED -> uiText("Il bunker ha rifiutato l'accesso.", "The bunker refused the sign-in.")
}

/** `npub1abcdefgh…xyz123`: abbastanza per riconoscersi, non l'intera chiave. */
internal fun shortNpub(npub: String): String =
    if (npub.length > 20) npub.take(12) + "…" + npub.takeLast(6) else npub
