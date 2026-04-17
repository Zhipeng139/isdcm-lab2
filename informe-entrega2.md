# Informe de prácticas de ISDCM — Entrega 2

**Asignatura:** ISDCM  
**Proyecto:** Servicio web REST para gestión de vídeos y usuarios  
**Fecha:** 2026-04-20
**Estudiante:** Zhipeng Lin (zhipeng.lin@estudiantat.upc.edu)  
**Estudiante:** Zhiwei Lin (zhiwei.lin1@estudiantat.upc.edu)

---

## Tabla de contenidos

1. [Introducción](#introducción)
2. [Decisiones de diseño](#decisiones-de-diseño)
3. [Ejemplos de uso de la API](#ejemplos-de-uso-de-la-api)
4. [Cumplimiento de la rúbrica](#cumplimiento-de-la-rúbrica)
5. [Conclusiones](#conclusiones)

---

## Introducción

Este informe describe la arquitectura y las decisiones de diseño de la Entrega 2 de la práctica ISDCM: un servicio web RESTful para la gestión de vídeos y usuarios.

El servicio expone una API HTTP que permite:

- Registrar y autenticar usuarios.
- Crear, consultar, actualizar, eliminar y buscar vídeos.
- Incrementar el contador de reproducciones de un vídeo.
- Proteger los endpoints de vídeo mediante autenticación por API key.

La aplicación se despliega como archivo WAR sobre GlassFish 7 y utiliza Jakarta EE 11, Apache Derby como base de datos relacional y CDI para la inyección de dependencias.

---

## Decisiones de diseño

### Arquitectura final implementada

La aplicación sigue una arquitectura por capas dentro de un único módulo WAR:

```
org.upc.student.isdcm.entrega2
├── rest/          ← Endpoints HTTP
├── repository/    ← Acceso a base de datos (JDBC)
├── model/         ← Entidades y proveedor de conexión
├── security/      ← Filtro de autenticación y anotación @Secured
└── error/         ← Manejador global de errores y DTO de error
```

**Flujo de una petición:**

```
Cliente HTTP
    → Gateway (/api)
    → ApiKeyFilter (si el recurso está anotado con @Secured)
    → VideoResource / UsuarioResource
    → VideoRepository / UsuarioRepository
    → Apache Derby
```

### Tecnologías utilizadas

| Elemento | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Especificación | Jakarta EE 11 |
| API REST | JAX-RS (Jersey en GlassFish 7) |
| Serialización JSON | Jakarta JSON-B (automática) |
| Inyección de dependencias | CDI (`@ApplicationScoped`, `@Inject`) |
| Base de datos | Apache Derby (modo red, puerto 1527) |
| Acceso a datos | JDBC con `PreparedStatement` |
| Construcción | Gradle |
| Servidor | GlassFish 7 |

### Recursos y endpoints REST

La aplicación publica todos sus endpoints bajo el prefijo `/api` definido en `Gateway.java`:

```java
@ApplicationPath("/api")
public class Gateway extends Application {}
```

#### Recurso de vídeos — `VideoResource` (`/api/videos`)

Todos los endpoints de vídeo están protegidos con `@Secured`.

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/videos` | Listar todos los vídeos | 200 + array JSON |
| GET | `/api/videos/{id}` | Obtener vídeo por ID | 200 / 404 |
| POST | `/api/videos` | Crear nuevo vídeo | 201 / 409 |
| PUT | `/api/videos/{id}` | Actualizar vídeo | 200 / 404 |
| DELETE | `/api/videos/{id}` | Eliminar vídeo | 204 / 404 |
| POST | `/api/videos/{id}/view` | Incrementar reproducciones | 200 / 404 |
| GET | `/api/videos/search` | Buscar por título, autor o fecha | 200 / 400 |

El endpoint de búsqueda acepta los parámetros opcionales `titulo`, `autor`, `year`, `month` y `day`. Se aplica uno de los tres criterios de forma excluyente: título > autor > fecha.

#### Recurso de usuarios — `UsuarioResource` (`/api/usuaris`)

Los endpoints de usuario son públicos (no requieren API key).

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| POST | `/api/usuaris` | Registrar nuevo usuario | 201 + `{username, apiKey}` / 409 |
| POST | `/api/usuaris/login` | Iniciar sesión | 200 + `{username, apiKey}` / 401 |

### Modelo de datos

**Tabla `videos`:**

| Columna | Tipo | Restricción |
|---|---|---|
| id | VARCHAR(80) | PRIMARY KEY |
| titulo | VARCHAR(150) | NOT NULL |
| autor | VARCHAR(100) | NOT NULL |
| fecha_creacion | DATE | NOT NULL |
| duracion | INT | NOT NULL |
| reproducciones | INT | NOT NULL |
| descripcion | VARCHAR(800) | NOT NULL |
| formato | VARCHAR(40) | NOT NULL |
| url | VARCHAR(400) | NOT NULL |
| categoria | VARCHAR(120) | NOT NULL |
| resolucion | VARCHAR(40) | NOT NULL |

**Tabla `usuaris`:**

| Columna | Tipo | Restricción |
|---|---|---|
| id | INT IDENTITY | PRIMARY KEY |
| nombre | VARCHAR(80) | NOT NULL |
| apellido | VARCHAR(80) | NOT NULL |
| email | VARCHAR(120) | UNIQUE NOT NULL |
| username | VARCHAR(50) | UNIQUE NOT NULL |
| password_hash | VARCHAR(64) | NOT NULL |
| api_key | VARCHAR(36) | UNIQUE NOT NULL |

Las tablas se crean automáticamente en el arranque de la aplicación mediante `@PostConstruct` en cada repositorio. 

### Autenticación por API key (elemento extra)

Se implementó un sistema de autenticación mediante API key propio, sin depender de mecanismos de seguridad externos.

**Diseño:**

- Al registrarse, cada usuario recibe un API key UUID único generado en el servidor y almacenado en base de datos.
- El mismo API key se devuelve también en el login.
- Todos los endpoints de vídeo exigen que la petición incluya la cabecera `X-API-Key` con un valor válido.

**Implementación con `@NameBinding`:**

Se utilizó el patrón JAX-RS `@NameBinding` para asociar el filtro únicamente a los recursos anotados con `@Secured`, sin afectar a los endpoints de usuario:

```java
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Secured {}
```

El filtro `ApiKeyFilter` intercepta todas las peticiones a recursos `@Secured` y consulta la base de datos para validar el API key recibido:

```java
@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {
    @Inject
    private UsuarioRepository repo;

    public void filter(ContainerRequestContext ctx) {
        String apiKey = ctx.getHeaderString("X-API-Key");
        if (apiKey == null || apiKey.isBlank() || !repo.validateApiKey(apiKey)) {
            ctx.abortWith(Response.status(UNAUTHORIZED)
                .entity(new ErrorResponse(401, "API key invàlida o absent"))
                .build());
        }
    }
}
```

**Ventajas de este diseño:**
- Limpio: la anotación `@Secured` en la clase `VideoResource` es suficiente para proteger todos sus métodos.
- Extensible: se puede aplicar selectivamente a métodos individuales.
- Sin acoplamiento: `UsuarioResource` no tiene `@Secured` y permanece público.

### Gestión de contraseñas

Las contraseñas se almacenan como hash SHA-256 en hexadecimal. Nunca se persiste la contraseña en claro:

```java
MessageDigest md = MessageDigest.getInstance("SHA-256");
byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
for (byte b : bytes) sb.append(String.format("%02x", b));
return sb.toString();
```

### Control de errores

Se implementó un manejador global `GlobalExceptionHandler` con `@Provider` que convierte excepciones no controladas en respuestas JSON con estructura uniforme:

```json
{ "status": 500, "message": "descripción del error" }
```

Las excepciones `WebApplicationException` (lanzadas por JAX-RS o por los recursos explícitamente) se propagan directamente sin ser envueltas, preservando su código HTTP original.

### Acceso a base de datos

`DatabaseProvider` centraliza la obtención de conexiones JDBC. Intenta primero conexión en modo red (`jdbc:derby://localhost:1527/isdcm2;create=true`) y en caso de fallo utiliza el modo embebido como alternativa. Todas las consultas usan `PreparedStatement` para evitar inyección SQL.

Los repositorios están anotados con `@ApplicationScoped` y se inicializan vía `@PostConstruct`, lo que garantiza que las tablas existen antes de la primera petición.

### Relación con el frontend (Entrega 1 ampliada)

El frontend JSP consume esta API mediante `HttpURLConnection`. Tras el login, el API key se almacena en la sesión HTTP del usuario y se envía como cabecera `X-API-Key` en todas las llamadas a endpoints de vídeo. La URL base del servicio REST es configurable en el archivo `config.properties` del proyecto frontend, sin necesidad de modificar el código.

---

## Ejemplos de uso de la API

Todos los ejemplos asumen que el servidor está disponible en `http://localhost:8080/entrega-2-1.0-SNAPSHOT`. Los endpoints de vídeo requieren la cabecera `X-API-Key` con el valor obtenido en el registro o login del usuario.

---

### Usuarios `POST /api/usuaris` Registrar usuario

**Motivo del método:** Se usa `POST` porque se está creando un nuevo recurso (usuario) en el servidor. La operación no es idempotente: llamarla dos veces con los mismos datos produce un error 409 en la segunda.

**Request:**
```http
POST /api/usuaris HTTP/1.1
Content-Type: application/json

{
  "nombre": "Zhiwei",
  "apellido": "Lin",
  "email": "zhiwei.lin1@estudiantat.upc.edu",
  "username": "zhiwei",
  "password": "secreta123"
}
```

**Response 201 — Registro correcto:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "username": "zhiwei",
  "apiKey": "a3f7c2d1-84e0-4b6a-9c31-2e5d8f0b1a7c"
}
```

**Response 409 — Usuario o email ya existe:**
```http
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "status": 409,
  "message": "Username already taken"
}
```

---

### Usuarios `POST /api/usuaris/login` Iniciar sesión

**Motivo del método:** Se usa `POST` porque las credenciales viajan en el cuerpo de la petición, nunca en la URL (donde quedarían expuestas en logs y caché). Además, el login tiene un efecto de lado (recuperar el API key del usuario) que lo hace semánticamente no seguro como `GET`.

**Request:**
```http
POST /api/usuaris/login HTTP/1.1
Content-Type: application/json

{
  "username": "zhiwei",
  "password": "secreta123"
}
```

**Response 200 — Login correcto:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "username": "zhiwei",
  "apiKey": "a3f7c2d1-84e0-4b6a-9c31-2e5d8f0b1a7c"
}
```

**Response 401 — Credenciales incorrectas:**
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "status": 401,
  "message": "Credencials incorrectes"
}
```

---

### Vídeos `GET /api/videos` Listar todos los vídeos

**Motivo del método:** Se usa `GET` porque es una operación de solo lectura, segura e idempotente. No modifica ningún estado en el servidor. Los parámetros de paginación o filtrado, si los hubiera, irían como query params en la URL.

**Request:**
```http
GET /api/videos HTTP/1.1
X-API-Key: a3f7c2d1-84e0-4b6a-9c31-2e5d8f0b1a7c
```

**Response 200:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "titulo": "Introducción a JAX-RS",
    "autor": "Zhiwei Lin",
    "fechaCreacion": "2026-03-15",
    "duracion": 320,
    "reproducciones": 12,
    "descripcion": "Tutorial básico sobre JAX-RS y REST.",
    "formato": "MP4",
    "url": "https://example.com/videos/jaxrs-intro.mp4",
    "categoria": "Programación",
    "resolucion": "1080p"
  }
]
```

**Response 401 — API key ausente o inválida:**
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "status": 401,
  "message": "API key invàlida o absent"
}
```

---

### Vídeos `GET /api/videos/{id}` Obtener vídeo por ID

**Motivo del método:** Se usa `GET` porque se lee un recurso concreto identificado por su ID en la URL. Es seguro e idempotente: la misma petición siempre devuelve el mismo resultado sin modificar datos.

**Request:**
```http
GET /api/videos/550e8400-e29b-41d4-a716-446655440000 HTTP/1.1
X-API-Key: a3f7c2d1-84e0-4b6a-9c31-2e5d8f0b1a7c
```

**Response 200:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "titulo": "Introducción a JAX-RS",
  "autor": "Zhiwei Lin",
  "fechaCreacion": "2026-03-15",
  "duracion": 320,
  "reproducciones": 12,
  "descripcion": "Tutorial básico sobre JAX-RS y REST.",
  "formato": "MP4",
  "url": "https://example.com/videos/jaxrs-intro.mp4",
  "categoria": "Programación",
  "resolucion": "1080p"
}
```

**Response 404 — Vídeo no encontrado:**
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "status": 404,
  "message": "Video not found: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

### Vídeos `POST /api/videos` Crear nuevo vídeo

**Motivo del método:** Se usa `POST` porque se crea un nuevo recurso cuyo ID lo genera el cliente (UUID) antes de enviarlo. Si bien `PUT` también podría usarse al conocer el ID de antemano, `POST` es la convención habitual para la creación y señala que la operación no es idempotente: un segundo envío con el mismo ID producirá un 409.

**Request:**
```http
POST /api/videos HTTP/1.1
Content-Type: application/json
X-API-Key: a3f7c2d1-84e0-4b6a-9c31-2e5d8f0b1a7c

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "titulo": "Introducción a JAX-RS",
  "autor": "Zhiwei Lin",
  "fechaCreacion": "2026-03-15",
  "duracion": 320,
  "reproducciones": 0,
  "descripcion": "Tutorial básico sobre JAX-RS y REST.",
  "formato": "MP4",
  "url": "https://example.com/videos/jaxrs-intro.mp4",
  "categoria": "Programación",
  "resolucion": "1080p"
}
```

**Response 201 — Vídeo creado:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "titulo": "Introducción a JAX-RS",
  ...
}
```

**Response 409 — ID ya existe:**
```http
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "status": 409,
  "message": "Video with this id already exists"
}
```

---

### Vídeos `PUT /api/videos/{id}` Actualizar vídeo

**Motivo del método:** Se usa `PUT` porque se reemplaza completamente el estado de un recurso ya existente identificado por su ID en la URL. Es idempotente: aplicar la misma petición varias veces produce el mismo resultado final.

**Request:**
```http
PUT /api/videos/550e8400-e29b-41d4-a716-446655440000 HTTP/1.1
Content-Type: application/json
X-API-Key: a3f7c2d1-84e0-4b6a-9c31-2e5d8f0b1a7c

