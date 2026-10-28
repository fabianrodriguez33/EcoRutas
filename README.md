# EcoRutas

Aplicación Android (Kotlin Multiplatform, Compose) para los pesajes en ruta de EcoRecicla. Consume EcoAPI y funciona sin conexión.

## Ejecución

1. Levantar el backend (`docker compose up -d --build` en EcoAPI, o `./mvnw spring-boot:run`).
2. Abrir un emulador o dispositivo y ejecutar:

```bash
./gradlew :androidApp:installDebug
```

La dirección base está en `ApiConfig.URL_EMULADOR` (`http://10.0.2.2:8080/api/v1/`). En un teléfono físico use la IP de la PC.

Pruebas de `commonTest`:

```bash
./gradlew :shared:testAndroidHostTest
```

Usuarios: `operario1`, `supervisor1`, `admin` con contraseña `Eco2026*`.

## Estructura (`shared/src/commonMain/kotlin/pe/upeu/ecorecicla`)

- `domain/`: modelos (sección 4.5), `ErrorApi` sellado, interfaces de repositorio y casos de uso (`SincronizarUseCase`).
- `data/remote/`: `ClienteHttp` (un único `HttpClient` con Koin, cabecera Authorization centralizada), `ApiConfig`, `MapeoErrores`, `ServicioApi` y `dto/`.
- `data/local/`: `AlmacenSeguro` (contrato), `BaseLocal` (SQLDelight), `SesionActual`, `TokenJwt`.
- `data/repository/`: implementaciones offline-first y `Enviador`.
- `presentation/`: `sesion`, `listado`, `detalle`, `perfil`, `navigation`, `theme`.
- `di/`: módulos Koin. En `androidApp`: `AlmacenSeguroAndroid`, `BaseLocalAndroid`, `PlatformModule.android`.

## Decisiones

- **Offline-first:** la interfaz solo lee de SQLDelight mediante flujos. Todo registro se guarda como pendiente con id negativo (`siguienteIdLocal`) y se envía al instante si hay red.
- **Sincronización:** sube lo pendiente y luego baja lo actualizado; la fila provisional se reemplaza por la definitiva en una transacción. Se ejecuta a mano y al recuperar la conexión.
- **Errores:** 400 bajo el campo; 401 borra el token y vuelve al login; 403 informa y mantiene la sesión; 404 informa y refresca; 409 muestra el motivo; 500 ofrece reintento.
- **Seguridad:** el token se guarda en `EncryptedSharedPreferences` (Keystore). Ocultar botones por privilegio es solo experiencia de uso; el servicio rechaza con 403.
- **Cierre de sesión:** borra el token y los datos locales para el turno siguiente (avisa si hay pendientes).
- Solo Android; se eliminaron los targets de iOS.
