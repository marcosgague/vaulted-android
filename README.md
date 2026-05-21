# Vaulted Android

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Java](https://img.shields.io/badge/Java-11-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Room](https://img.shields.io/badge/Room-2.6.1-4285F4)](https://developer.android.com/jetpack/androidx/releases/room)
[![Retrofit](https://img.shields.io/badge/Retrofit-2.11.0-48B983)](https://square.github.io/retrofit/)

Vaulted es una aplicacion Android orientada a usuarios de Steam que quieren reunir en un mismo sitio su biblioteca, su perfil como jugador y una capa social para descubrir personas con gustos parecidos.

El proyecto combina sincronizacion con Steam, gestion de biblioteca, personalizacion de perfil y funciones sociales como seguimiento, compatibilidad, chat y notificaciones.

## Resumen

- Biblioteca sincronizada desde Steam
- Perfil publico con informacion social y configuracion de hardware
- Reviews, favoritos y logros destacados
- Compatibilidad entre usuarios y recomendaciones
- Chat privado y sistema de notificaciones
- Persistencia hibrida en Firestore y Room

## Tecnologias

| Area | Tecnologia |
|---|---|
| Lenguaje | Java 11 |
| Entorno de desarrollo | Android Studio |
| Interfaz | XML + Material Components |
| Autenticacion | Firebase Authentication |
| Base de datos en nube | Firebase Firestore |
| Base de datos local | Room 2.6.1 |
| Consumo de APIs | Retrofit 2.11.0 + Gson |
| Imagenes | Glide 4.16.0 |
| Navegacion | Navigation Component |

## Funcionalidades

### Cuenta y acceso

- Registro de usuario
- Inicio de sesion
- Validacion de `@usuario` y correo
- Edicion del perfil propio
- Cierre y eliminacion de cuenta

### Integracion con Steam

- Vinculacion mediante `Steam ID`
- Sincronizacion de juegos
- Sincronizacion de logros
- Recuperacion de avatar e informacion publica del perfil

### Biblioteca

- Visualizacion de juegos sincronizados
- Ordenacion y busqueda
- Pantalla de detalle por juego
- Reviews personales
- Favoritos

### Social

- Busqueda de usuarios
- Perfil publico
- Seguimiento entre usuarios
- Calculo de compatibilidad
- Recomendaciones basadas en afinidad

### Comunicacion

- Chat privado
- Mensajes no leidos
- Notificaciones internas

## Arquitectura

El proyecto sigue una organizacion modular por responsabilidades:

- **Activities** para el acceso y el contenedor principal
- **Fragments** para cada pantalla funcional
- **Adapters** para listas, chats, logros, notificaciones y recomendaciones
- **Repositories** para centralizar la logica de datos y sincronizacion
- **Room** para biblioteca y logros en local
- **Firestore** para perfiles, reviews, favoritos, chat y notificaciones

## Estructura del proyecto

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
- Clave valida de Steam Web API

## Configuracion local

Este repositorio **no incluye archivos sensibles** como `google-services.json` ni claves privadas.

### Firebase

Coloca tu archivo en:

```text
app/google-services.json
```

### Steam API Key

Anade tu clave en `local.properties`:

```properties
steamApiKey=TU_CLAVE_DE_STEAM
```

> `local.properties` y `app/google-services.json` estan ignorados en Git y no se suben al repositorio.

## Seguridad

- No se suben claves privadas al repositorio
- `google-services.json` esta excluido de Git
- La Steam API key se inyecta desde `local.properties` a `BuildConfig`
- Los datos sociales se apoyan en Firebase Authentication y Firestore

## Estado del proyecto

Vaulted cuenta con una base funcional completa a nivel academico, con especial foco en:

- integracion real con Steam
- persistencia hibrida en nube y local
- experiencia social dentro de la aplicacion
- estructura mantenible para futuras ampliaciones

## Roadmap

- Mejorar el algoritmo de compatibilidad
- Anadir estados de actividad en tiempo real
- Incluir filtros mas avanzados en la biblioteca
- Ampliar el sistema de recomendaciones
- Incorporar estadisticas visuales del perfil

## Autor

**Marcos Garcia Guerrero**  
IES Arroyo Harnina  
2o Desarrollo de Aplicaciones Multiplataforma