{
  "titulo": "Introducción a JAX-RS (actualizado)",
  "autor": "Zhiwei Lin",
  "fechaCreacion": "2026-03-15",
  "duracion": 400,
  "reproducciones": 12,
  "descripcion": "Versión extendida del tutorial.",
  "formato": "MP4",
  "url": "https://example.com/videos/jaxrs-intro-v2.mp4",
  "categoria": "Programación",
  "resolucion": "1080p"
}
```

**Response 200 — Actualización correcta:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "titulo": "Introducción a JAX-RS (actualizado)",
  "duracion": 400,
  ...
}
```

**Response 404 — Vídeo no encontrado:**
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "status": 404,
  "message": "Video not found: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

### Vídeos `DELETE /api/videos/{id}` Eliminar vídeo

**Motivo del método:** Se usa `DELETE` porque es la operación semántica HTTP para eliminar un recurso. Es idempotente: si el recurso ya no existe, el resultado observable (que no existe) es el mismo.

**Request:**
```http
DELETE /api/videos/550e8400-e29b-41d4-a716-446655440000 HTTP/1.1
X-API-Key: a3f7c2d1-84e0-4b6a-9c31-2e5d8f0b1a7c
```

**Response 204 — Eliminado correctamente:**
```http
HTTP/1.1 204 No Content
```

