# Fase 1: Estructura del proyecto y control de versiones

**Fecha:** 2026-10-08
**Proyecto:** UserAndOrdersApp (`com.example.userandordersapp`)
**Estado:** Completada. El proyecto compila en main, unit test y androidTest después de todos los cambios.
**Fase anterior:** [fase-0-configuracion-inicial.md](fase-0-configuracion-inicial.md)

---

## 1. Objetivo

Preparar el repositorio para el desarrollo de la app antes de escribir funcionalidad:

- Definir qué archivos deben quedar fuera del repositorio (`.gitignore`).
- Crear la estructura de carpetas por capas (`presentation`, `domain`, `data`, `di`, `util`) con archivos vacíos.
- Eliminar duplicados que se generaron al crear la estructura.

---

## 2. `.gitignore`

El proyecto no tenía `.gitignore`. Se creó en dos iteraciones.

### 2.1 Primera versión: archivos de Claude Code

```
.claude/
CLAUDE.md
CLAUDE.local.md
.mcp.json
```

Se verificó creando esos archivos temporalmente con `git check-ignore -v`. Los archivos de Claude se ignoran y las rutas normales de `docs/` y `app/` no. Después se borraron los archivos de prueba.

### 2.2 Segunda versión: archivos generados y locales

Se agregaron reglas para lo que se regenera al compilar o es específico de cada máquina:

| Categoría | Reglas | Motivo |
|---|---|---|
| Build y caché de Gradle | `.gradle/`, `build/`, `**/build/`, `.kotlin/`, `.cxx/`, `.externalNativeBuild/`, `captures/`, `*.hprof` | Se regeneran con cada compilación. |
| Binarios | `*.apk`, `*.aab`, `*.ap_`, `*.dex`, `output.json`, `/release/` | Son salidas de build. |
| Datos locales | `local.properties` | Contiene la ruta del SDK de cada máquina. |
| Firmas | `*.jks`, `*.keystore`, `keystore.properties` | Si se suben, comprometen la firma de la app. |
| IDE | `.idea/`, `*.iml`, `.navigation/` | Estado del IDE. |
| Sistema operativo | `.DS_Store`, `Thumbs.db` | Archivos del sistema. |

Verificación con `git status` y `git check-ignore`:
- `.gradle/`, `.idea/`, `.kotlin/`, `local.properties` y `.DS_Store` dejaron de aparecer como sin rastrear.
- `app/`, `docs/`, `gradle/` y `.gitignore` siguen apareciendo, como debe ser.

### 2.3 Decisión sobre `.idea/`

Se ignora la carpeta completa. Algunos equipos la commitean para compartir estilo de código o configuraciones de ejecución. Si se quiere conservar eso, la regla se cambia a rutas específicas como `.idea/workspace.xml`.

---

## 3. Estructura de carpetas

Se creó la estructura por capas que definió el usuario, con 75 archivos vacíos. Después se eliminaron 6 duplicados (ver sección 4), y quedan 69 archivos nuevos.

Base del paquete: `app/src/main/java/com/example/userandordersapp/`

| Capa | Carpetas | Archivos nuevos |
|---|---|---|
| `presentation/ui/screens` | `home/`, `details/`, `login/` (cada una con Screen, ViewModel y UiState) | 9 |
| `presentation/ui/components` | Botones, tarjetas, campos de texto, diálogo de error, indicador de carga, estado vacío | 6 |
| `presentation/navigation` | `NavGraph`, `Screen`, `NavigationHost` | 3 |
| `domain/models` | `User`, `Product`, `Order` | 3 |
| `domain/repositories` | Interfaces de `User`, `Product` y `Order` | 3 |
| `domain/usecases` | `user/` (4), `product/` (2), `order/` (2) | 8 |
| `data/repositories` | Implementaciones de `User`, `Product` y `Order` | 3 |
| `data/datasources/local` | `UserLocalDataSource`, `ProductLocalDataSource` y `dao/` (`UserDao`, `ProductDao`, `OrderDao`) | 5 |
| `data/datasources/remote` | `UserRemoteDataSource`, `ProductRemoteDataSource` y `api/` (`UserApiService`, `ProductApiService`, `ApiClient`) | 5 |
| `data/models` | DTOs de `User`, `Product` y `Order` | 3 |
| `data/mappers` | Mappers de `User`, `Product` y `Order` | 3 |
| `data/database` | `AppDatabase` y `entities/` (`UserEntity`, `ProductEntity`, `OrderEntity`) | 4 |
| `di` | `AppModule`, `RepositoryModule`, `UseCaseModule`, `NetworkModule`, `DatabaseModule`, `DataSourceModule` | 6 |
| `util` | `Constants`, `Extensions`, `Result` | 3 |

