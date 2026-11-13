package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProgramaModel(app: Application, private val estado: SavedStateHandle) : AndroidViewModel(app) {
    private val servicios = Servicios.de(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    // null = Room todavía no respondió; evita mostrar «reintentar» o «vacío» por un instante.
    val cartelera = repo.cartelera.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val charlas = cartelera.map { it?.charlas.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    // Los filtros viven en SavedStateHandle: sobreviven a la rotación y a que Android mate el
    // proceso, pero una tarea nueva (sin estado guardado) arranca con los valores por defecto.
    val dia = estado.getStateFlow("dia", 0)
    val tema = estado.getStateFlow("tema", "Todos")
    fun elegirDia(valor: Int) { estado["dia"] = valor }
    fun elegirTema(valor: String) { estado["tema"] = valor }
    // true si el último intento de sincronizar falló; se apaga recién cuando uno tiene éxito.
    val fallo = MutableStateFlow(false)
    val cargando = MutableStateFlow(false)

    /** La carga automática: una por proceso. */
    fun cargarAlAbrir() {
        if (servicios.cargaAutomatica.compareAndSet(false, true)) sincronizar()
    }

    /** Actualización explícita (Actualizar / Reintentar): una por pedido del usuario. */
    fun actualizar() = sincronizar()

    private fun sincronizar() {
        // Con una petición en curso, un segundo toque no suma otro intento.
        if (!cargando.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                repo.sincronizar()
                fallo.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Cualquier error (red, HTTP no 2xx, JSON inválido) deja la edición guardada intacta.
                fallo.value = true
            } finally {
                cargando.value = false
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}
