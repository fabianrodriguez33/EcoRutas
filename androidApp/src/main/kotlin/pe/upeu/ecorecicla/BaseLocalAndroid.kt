package pe.upeu.ecorecicla

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pe.upeu.ecorecicla.db.EcoDatabase

/** Controlador de SQLDelight para Android: el esquema se define en el código común. */
fun crearControladorAndroid(contexto: Context): SqlDriver =
    AndroidSqliteDriver(EcoDatabase.Schema, contexto, "ecorutas.db")
