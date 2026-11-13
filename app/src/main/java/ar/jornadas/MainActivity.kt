package ar.jornadas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import ar.jornadas.ui.*

class MainActivity : ComponentActivity() {
    private val model: ProgramaModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.hasExtra("servicio_disponible")) {
            model.mesa.disponible = intent.getBooleanExtra("servicio_disponible", true)
            intent.removeExtra("servicio_disponible")
        }
        setContent { Portada(model) }
        // Después de aplicar el extra de arriba, para que la primera petición ya lo vea.
        // Es una carga por proceso: rotar o reabrir la Activity no vuelve a pedir.
        model.cargarAlAbrir()
    }
}