**Response 404 — Vídeo no encontrado:**
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "status": 404,
  "message": "Video not found: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

### Vídeos `POST /api/videos/{id}/view` Incrementar reproducciones

**Motivo del método:** Se usa `POST` porque esta operación produce un efecto de lado no idempotente: cada llamada incrementa en 1 el contador de reproducciones. No sería correcto usar `PUT` (que implica idempotencia) ni `GET` (que no debe modificar estado). La sub-ruta `/view` representa una acción sobre el recurso, un patrón habitual en REST cuando no encaja un CRUD puro.

**Request:**
```http
POST /api/videos/550e8400-e29b-41d4-a716-446655440000/view HTTP/1.1
X-API-Key: a3f7c2d1-84e0-4b6a-9c31-2e5d8f0b1a7c
```

**Response 200 — Contador actualizado:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "titulo": "Introducción a JAX-RS",
  "reproducciones": 13,
  ...
}
```

**Response 404 — Vídeo no encontrado:**
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "status": 404,
  "message": "Video no encontrado: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

### Vídeos `GET /api/videos/search` Buscar vídeos

**Motivo del método:** Se usa `GET` porque la búsqueda es una operación de lectura pura. Los criterios de búsqueda van como query parameters en la URL, lo cual es la convención REST estándar para filtros. Es seguro e idempotente: la misma consulta siempre devuelve el mismo conjunto de resultados sin modificar datos.

Los parámetros disponibles son mutuamente excluyentes por prioridad: `titulo` > `autor` > `year` (con `month` y `day` opcionales).

**Request — búsqueda por título:**
```http
GET /api/videos/search?titulo=JAX-RS HTTP/1.1
X-API-Key: a3f7c2d1-84e0-4b6a-9c31-2e5d8f0b1a7c
```

**Request — búsqueda por autor:**
```http
GET /api/videos/search?autor=Zhiwei HTTP/1.1
X-API-Key: a3f7c2d1-84e0-4b6a-9c31-2e5d8f0b1a7c
```

**Request — búsqueda por año y mes:**
```http
GET /api/videos/search?year=2026&month=3 HTTP/1.1
X-API-Key: a3f7c2d1-84e0-4b6a-9c31-2e5d8f0b1a7c
```

**Response 200:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "titulo": "Introducción a JAX-RS",
    "autor": "Zhiwei Lin",
    "fechaCreacion": "2026-03-15",
    ...
  }
]
```

