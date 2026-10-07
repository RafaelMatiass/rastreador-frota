package br.com.rastreadorfrota.rota

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint

val PERMISSOES_LOCALIZACAO = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

fun temPermissaoLocalizacao(context: Context): Boolean =
    PERMISSOES_LOCALIZACAO.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/** Localização atual do aparelho pelo FusedLocationProvider; null se não houver permissão ou leitura. */
@SuppressLint("MissingPermission") // a permissão é conferida logo na primeira linha
suspend fun localizacaoAtual(context: Context): GeoPoint? {
    if (!temPermissaoLocalizacao(context)) return null
    val cliente = LocationServices.getFusedLocationProviderClient(context)
    val local = try {
        // getCurrentLocation pede uma leitura nova; lastLocation é o plano B (pode ser antiga).
        cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            ?: cliente.lastLocation.await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null // GPS desligado, Play Services indisponível etc.
    }
    return local?.let { GeoPoint(it.latitude, it.longitude) }
}
