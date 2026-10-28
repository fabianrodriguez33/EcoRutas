package pe.upeu.ecorecicla.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.data.local.SesionActual
import pe.upeu.ecorecicla.data.remote.ServicioApi
import pe.upeu.ecorecicla.data.remote.crearClienteHttp
import pe.upeu.ecorecicla.data.repository.Enviador
import pe.upeu.ecorecicla.data.repository.PesajeRepositoryImpl
import pe.upeu.ecorecicla.data.repository.RutaRepositoryImpl
import pe.upeu.ecorecicla.data.repository.SesionRepositoryImpl
import pe.upeu.ecorecicla.data.repository.SincronizacionRepositoryImpl
import pe.upeu.ecorecicla.db.EcoDatabase
import pe.upeu.ecorecicla.domain.repository.PesajeRepository
import pe.upeu.ecorecicla.domain.repository.RutaRepository
import pe.upeu.ecorecicla.domain.repository.SesionRepository
import pe.upeu.ecorecicla.domain.repository.SincronizacionRepository
import pe.upeu.ecorecicla.domain.usecase.AnularPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.CerrarRutaUseCase
import pe.upeu.ecorecicla.domain.usecase.CerrarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.CorregirPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.IniciarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarConexionUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPendientesUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPesajesUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPuntoUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPuntosUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarRutaUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.RegistrarPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.RestaurarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import pe.upeu.ecorecicla.presentation.detalle.DetalleViewModel
import pe.upeu.ecorecicla.presentation.detalle.FormularioViewModel
import pe.upeu.ecorecicla.presentation.listado.RutaViewModel
import pe.upeu.ecorecicla.presentation.navigation.Navegador
import pe.upeu.ecorecicla.presentation.navigation.RaizViewModel
import pe.upeu.ecorecicla.presentation.perfil.PerfilViewModel
import pe.upeu.ecorecicla.presentation.sesion.LoginViewModel

/** Cliente HTTP único (el motor y la configuración los aporta platformModule) y servicio de red. */
val redModule = module {
    single { SesionActual(get()) }
    single { crearClienteHttp(get(), get(), get()) }
    single { ServicioApi(get(), get()) }
}

val dataModule = module {
    single { EcoDatabase(get()) }
    single { BaseLocal(get()) }
    single { Enviador(get(), get()) }
    single<SesionRepository> { SesionRepositoryImpl(get(), get(), get(), get(), get()) }
    single<RutaRepository> { RutaRepositoryImpl(get(), get()) }
    single<PesajeRepository> { PesajeRepositoryImpl(get(), get(), get(), get()) }
    single<SincronizacionRepository> { SincronizacionRepositoryImpl(get(), get(), get(), get(), get()) }
}

val domainModule = module {
    factory { ObservarSesionUseCase(get()) }
    factory { IniciarSesionUseCase(get()) }
    factory { RestaurarSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
    factory { ObservarRutaUseCase(get()) }
    factory { ObservarPuntosUseCase(get()) }
    factory { ObservarPuntoUseCase(get()) }
    factory { CerrarRutaUseCase(get()) }
    factory { ObservarPesajesUseCase(get()) }
    factory { ObservarPesajeUseCase(get()) }
    factory { ObservarPendientesUseCase(get()) }
    factory { RegistrarPesajeUseCase(get()) }
    factory { CorregirPesajeUseCase(get()) }
    factory { AnularPesajeUseCase(get()) }
    factory { SincronizarUseCase(get()) }
    factory { ObservarConexionUseCase(get()) }
}

val presentationModule = module {
    single { Navegador() }
    viewModel { RaizViewModel(get(), get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { RutaViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { DetalleViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { FormularioViewModel(get(), get(), get(), get(), get()) }
    viewModel { PerfilViewModel(get(), get(), get()) }
}

/**
 * Arranca Koin con los módulos comunes. [plataforma] debe definir: ApiConfig, HttpClientEngine, SqlDriver,
 * AlmacenSeguro, Reloj y MonitorRed (ver PlatformModule.android.kt).
 */
fun iniciarKoin(plataforma: Module, configuracion: KoinApplication.() -> Unit = {}): KoinApplication =
    startKoin {
        configuracion()
        modules(plataforma, redModule, dataModule, domainModule, presentationModule)
    }
