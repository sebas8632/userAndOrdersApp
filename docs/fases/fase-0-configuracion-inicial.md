# Fase 0: Configuración inicial del proyecto

**Fecha:** 2026-10-08
**Proyecto:** UserAndOrdersApp (`com.example.userandordersapp`)
**Estado:** Completada. Compila, pero los tests de instrumentación no se ejecutaron (requieren emulador o dispositivo).

---

## 1. Objetivo

Preparar la base técnica de la app antes de escribir funcionalidad. Esto incluye:

- Declarar todas las dependencias en el catálogo de versiones (`gradle/libs.versions.toml`).
- Conectar esas dependencias a los módulos de Gradle.
- Configurar Hilt como inyector de dependencias y su clase `Application`.
- Configurar el runner de tests de instrumentación con Hilt.

---

## 2. Dependencias agregadas

Todas las versiones se consultaron en Maven Central y Google Maven, usando solo versiones estables (sin alpha, beta ni RC).

### 2.1 Coroutines, Red y Serialización

| Librería | Versión | Alias en el TOML | Configuración |
|---|---|---|---|
| kotlinx-coroutines-core | 1.11.0 | `kotlinx-coroutines-core` | `implementation` |
| kotlinx-coroutines-android | 1.11.0 | `kotlinx-coroutines-android` | `implementation` |
| Retrofit | 3.0.0 | `retrofit` | `implementation` |
| OkHttp | 5.5.0 | `okhttp` | `implementation` |
| OkHttp logging-interceptor | 5.5.0 | `okhttp-logging-interceptor` | `implementation` |
| retrofit converter-kotlinx-serialization | 3.0.0 | `retrofit-converter-kotlinx-serialization` | `implementation` |
| kotlinx-serialization-json | 1.11.0 | `kotlinx-serialization-json` | `implementation` |

Notas:
- Retrofit 3 requiere OkHttp 5, por eso ambas están en la versión 5.x.
- Se eligió `converter-kotlinx-serialization` en lugar de Gson porque es la opción nativa de Kotlin.

### 2.2 Persistencia

| Librería | Versión | Alias en el TOML | Configuración |
|---|---|---|---|
| Room runtime | 2.8.5 | `androidx-room-runtime` | `implementation` |
| Room ktx | 2.8.5 | `androidx-room-ktx` | `implementation` |
| Room compiler | 2.8.5 | `androidx-room-compiler` | `ksp` |
| DataStore preferences | 1.2.1 | `androidx-datastore-preferences` | `implementation` |

Notas:
- Room usa KSP para generar código, no kapt.
- DataStore se fijó en 1.2.1 (estable). La 1.3.0-alpha11 existe pero no se usa.

### 2.3 Inyección de dependencias

| Librería | Versión | Alias en el TOML | Configuración |
|---|---|---|---|
| Hilt android | 2.60.1 | `hilt-android` | `implementation` |
| Hilt compiler | 2.60.1 | `hilt-compiler` | `ksp` (main) y `kspAndroidTest` |
| hilt-navigation-compose | 1.4.0 | `androidx-hilt-navigation-compose` | `implementation` |
| hilt-android-testing | 2.60.1 | `hilt-android-testing` | `androidTestImplementation` |

Hilt usa KSP en lugar de kapt.

### 2.4 Compose y arquitectura

| Librería | Versión | Alias en el TOML | Configuración |
|---|---|---|---|
| lifecycle-viewmodel-compose | 2.11.0 | `androidx-lifecycle-viewmodel-compose` | `implementation` |
| lifecycle-runtime-compose | 2.11.0 | `androidx-lifecycle-runtime-compose` | `implementation` |
| navigation-compose | 2.10.2 | `androidx-navigation-compose` | `implementation` |
| coil-compose | 3.3.0 | `coil-compose` | `implementation` |
| coil-network-okhttp | 3.3.0 | `coil-network-okhttp` | `implementation` |

Notas:
- Compose ya estaba configurado desde el inicio con `compose-bom` 2026.02.01. Ese BOM alinea los módulos `ui`, `material3`, `foundation` y `tooling`, por eso no llevan versión en el catálogo.
- `lifecycle-viewmodel-compose` y `lifecycle-runtime-compose` reutilizan la referencia `lifecycleRuntimeKtx`, porque pertenecen a la misma familia de versiones.
- **Coil se fijó en 3.3.0, no en 3.6.3.** La 3.6.3 depende de `kotlin-stdlib` 2.4.10, y el proyecto usa Kotlin 2.2.10. Esa diferencia rompía la compilación con el error `incompatible version of Kotlin`. La 3.3.0 depende de `kotlin-stdlib` 2.2.0, que es compatible. Si el proyecto sube a Kotlin 2.4.x, se puede volver a la 3.6.x.

### 2.5 Testing

| Librería | Versión | Alias en el TOML | Configuración |
|---|---|---|---|
| JUnit | 4.13.2 | `junit` | `testImplementation` (ya existía) |
| Mockito core | 5.24.0 | `mockito-core` | `testImplementation` |
| mockito-kotlin | 6.4.0 | `mockito-kotlin` | `testImplementation` |
| kotlinx-coroutines-test | 1.11.0 | `kotlinx-coroutines-test` | `testImplementation` |
| Turbine | 1.2.1 | `turbine` | `testImplementation` |
| Compose UI test JUnit4 | BOM | `androidx-compose-ui-test-junit4` | `androidTestImplementation` (ya existía) |

---

## 3. Plugins agregados

