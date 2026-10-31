package com.nuvio.tv.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.domain.repository.AddonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** One-shot read-only Stremio import. Neither password nor session token is persisted. */
internal suspend fun importStremioAddons(email: String, password: String, repository: AddonRepository): Pair<Int, Int> = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
    fun call(method: String, body: JSONObject): JSONObject {
        val request = Request.Builder().url("https://api.strem.io/api/$method")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        return client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Service Stremio indisponible." }
            val json = JSONObject(response.body?.string() ?: "{}")
            check(!json.has("error")) { "Connexion refusée par Stremio. Vérifiez vos identifiants." }
            json.optJSONObject("result") ?: error("Réponse Stremio inattendue.")
        }
    }
    val login = call("login", JSONObject().put("email", email.trim()).put("password", password))
    val token = login.optString("authKey").takeIf { it.isNotBlank() } ?: error("Connexion Stremio incomplète.")
    val addons = call("addonCollectionGet", JSONObject().put("authKey", token)).optJSONArray("addons") ?: error("Liste des extensions absente.")
    var imported = 0
    var failed = 0
    for (index in 0 until addons.length()) {
        val url = addons.optJSONObject(index)?.optString("transportUrl").orEmpty()
        if (!url.startsWith("https://")) { failed++; continue }
        try {
            when (repository.fetchAddon(url)) {
                is NetworkResult.Success -> { repository.addAddon(url); imported++ }
                else -> failed++
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { failed++ }
    }
    imported to failed
}

@Composable
internal fun StremioImportDialog(repository: AddonRepository, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Importe vos extensions. Le suivi de lecture reste local à Nawflix.") }
    val scope = rememberCoroutineScope()
    androidx.compose.material3.MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = androidx.compose.ui.graphics.Color.White,
            onPrimary = androidx.compose.ui.graphics.Color.Black,
            surface = androidx.compose.ui.graphics.Color(0xFF202126),
            onSurface = androidx.compose.ui.graphics.Color.White
        )
    ) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Importer depuis Stremio") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message)
            OutlinedTextField(email, { email = it }, label = { Text("Adresse e-mail") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Mot de passe") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = {
        TextButton(enabled = !busy && email.isNotBlank() && password.isNotBlank(), onClick = {
            busy = true
            scope.launch {
                try {
                    val (ok, failed) = importStremioAddons(email, password, repository)
                    message = "$ok extension(s) importée(s), $failed indisponible(s). Vous pouvez fermer cette fenêtre."
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { message = error.message?.takeIf { it.startsWith("Connexion") || it.startsWith("Service") || it.startsWith("Réponse") || it.startsWith("Liste") } ?: "Import impossible. Vérifiez votre connexion et réessayez." }
                finally { password = ""; busy = false }
            }
        }) { Text(if (busy) "Import en cours…" else "Importer") }
    }, dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Fermer") } })
    }
}
