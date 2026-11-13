package ar.jornadas.data

import kotlinx.coroutines.flow.combine

class AgendaRepository(private val api: ProgramaApi, private val dao: CarteleraDao) {
    val cartelera = combine(dao.observar(), dao.ediciones()) { charlas, ediciones ->
        Cartelera(charlas, hayEdicion = ediciones > 0)
    }

    suspend fun sincronizar() {
        val fichas = api.consultar().map {
            Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
        }
        dao.reemplazar(fichas)
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)
}
