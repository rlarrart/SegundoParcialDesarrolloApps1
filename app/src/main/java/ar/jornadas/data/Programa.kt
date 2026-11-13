package ar.jornadas.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

// Existe una fila (id = 1) si alguna vez se descargó una edición completa del programa,
// aunque haya venido con cero charlas. Sin esa fila, «lista vacía» no distingue
// «nunca descargué» de «la organización publicó un programa vacío».
@Entity(tableName = "edicion")
data class Edicion(@PrimaryKey val id: Int = 1)

data class Cartelera(val charlas: List<Charla>, val hayEdicion: Boolean)

@Dao
abstract class CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    abstract fun observar(): Flow<List<Charla>>

    @Query("SELECT COUNT(*) FROM edicion")
    abstract fun ediciones(): Flow<Int>

    @Query("SELECT id FROM charlas WHERE favorita = 1")
    abstract suspend fun marcadas(): List<String>

    @Query("DELETE FROM charlas")
    abstract suspend fun vaciar()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertar(fichas: List<Charla>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun registrar(edicion: Edicion)

    // La instantánea reemplaza al catálogo completo, en una sola transacción: o queda la
    // edición anterior intacta o queda la nueva. Las marcas se leen dentro de la
    // transacción, así que incluyen cualquier cambio hecho mientras viajaba la respuesta.
    @Transaction
    open suspend fun reemplazar(fichas: List<Charla>) {
        val marcadas = marcadas().toSet()
        vaciar()
        insertar(fichas.map { it.copy(favorita = it.id in marcadas) })
        registrar(Edicion())
    }

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    abstract suspend fun alternar(id: String)
}

val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `edicion` (`id` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        // Quien ya tenía charlas descargadas conserva esa edición como válida.
        db.execSQL("INSERT INTO edicion (id) SELECT 1 WHERE EXISTS (SELECT 1 FROM charlas)")
    }
}

@Database(entities = [Charla::class, Edicion::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}
