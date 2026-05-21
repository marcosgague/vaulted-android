# Vaulted Android

Vaulted es una aplicacion Android orientada a usuarios de Steam que quieren reunir en un mismo sitio su biblioteca, su perfil como jugador y una capa social para descubrir personas con gustos parecidos.

El proyecto combina sincronizacion con Steam, gestion de biblioteca, personalizacion de perfil y funciones sociales como seguimiento, compatibilidad, chat y notificaciones.

## Caracteristicas principales

- Registro e inicio de sesion con Firebase Authentication
- Perfil propio con nombre, `@usuario`, avatar y configuracion personal
- Vinculacion de cuenta de Steam mediante `Steam ID`
- Sincronizacion de juegos, horas jugadas y logros desde la Steam Web API
- Biblioteca personal con ordenacion, busqueda y detalle de juego
- Seleccion de juegos favoritos y logros destacados
- Reviews personales por juego
- Busqueda de usuarios y visualizacion de perfiles publicos
- Sistema de seguimiento entre usuarios
- Calculo de compatibilidad entre perfiles
- Chat privado entre usuarios
- Notificaciones integradas dentro de la app
- Recomendaciones basadas en afinidad y favoritos compartidos

## Stack tecnologico

- **Lenguaje**: Java 11
- **IDE**: Android Studio
- **UI**: XML + Material Components
- **Autenticacion y nube**: Firebase Authentication + Firestore
- **Persistencia local**: Room 2.6.1
- **Consumo de APIs**: Retrofit 2.11.0 + Gson
- **Carga de imagenes**: Glide 4.16.0
- **Navegacion**: Navigation Component

## Arquitectura

El proyecto sigue una organizacion modular por responsabilidades:

- **Activities** para el acceso y el contenedor principal de navegacion
- **Fragments** para cada pantalla funcional de la aplicacion
- **Adapters** para listas, chats, logros, notificaciones y recomendaciones
- **Repositories** para centralizar logica de datos y sincronizacion
- **Room** para guardar en local biblioteca y logros
- **Firestore** para perfiles, reviews, favoritos, notificaciones y chat

## Estructura general del proyecto

```text
vaulted/
|-- app/
|   |-- src/main/java/com/example/vaulted/
|   |-- src/main/res/
|   |-- build.gradle.kts
|   `-- proguard-rules.pro
|-- gradle/
|-- build.gradle.kts
|-- settings.gradle.kts
`-- README.md
```

## Requisitos

- Android Studio actualizado
- JDK 11
- Android 8.0 o superior (`minSdk = 26`)
- Conexion a Internet
- Proyecto Firebase configurado
- Clave de Steam Web API valida

## Configuracion local

Este repositorio **no incluye archivos sensibles** como `google-services.json` ni claves privadas.

### 1. Firebase

Coloca tu archivo `google-services.json` en:

```text
app/google-services.json
```

### 2. Steam API Key

Anade tu clave en `local.properties` con esta propiedad:

```properties
steamApiKey=TU_CLAVE_DE_STEAM
```

> `local.properties` y `app/google-services.json` estan ignorados en Git y no se suben al repositorio.

## Como ejecutar el proyecto

1. Clona el repositorio:

```bash
git clone https://github.com/marcosgague/vaulted-android.git
```

2. Abre el proyecto en Android Studio.

3. Anade:
   - `app/google-services.json`
   - `steamApiKey=...` en `local.properties`

4. Sincroniza Gradle.

5. Ejecuta la app en un emulador o dispositivo fisico.

## Compilacion desde terminal

Para generar una build debug:

```bash
./gradlew assembleDebug
```

En Windows:

```powershell
.\gradlew.bat assembleDebug
```

## Funcionalidades por modulo

### Acceso

- `LoginActivity`
- Registro
- Inicio de sesion
- Validacion de nombre de usuario y correo

### Navegacion principal

- `MainActivity`
- Dashboard
- Biblioteca
- Busqueda
- Chat
- Notificaciones
- Perfil

### Biblioteca y Steam

- Sincronizacion de juegos
- Sincronizacion de logros
- Horas jugadas
- Ultima actividad
- Detalle individual de juego

### Perfil y social

- Perfil publico
- Seguidores y seguidos
- Favoritos
- Logros destacados
- Reviews
- Compatibilidad

### Comunicacion

- Conversaciones privadas
- Mensajes no leidos
- Notificaciones internas

## Estado del proyecto

Vaulted se encuentra en una base funcional completa a nivel academico, con especial foco en:

- integracion real con Steam
- persistencia hibrida en nube y local
- experiencia social dentro de la aplicacion
- estructura mantenible para futuras ampliaciones

## Seguridad y buenas practicas

- No se suben claves privadas al repositorio
- `google-services.json` esta excluido de Git
- La Steam API key se inyecta desde `local.properties` a `BuildConfig`
- Los datos sociales se apoyan en Firebase Authentication y Firestore

## Posibles mejoras futuras

- Mejorar el algoritmo de compatibilidad entre usuarios
- Anadir estados de actividad en tiempo real
- Incluir filtros mas avanzados en la biblioteca
- Ampliar el sistema de recomendaciones
- Incorporar estadisticas visuales del perfil

## Autor

**Marcos Garcia Guerrero**  
IES Arroyo Harnina  
2o Desarrollo de Aplicaciones Multiplataforma