| Plugin | ID | Versión | Dónde se aplica |
|---|---|---|---|
| Hilt | `com.google.dagger.hilt.android` | 2.60.1 | `app/build.gradle.kts` |
| KSP | `com.google.devtools.ksp` | 2.3.12 | `app/build.gradle.kts` |
| Kotlin serialization | `org.jetbrains.kotlin.plugin.serialization` | 2.2.10 (ref `kotlin`) | `app/build.gradle.kts` |

Los tres se declaran en la raíz con `apply false` y en `app/` sin esa opción, que es el patrón recomendado por Gradle.

Los plugins de Compose (`org.jetbrains.kotlin.plugin.compose`) y de Android (`com.android.application`) ya existían.

---

## 4. Archivos modificados y creados

### 4.1 Modificados

**`gradle/libs.versions.toml`**
- Nuevas versiones en `[versions]`: `coroutines`, `retrofit`, `okhttp`, `hilt`, `ksp`, `mockito`, `kotlinxSerialization`, `retrofitKotlinxSerialization`, `loggingInterceptor`, `room`, `datastore`, `hiltNavigationCompose`, `navigationCompose`, `coil`, `turbine`, `mockitoKotlin`.
- Nuevas librerías en `[libraries]`: 22 entradas, detalladas en la sección 2.
- Nuevos plugins en `[plugins]`: `hilt`, `ksp`, `kotlin-serialization`.

**`build.gradle.kts`** (raíz)
- Se agregaron `hilt`, `ksp` y `kotlin-serialization` con `apply false`.

**`app/build.gradle.kts`**
- Se aplican los plugins `hilt`, `ksp` y `kotlin-serialization`.
- `testInstrumentationRunner` cambió de `androidx.test.runner.AndroidJUnitRunner` a `com.example.userandordersapp.HiltTestRunner`.
- Bloque `dependencies` ampliado con las librerías de las secciones 2 y 5.

Bloque de dependencias resultante (resumen):
- `implementation`: coroutines, Retrofit, OkHttp, logging-interceptor, converter de serialización, serialization-json, Room runtime y ktx, DataStore, Lifecycle compose, Navigation compose, Hilt navigation compose, Coil, Hilt android.
- `ksp`: Room compiler, Hilt compiler.
- `testImplementation`: Mockito core, mockito-kotlin, coroutines-test, Turbine.
- `androidTestImplementation`: hilt-android-testing.
- `kspAndroidTest`: Hilt compiler.

**`app/src/main/AndroidManifest.xml`**
- Se agregó `android:name=".UserAndOrdersApplication"` en el elemento `<application>`.

### 4.2 Creados

**`app/src/main/java/com/example/userandordersapp/UserAndOrdersApplication.kt`**

```kotlin
@HiltAndroidApp
class UserAndOrdersApplication : Application()
```

Es el punto de entrada de Hilt. Genera el componente raíz de la aplicación.

**`app/src/androidTest/java/com/example/userandordersapp/HiltTestRunner.kt`**

Extiende `AndroidJUnitRunner` y reemplaza la Application por `HiltTestApplication` al crear la app de prueba. Así los tests de instrumentación pueden inyectar dependencias falsas.

**`docs/fases/fase-0-configuracion-inicial.md`**

Este documento.

---

## 5. Verificación

| Comando | Resultado |
|---|---|
| `./gradlew :app:dependencies --configuration debugRuntimeClasspath` | `BUILD SUCCESSFUL`. Versiones resueltas correctamente. |
| `./gradlew :app:compileDebugKotlin` | `BUILD SUCCESSFUL`. |
| `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin` | `BUILD SUCCESSFUL` después de bajar Coil a 3.3.0. Antes fallaba por `kotlin-stdlib` 2.4.10. |
| `./gradlew :app:assembleDebug :app:compileDebugAndroidTestKotlin` | `BUILD SUCCESSFUL`. Hilt generó `Hilt_UserAndOrdersApplication.java`. |
| `./gradlew connectedDebugAndroidTest` | **No ejecutado.** Requiere emulador o dispositivo. |

---

## 6. Problemas encontrados y cómo se resolvieron

1. **Conflicto de versiones de `kotlin-stdlib`.** Coil 3.6.3 requiere `kotlin-stdlib` 2.4.10, pero el proyecto usa Kotlin 2.2.10. Se resolvió fijando Coil en 3.3.0. Subir Kotlin a 2.4.x implicaría revisar AGP, KSP y Hilt, así que se decidió no hacerlo en esta fase.
2. **`timeout` no existe en macOS.** Se ejecutó Gradle directamente, sin el comando `timeout`.

---

## 7. Decisiones tomadas

| Decisión | Motivo |
|---|---|
| Kotlin 2.2.10 se mantiene | Es compatible con AGP 9.4.1, KSP 2.3.12 y Hilt 2.60.1. Subirlo no era necesario para esta fase. |
| Hilt y Room con KSP, no kapt | kapt está en modo de mantenimiento y KSP es más rápido. |
| Converter de serialización de Kotlin, no Gson | Es la opción nativa de Kotlin y no requiere reflexión. |
| DataStore 1.2.1, no 1.3.0-alpha | Se prefieren versiones estables. |
| Coil 3.3.0, no 3.6.3 | Compatibilidad con Kotlin 2.2.10 (ver problema 1). |

---

## 8. Pendientes para la siguiente fase

- Crear un módulo de Hilt (`@Module`) que provea Retrofit, OkHttp y la base de datos de Room.
- Crear la clase `@Database` de Room y sus DAOs.
- Ejecutar `connectedDebugAndroidTest` cuando haya un emulador disponible.
- Los cambios de esta fase **no están commiteados**. El proyecto sigue sin historial de cambios propios (los archivos están sin rastrear en git).