**Response 400 — Ningún parámetro de búsqueda proporcionado:**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "status": 400,
  "message": "Cal indicar titulo, autor o year"
}
```

---

## Cumplimiento de la rúbrica

| Criterio | Nivel | Justificación |
|---|---|---|
| **Informe** | Correcto (1,5 pts) | El documento incluye portada con datos de los estudiantes, índice numerado, secciones estructuradas (introducción, diseño, ejemplos de API, rúbrica, conclusiones y bibliografía) y está redactado sin faltas ortográficas. |
| **Uso de MVC** | Correcto (1,5 pts) | La arquitectura separa claramente las capas: `rest/` actúa como controlador (recibe peticiones HTTP y produce respuestas JSON), `repository/` gestiona el acceso a datos con JDBC, y `model/` contiene las entidades. La lógica de negocio no se mezcla con la persistencia ni con la capa de presentación. |
| **REST PUT** | Correcto (2 pts) | Implementado en `PUT /api/videos/{id}`. Reemplaza completamente el recurso vídeo identificado por su ID. Devuelve 200 con el vídeo actualizado o 404 si no existe. |
| **REST POST** | Correcto (1,5 pts) | Implementado en `POST /api/videos` (crear vídeo), `POST /api/videos/{id}/view` (incrementar reproducciones), `POST /api/usuaris` (registrar usuario) y `POST /api/usuaris/login` (iniciar sesión). |
| **REST GET** | Correcto (0,5 pts) | Implementado en `GET /api/videos` (listar todos), `GET /api/videos/{id}` (obtener por ID) y `GET /api/videos/search` (buscar por título, autor o fecha con query params). |
| **Integración REST correcta** | Correcto (2 pts) | El frontend JSP consume todos los endpoints de esta API mediante `HttpURLConnection`. Tras el login, el API key se almacena en la sesión HTTP y se reenvía como cabecera `X-API-Key` en cada llamada a vídeos. La URL base del servicio es configurable en `config.properties` sin tocar el código. |
| **Player** | Correcto (2 pts) | La vista `reproduccion.jsp` integra el reproductor VideoJS 8.10. Al iniciarse la reproducción por primera vez (`player.one('play', ...)`), el frontend realiza una llamada AJAX al servlet, que a su vez invoca `POST /api/videos/{id}/view`. El contador visible en pantalla se actualiza sin recargar la página. |
| **Uso local de BD y no REST** | Correcto (1 pt) | El incremento de reproducciones se realiza íntegramente a través del servicio REST (`POST /api/videos/{id}/view → VideoRepository.incrementarReproducciones`). El frontend nunca accede directamente a la base de datos. |
| **Extras** | Autenticación por API key (+0,5 pts) | Sistema de autenticación propio mediante cabecera `X-API-Key` usando el patrón JAX-RS `@NameBinding`. Cada usuario recibe un UUID único como API key al registrarse. El filtro `ApiKeyFilter` valida la clave contra la base de datos antes de procesar cualquier petición a `VideoResource`, sin afectar a los endpoints públicos de usuario. |

---

## Conclusiones

La Entrega 2 implementa un servicio web REST completo y funcional para la gestión de vídeos y usuarios, cumpliendo todos los criterios de la rúbrica.

La decisión de usar JAX-RS con el patrón `@NameBinding` para la autenticación por API key permite proteger selectivamente los endpoints de vídeo sin modificar los de usuario, manteniendo el código desacoplado y limpio. El uso de CDI con `@ApplicationScoped` garantiza que los repositorios se inicializan una sola vez al arrancar el servidor, asegurando que las tablas están disponibles antes de la primera petición.

La integración con el frontend JSP demuestra que la API es consumible desde un cliente externo real: el contador de reproducciones se incrementa exclusivamente a través del servicio REST, respetando el requisito de no acceder directamente a la base de datos desde el frontend.

---