Tests:

| Source set | Archivo |
|---|---|
| `src/test` | `domain/usecases/GetUsersUseCaseTest.kt` |
| `src/test` | `domain/usecases/CreateUserUseCaseTest.kt` |
| `src/test` | `data/repositories/UserRepositoryImplTest.kt` |
| `src/test` | `presentation/viewmodels/HomeViewModelTest.kt` |
| `src/androidTest` | `ui/screens/HomeScreenTest.kt` |

Todos los archivos están vacíos (0 bytes) y sin declaración de `package` ni clase.

### 3.1 Adaptaciones a la plantilla

- **Paquete:** la plantilla usaba `com.example.app`. Se usó `com.example.userandordersapp`, el paquete del proyecto.
- **Archivos existentes:** no se sobrescribió ni vació ningún archivo que ya existía. Esto incluye `MainActivity.kt`, `AndroidManifest.xml`, `UserAndOrdersApplication.kt`, `HiltTestRunner.kt`, `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt` y los del tema.

---

## 4. Duplicados eliminados

La estructura generó dos duplicados con archivos ya existentes. Se eliminaron los que no cumplían el estándar de Android:

| Eliminado | Motivo |
|---|---|
| `App.kt` | Estaba vacío y la clase de Application real es `UserAndOrdersApplication.kt`, que es la que registra el manifest. |
| `presentation/ui/theme/` (5 archivos: `Color`, `Theme`, `Typography`, `Shapes`, `Spacing`) | Estaban vacíos y duplicaban el tema real. Se conservó `ui/theme/`, que es la ubicación estándar de Android Studio y la que usa `MainActivity` (`UserAndOrdersAppTheme`). |

Antes de borrar se comprobó que todos los archivos tuvieran 0 bytes y que ningún otro archivo los importara.

---

## 5. Verificación

| Comando | Resultado |
|---|---|
| `git check-ignore` sobre archivos de Claude | Ignorados. |
| `git check-ignore` sobre `docs/`, `app/` y `gradle/` | No ignorados. |
| `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin` (después de crear la estructura) | `BUILD SUCCESSFUL`. |
| Mismo comando después de borrar duplicados | `BUILD SUCCESSFUL`. |

Un intento de borrado falló por un error de script (un `&&` después de un `for` que devolvía un código de error). Se verificó que no se borrara nada, y se repitió con una condición correcta.

---

## 6. Decisiones tomadas

| Decisión | Motivo |
|---|---|
| Se mantiene `ui/theme/` y no `presentation/ui/theme/` | Es la ubicación estándar de Android Studio y ya la usaba `MainActivity`. |
| Archivos vacíos sin `package` ni clase | Así lo pidió el usuario. Android Studio marca error hasta que se escribe el contenido. |
| `.idea/` ignorado completo | El estado del IDE es específico de cada máquina. |

---

## 7. Pendientes

- **Archivos del tema:** `Typography.kt`, `Shapes.kt` y `Spacing.kt` ya no existen. El tema actual tiene `Type.kt`, que cumple el rol de `Typography.kt`. Falta decidir si se crean `Shapes.kt` y `Spacing.kt` en `ui/theme/`, y si `Type.kt` se renombra a `Typography.kt`.
- **Archivos sin contenido:** los 69 archivos nuevos están vacíos. Falta escribir su contenido, empezando por las capas de datos y dominio.
- **Archivos de ejemplo de la plantilla:** `ExampleUnitTest.kt` y `ExampleInstrumentedTest.kt` siguen en el proyecto. Se pueden borrar cuando haya tests reales.
- **Commit:** los cambios de esta fase no están commiteados. Los archivos del proyecto siguen sin rastrear en git.
- **Siguiente fase:** módulo de Hilt con Retrofit, OkHttp y la base de datos de Room, usando los archivos de `di/` y `data/`.
