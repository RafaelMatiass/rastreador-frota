package br.com.rastreadorfrota.rota

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.osmdroid.util.GeoPoint

/**
 * Atalho para navegar com voz no Waze ou no Google Maps até o mesmo destino da rota.
 * Sem o app instalado, o link abre no navegador (Waze) ou num app de mapas qualquer (geo:).
 */
object NavegacaoExterna {

    fun abrirWaze(context: Context, destino: GeoPoint) {
        abrir(context, Uri.parse("https://waze.com/ul?ll=${destino.latitude},${destino.longitude}&navigate=yes"))
    }

    fun abrirGoogleMaps(context: Context, destino: GeoPoint) {
        val navegacao = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${destino.latitude},${destino.longitude}"))
            .setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(navegacao)
        } catch (e: ActivityNotFoundException) {
            abrir(context, Uri.parse("geo:${destino.latitude},${destino.longitude}?q=${destino.latitude},${destino.longitude}"))
        }
    }

    private fun abrir(context: Context, uri: Uri) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            // Nenhum app de mapas nem navegador: não há o que abrir.
        }
    }
}
