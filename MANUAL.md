# Plan de Desarrollo: GameArena - Sistema de Gestión de Torneos de Videojuegos

## Información del Proyecto

**Nombre:** GameArena
**Descripción:** Aplicación web para gestionar torneos de videojuegos y las inscripciones de jugadores
**Stack Tecnológico:**
- Java 21
- Spring Boot 4.0.0
- Maven
- MySQL (base de datos principal)
- H2 Database (solo tests)
- Thymeleaf
- Bootstrap 5.3.8
- Chart.js 4.4.0
- JUnit 5

---

## 1. CONFIGURACIÓN INICIAL

### 1.1 Crear proyecto Spring Boot
- **Group:** com.salesianos
- **Artifact:** gamearena
- **Packaging:** War
- **Dependencies:**
  - Spring Web
  - Spring Data JPA
  - MySQL Driver
  - H2 Database
  - Thymeleaf
  - Validation
  - Spring Boot DevTools

### 1.2 Configurar dependencias en `pom.xml`

MySQL va como `runtime` normal. H2 va como `scope=test` (solo se usa en tests):

```xml
<!-- MySQL - Base de datos principal -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- H2 - Base de datos en memoria solo para tests -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### 1.3 Configurar `application.properties`

> Antes de arrancar la app, crear la BD en MySQL: `CREATE DATABASE gamearena;`

```properties
server.port=8080

# MySQL - Base de datos principal
spring.datasource.url=jdbc:mysql://localhost:3306/gamearena?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=1234

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Thymeleaf
spring.thymeleaf.cache=false

# Logging
logging.level.org.springframework.web=DEBUG
logging.level.com.salesianos.gamearena=DEBUG
```

> `ddl-auto=update` crea las tablas si no existen y las actualiza si cambia el esquema.
> Usar `create-drop` solo en desarrollo si quieres partir limpio cada vez.

### 1.4 Configurar `application-test.properties`

Los tests usan H2 en memoria para ser rápidos, aislados y no tocar la BD de MySQL:

```properties
# Perfil de test: H2 en memoria, sin MySQL, sin DataLoader
spring.datasource.url=jdbc:h2:mem:gamearena-test;DB_CLOSE_DELAY=-1
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
```

---

## 2. ESTRUCTURA DE PAQUETES

Crear la siguiente estructura en `src/main/java/com/salesianos/gamearena/`:

```
com.salesianos.gamearena
├── config
├── controller
│   ├── web
│   └── api
├── entity
├── repository
├── service
└── GameArenaApplication.java
```

---

## 3. ENTIDADES (CAPA MODEL)

### 3.1 Entidad Jugador
**Ubicación:** `entity/Jugador.java`

**Requisitos:**
- `@Entity` con nombre de tabla `"jugadores"`
- Campos:
  - `id`: Long (PK, auto-generado con `@GeneratedValue(strategy = GenerationType.IDENTITY)`)
  - `nickname`: String (`@NotBlank`, mínimo 3 caracteres con `@Size(min=3)`)
  - `email`: String (`@Email`, opcional pero validable cuando se rellena)
  - `categoria`: String (`@NotBlank`, ej: "Junior", "Senior", "Pro")
- Relación `@OneToMany(mappedBy = "jugador", cascade = CascadeType.ALL, orphanRemoval = true)` con Inscripcion
- `@JsonManagedReference` en la lista de inscripciones para evitar ciclos en JSON
- Métodos helper para gestión bidireccional:
  - `addInscripcion(Inscripcion i)` → añade a la lista y asigna `i.setJugador(this)`
  - `removeInscripcion(Inscripcion i)` → retira de la lista y asigna `i.setJugador(null)`
- Constructor vacío + constructor con `(nickname, email, categoria)`
- Getters y setters de todos los campos

### 3.2 Entidad Inscripcion
**Ubicación:** `entity/Inscripcion.java`

**Requisitos:**
- `@Entity` con nombre de tabla `"inscripciones"`
- Campos:
  - `id`: Long (PK, auto-generado)
  - `torneo`: String (`@NotBlank`, nombre del torneo, ej: "Copa Primavera 2026")
  - `fechaInscripcion`: LocalDate (`@NotNull`)
  - `eliminado`: boolean (default: `false`, indica si el jugador fue eliminado del torneo)
  - `fechaEliminacion`: LocalDate (nullable)
  - `jugador`: Jugador (relación `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "jugador_id", nullable = false)`, `@NotNull`)
- `@JsonBackReference` en el campo `jugador` para evitar ciclos en JSON
- **Validaciones personalizadas con `@AssertTrue`:**
  - Si `eliminado = true` → `fechaEliminacion` no puede ser null
    *(nombre del método: `isEliminadoTieneFechaEliminacion` → propertyPath: `"eliminadoTieneFechaEliminacion"`)*
  - `fechaEliminacion` debe ser >= `fechaInscripcion`
    *(nombre del método: `isFechaEliminacionValida` → propertyPath: `"fechaEliminacionValida"`)*
- Constructor vacío + constructor con todos los campos
- Getters y setters de todos los campos

---

## 4. REPOSITORIOS (CAPA DATA)

### 4.1 JugadorRepository
**Ubicación:** `repository/JugadorRepository.java`

```java
@Repository
public interface JugadorRepository extends JpaRepository<Jugador, Long> {

    // Buscar jugadores por categoría (usado en el filtro de la vista)
    List<Jugador> findByCategoria(String categoria);

    // Agrupación de jugadores por categoría para KPI
    @Query("SELECT j.categoria AS categoria, COUNT(j) AS total FROM Jugador j GROUP BY j.categoria")
    List<Map<String, Object>> getJugadoresPorCategoria();

    // Top 5 jugadores con más inscripciones (para el gráfico de radar)
    @Query("SELECT j.nickname AS nickname, COUNT(i) AS totalInscripciones " +
           "FROM Jugador j LEFT JOIN j.inscripciones i " +
           "GROUP BY j.id, j.nickname " +
           "ORDER BY totalInscripciones DESC " +
           "LIMIT 5")
    List<Map<String, Object>> getTop5JugadoresConMasInscripciones();
}
```

### 4.2 InscripcionRepository
**Ubicación:** `repository/InscripcionRepository.java`

```java
@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    // Inscripciones de un jugador concreto
    List<Inscripcion> findByJugadorId(Long jugadorId);

    // Inscripciones activas (no eliminados)
    List<Inscripcion> findByEliminadoFalse();

    // Conteo de activos vs eliminados para el pie chart
    @Query("SELECT " +
           "SUM(CASE WHEN i.eliminado = true THEN 1 ELSE 0 END) AS eliminados, " +
           "SUM(CASE WHEN i.eliminado = false THEN 1 ELSE 0 END) AS activos " +
           "FROM Inscripcion i")
    List<Map<String, Object>> getInscripcionesPorEstado();

    // Inscripciones agrupadas por mes para el bar chart
    @Query("SELECT MONTH(i.fechaInscripcion) AS mes, COUNT(i) AS total " +
           "FROM Inscripcion i " +
           "GROUP BY MONTH(i.fechaInscripcion) " +
           "ORDER BY mes")
    List<Map<String, Object>> getInscripcionesPorMes();
}
```

---

## 5. SERVICIOS (CAPA BUSINESS)

### 5.1 JugadorService
**Ubicación:** `service/JugadorService.java`

- Anotar con `@Service` y `@Transactional` a nivel de clase
- Inyectar `JugadorRepository` con `@Autowired`
- Métodos:
  - `findAll()`: `List<Jugador>` — `@Transactional(readOnly = true)`
  - `findById(Long id)`: `Optional<Jugador>` — `@Transactional(readOnly = true)`
  - `save(Jugador jugador)`: `Jugador`
  - `deleteById(Long id)`: `void`
  - `getJugadoresPorCategoria()`: `List<Map<String, Object>>` — `@Transactional(readOnly = true)`
  - `getTop5JugadoresConMasInscripciones()`: `List<Map<String, Object>>` — `@Transactional(readOnly = true)`

### 5.2 InscripcionService
**Ubicación:** `service/InscripcionService.java`

- Anotar con `@Service` y `@Transactional` a nivel de clase
- Inyectar `InscripcionRepository` con `@Autowired`
- Métodos:
  - `findAll()`: `List<Inscripcion>` — `@Transactional(readOnly = true)`
  - `findById(Long id)`: `Optional<Inscripcion>` — `@Transactional(readOnly = true)`
  - `save(Inscripcion inscripcion)`: `Inscripcion`
  - `deleteById(Long id)`: `void`
  - `getInscripcionesPorEstado()`: `List<Map<String, Object>>` — `@Transactional(readOnly = true)`
  - `getInscripcionesPorMes()`: `List<Map<String, Object>>` — `@Transactional(readOnly = true)`

---

## 6. CONTROLADORES WEB (MVC)

### 6.1 HomeController
**Ubicación:** `controller/web/HomeController.java`
- `GET /` → `redirect:/jugadores`

### 6.2 JugadorController
**Ubicación:** `controller/web/JugadorController.java`

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/jugadores` | Lista todos los jugadores |
| `GET` | `/jugadores/{id}` | Detalle del jugador con sus inscripciones |
| `GET` | `/jugadores/new` | Formulario de creación |
| `POST` | `/jugadores` | Guardar nuevo jugador |
| `GET` | `/jugadores/edit/{id}` | Formulario de edición |
| `POST` | `/jugadores/edit/{id}` | Actualizar jugador |
| `GET` | `/jugadores/delete/{id}` | Eliminar jugador |

**Notas de implementación:**
- `POST /jugadores` y `POST /jugadores/edit/{id}`: usar `@Valid @ModelAttribute` + `BindingResult`
- Si hay errores → retornar la vista `"jugadores/form"` (no redirigir)
- Si éxito → `redirectAttributes.addFlashAttribute("successMessage", "...")` + `redirect:/jugadores`
- En delete → capturar `Exception` y usar `redirectAttributes.addFlashAttribute("errorMessage", "...")`

### 6.3 InscripcionController
**Ubicación:** `controller/web/InscripcionController.java`

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/inscripciones` | Lista todas las inscripciones |
| `GET` | `/inscripciones/{id}` | Detalle de una inscripción |
| `GET` | `/inscripciones/new` | Formulario de creación (incluye selector de jugador) |
| `POST` | `/inscripciones` | Guardar nueva inscripción |
| `GET` | `/inscripciones/edit/{id}` | Formulario de edición |
| `POST` | `/inscripciones/edit/{id}` | Actualizar inscripción |
| `GET` | `/inscripciones/delete/{id}` | Eliminar inscripción |

**Notas de implementación (importante):**
- El campo `jugador` en `Inscripcion` es una relación `@ManyToOne` → Spring MVC no puede vincularlo automáticamente desde un `<select>` con `th:field`
- Solución: recibir `jugadorId` como `@RequestParam(value = "jugadorId", required = false) Long jugadorId` y asignar el jugador **antes** de validar:
  ```java
  if (jugadorId != null) {
      jugadorService.findById(jugadorId).ifPresent(inscripcion::setJugador);
  }
  ```
- Luego validar manualmente con `jakarta.validation.Validator`:
  ```java
  jakarta.validation.Validator validator = jakarta.validation.Validation
          .buildDefaultValidatorFactory().getValidator();
  var violations = validator.validate(inscripcion);
  if (!violations.isEmpty()) {
      for (var v : violations) {
          String field = v.getPropertyPath().toString();
          result.rejectValue(field, "error." + field, v.getMessage());
      }
  }
  ```
- En caso de error: añadir `model.addAttribute("jugadores", jugadorService.findAll())` antes de retornar el formulario

### 6.4 KPIController
**Ubicación:** `controller/web/KPIController.java`
- `GET /kpi` → retorna la vista `"kpi/dashboard"`

---

## 7. API REST CONTROLLERS

### 7.1 JugadorRestController
**Ubicación:** `controller/api/JugadorRestController.java`
**Mapping base:** `@RequestMapping("/api/jugadores")`
**Anotar con:** `@RestController`

| Método HTTP | URL | Comportamiento |
|-------------|-----|----------------|
| `GET` | `/api/jugadores` | `List<Jugador>` — 200 OK |
| `GET` | `/api/jugadores/{id}` | `ResponseEntity<Jugador>` — 200 o 404 |
| `POST` | `/api/jugadores` | `@Valid @RequestBody` → 201 Created |
| `PUT` | `/api/jugadores/{id}` | `@Valid @RequestBody` → 200 o 404 |
| `DELETE` | `/api/jugadores/{id}` | 204 No Content o 404 |

### 7.2 InscripcionRestController
**Ubicación:** `controller/api/InscripcionRestController.java`
**Mapping base:** `@RequestMapping("/api/inscripciones")`

| Método HTTP | URL | Comportamiento |
|-------------|-----|----------------|
| `GET` | `/api/inscripciones` | `List<Inscripcion>` — 200 OK |
| `GET` | `/api/inscripciones/{id}` | `ResponseEntity<Inscripcion>` — 200 o 404 |
| `POST` | `/api/inscripciones` | 201 Created |
| `PUT` | `/api/inscripciones/{id}` | 200 o 404 |
| `DELETE` | `/api/inscripciones/{id}` | 204 No Content o 404 |

### 7.3 KPIRestController
**Ubicación:** `controller/api/KPIRestController.java`
**Mapping base:** `@RequestMapping("/api/kpi")`

| Endpoint | Servicio que llama | Descripción |
|----------|--------------------|-------------|
| `GET /api/kpi/jugadores-por-categoria` | `jugadorService.getJugadoresPorCategoria()` | Agrupa jugadores por categoría |
| `GET /api/kpi/inscripciones-por-estado` | `inscripcionService.getInscripcionesPorEstado()` | Activos vs eliminados |
| `GET /api/kpi/inscripciones-por-mes` | `inscripcionService.getInscripcionesPorMes()` | Total inscripciones por mes |
| `GET /api/kpi/top-jugadores` | `jugadorService.getTop5JugadoresConMasInscripciones()` | Top 5 jugadores |

---

## 8. VISTAS THYMELEAF

### 8.1 Layout Maestro
**Ubicación:** `templates/layout.html`

El layout se implementa con **fragments parametrizados de Thymeleaf**. Así, cada página hija solo define su contenido y el layout pone todo lo común (head, navbar, scripts).

**Patrón del layout (`layout.html`):**
```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org"
      th:fragment="layout(content, pageTitle, scripts)">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <title th:insert="${pageTitle}">GameArena</title>
</head>
<body class="bg-light">
    <nav th:replace="~{fragments/navbar :: navbar}"></nav>
    <div th:insert="${content}"></div>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js"></script>
    <th:block th:insert="${scripts}"></th:block>
</body>
</html>
```

> **Clave:** `th:fragment="layout(...)"` va en el elemento `<html>` raíz, no en un `<th:block>` dentro del body.

**Patrón de cada página hija** (sin scripts extra):
```html
<html th:replace="~{layout :: layout(~{::content}, ~{::title}, _)}"
      xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Jugadores - GameArena</title>
</head>
<body>
<th:block th:fragment="content">
    <!-- solo el contenido específico de esta página -->
</th:block>
</body>
</html>
```

**Patrón de páginas con scripts** (ej: dashboard, formularios con JS):
```html
<html th:replace="~{layout :: layout(~{::content}, ~{::title}, ~{::scripts})}"
      xmlns:th="http://www.thymeleaf.org">
...
<th:block th:fragment="scripts">
<script>
    // JS específico de esta página
</script>
</th:block>
</html>
```

- `_` (guión bajo) = pasar nulo, para páginas que no necesitan scripts extra
- `th:insert` en el layout en vez de `th:replace`, para que el div/block contenedor permanezca

### 8.2 Navbar Fragment
**Ubicación:** `templates/fragments/navbar.html`

```html
<nav th:fragment="navbar" class="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
    <div class="container">
        <a class="navbar-brand fw-bold" th:href="@{/}">🎮 GameArena</a>
        <button class="navbar-toggler" type="button"
                data-bs-toggle="collapse" data-bs-target="#navMenu">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navMenu">
            <ul class="navbar-nav ms-auto">
                <li class="nav-item">
                    <a class="nav-link" th:href="@{/jugadores}">Jugadores</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" th:href="@{/inscripciones}">Inscripciones</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" th:href="@{/kpi}">KPI</a>
                </li>
            </ul>
        </div>
    </div>
</nav>
```

> **Nota:** En Thymeleaf 3.1+ / Spring Boot 4, `#httpServletRequest` ya no está disponible como variable de contexto. Para marcar el enlace activo usar JavaScript del lado cliente en el layout, o `#request.requestURI` si se necesita server-side.

### 8.3 Vistas de Jugadores
**Ubicación:** `templates/jugadores/`

**`list.html`:**
- Tabla con columnas: ID, Nickname, Email, Categoría, Nº Inscripciones, Acciones
- Botones: 👁 Ver detalle (info), ✏️ Editar (warning), 🗑 Eliminar (danger) con `confirm()`
- Alertas flash: `${successMessage}` y `${errorMessage}` con Bootstrap dismissible
- Badge de color para la categoría (`bg-success` Junior, `bg-warning` Senior, `bg-danger` Pro)
- Mensaje "No hay jugadores registrados" con `th:if="${#lists.isEmpty(jugadores)}"`

**`form.html`:**
- Formulario único para crear y editar (`jugador.id != null` → editar)
- `th:action` dinámico: `/jugadores` (crear) vs `/jugadores/edit/{id}` (editar)
- `th:object="${jugador}"` para el binding
- Campos con `th:field="*{campo}"` y `th:classappend="${#fields.hasErrors('campo')} ? 'is-invalid'"`
- Mensajes de error con `th:errors="*{campo}"`
- Campo `categoria`: selector visual con botones (Junior / Senior / Pro / Otro) + `<input type="hidden">`
- Botón "Guardar" (submit) y "Cancelar" (link a `/jugadores`)

**`detail.html`:**
- Ficha del jugador con todos sus datos
- Tabla de inscripciones con sus estados (badge verde = activo, badge rojo = eliminado)
- Botón "Volver al listado"

### 8.4 Vistas de Inscripciones
**Ubicación:** `templates/inscripciones/`

**`list.html`:**
- Tabla con columnas: ID, Torneo, Jugador, Fecha Inscripción, Estado, Fecha Eliminación, Acciones
- Badge de estado: `bg-success "Activo"` si `eliminado=false`, `bg-danger "Eliminado"` si `eliminado=true`
- Operador Elvis: `th:text="${inscripcion.fechaEliminacion ?: '-'}"`

**`form.html`:**
- Selector `<select>` para elegir jugador:
  ```html
  <select name="jugadorId" class="form-select">
      <option value="">-- Seleccionar jugador --</option>
      <option th:each="j : ${jugadores}"
              th:value="${j.id}"
              th:text="${j.nickname} + ' (' + ${j.categoria} + ')'"
              th:selected="${inscripcion.jugador != null and inscripcion.jugador.id == j.id}">
      </option>
  </select>
  ```
- Campos de fecha: `<input type="date" th:field="*{fechaInscripcion}">`
- Checkbox para `eliminado`: `<input type="checkbox" th:field="*{eliminado}">`
- Campo `fechaEliminacion` que aparece/se oculta con JS según el estado del checkbox

**`detail.html`:**
- Detalle completo de la inscripción con enlace al jugador

### 8.5 Vista KPI Dashboard
**Ubicación:** `templates/kpi/dashboard.html`

**4 gráficos con Chart.js (carga de datos vía AJAX `fetch`):**

| Gráfico | Tipo Chart.js | Endpoint API | Campos esperados |
|---------|--------------|--------------|-----------------|
| Jugadores por categoría | `bar` | `/api/kpi/jugadores-por-categoria` | `{ categoria, total }` |
| Inscripciones por estado | `pie` | `/api/kpi/inscripciones-por-estado` | `{ activos, eliminados }` |
| Inscripciones por mes | `bar` | `/api/kpi/inscripciones-por-mes` | `{ mes, total }` |
| Top 5 jugadores | `radar` | `/api/kpi/top-jugadores` | `{ nickname, totalInscripciones }` |

**Patrón JS para cada gráfico:**
```javascript
async function fetchJson(url) {
    const res = await fetch(url);
    if (!res.ok) throw new Error('Fetch error ' + res.status);
    return res.json();
}

async function renderJugadoresCat() {
    const data = await fetchJson('/api/kpi/jugadores-por-categoria');
    const labels = data.map(d => d.categoria);
    const values = data.map(d => +d.total);
    new Chart(document.getElementById('chartJugadoresCat'), {
        type: 'bar',
        data: { labels, datasets: [{ label: 'Jugadores', data: values, backgroundColor: '#198754' }] },
        options: { responsive: true, scales: { y: { beginAtZero: true } } }
    });
}

// Inicializar todos los gráficos en paralelo
(async function initCharts() {
    try {
        await Promise.all([
            renderJugadoresCat(),
            renderInscripcionesEstado(),
            renderInscripcionesMes(),
            renderTopJugadores()
        ]);
    } catch (e) {
        console.error('KPI charts error:', e);
    }
})();
```

---

## 9. TESTS

### 9.1 Tests Unitarios (2)
**Ubicación:** `src/test/java/com/salesianos/gamearena/entity/InscripcionTest.java`

**Tipo:** Test unitario puro, sin Spring, sin BD. Usa el `Validator` de Jakarta directamente.

```java
class InscripcionTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        // Se crea una sola vez para todos los tests (costoso por reflexión)
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("UT1 - eliminado=true exige fechaEliminacion")
    void UT1_eliminadoTrueExigeFechaEliminacion() {
        Jugador jugador = new Jugador("ProGamer99", null, "Pro");
        // INVÁLIDO: eliminado=true pero fechaEliminacion=null
        Inscripcion ins = new Inscripcion("Copa Primavera", LocalDate.of(2026, 1, 10), true, null, jugador);

        Set<ConstraintViolation<Inscripcion>> violations = validator.validate(ins);

        boolean hasViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("eliminadoTieneFechaEliminacion"));

        assertTrue(hasViolation, "Debe haber violación cuando eliminado=true y fechaEliminacion=null");
    }

    @Test
    @DisplayName("UT2 - fechaEliminacion debe ser >= fechaInscripcion")
    void UT2_fechaEliminacionDebeSerPosteriorOIgualFechaInscripcion() {
        Jugador jugador = new Jugador("SpeedRunner", null, "Junior");
        // INVÁLIDO: eliminado en fecha anterior a la inscripción
        Inscripcion ins = new Inscripcion(
                "Copa Verano",
                LocalDate.of(2026, 6, 15),
                true,
                LocalDate.of(2026, 6, 10), // ANTERIOR a la inscripción
                jugador
        );

        Set<ConstraintViolation<Inscripcion>> violations = validator.validate(ins);

        boolean hasViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("fechaEliminacionValida"));

        assertTrue(hasViolation, "Debe haber violación cuando fechaEliminacion es anterior a fechaInscripcion");
    }
}
```

### 9.2 Tests de Integración (2)

#### IT1 — JugadorRepositoryTest
**Ubicación:** `src/test/java/com/salesianos/gamearena/repository/JugadorRepositoryTest.java`

**Tipo:** Test de integración con BD H2 real. Usa `@DataJpaTest` que levanta solo la capa JPA.

```java
@DataJpaTest                // Levanta solo JPA: entidades, repos, H2. Sin controladores ni servicios.
@ActiveProfiles("test")     // Usa application-test.properties (BD aislada)
class JugadorRepositoryTest {

    @Autowired
    private JugadorRepository jugadorRepository;

    @Test
    @DisplayName("IT1 - Repositorio Jugadores y agrupación por categoría")
    void IT1_repositorioJugadoresYAgrupacionPorCategoria() {

        // ARRANGE: 2 Junior, 1 Pro
        jugadorRepository.save(new Jugador("PlayerOne",   null, "Junior"));
        jugadorRepository.save(new Jugador("PlayerTwo",   null, "Junior"));
        jugadorRepository.save(new Jugador("PlayerThree", null, "Pro"));

        // ACT
        List<Map<String, Object>> result = jugadorRepository.getJugadoresPorCategoria();

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size(), "Deben existir 2 categorías distintas");

        boolean foundJunior = result.stream()
                .anyMatch(m -> "Junior".equals(m.get("categoria")) && ((Number) m.get("total")).intValue() == 2);
        boolean foundPro = result.stream()
                .anyMatch(m -> "Pro".equals(m.get("categoria")) && ((Number) m.get("total")).intValue() == 1);

        assertTrue(foundJunior, "Debe haber 2 jugadores en Junior");
        assertTrue(foundPro,    "Debe haber 1 jugador en Pro");
    }
}
```

#### IT2 — InscripcionRepositoryTest
**Ubicación:** `src/test/java/com/salesianos/gamearena/repository/InscripcionRepositoryTest.java`

```java
@DataJpaTest
@ActiveProfiles("test")
class InscripcionRepositoryTest {

    @Autowired
    private JugadorRepository jugadorRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Test
    @DisplayName("IT2 - Conteo inscripciones activas y eliminadas")
    void IT2_conteoActivosEliminados() {

        // ARRANGE
        Jugador j = jugadorRepository.save(new Jugador("TesterIT2", null, "Senior"));

        // 2 activos
        inscripcionRepository.save(new Inscripcion("Copa 1", LocalDate.of(2026, 1, 1), false, null, j));
        inscripcionRepository.save(new Inscripcion("Copa 2", LocalDate.of(2026, 2, 1), false, null, j));
        // 3 eliminados (con fecha de eliminación válida)
        inscripcionRepository.save(new Inscripcion("Copa 3", LocalDate.of(2026, 3, 1), true, LocalDate.of(2026, 3, 10), j));
        inscripcionRepository.save(new Inscripcion("Copa 4", LocalDate.of(2026, 4, 1), true, LocalDate.of(2026, 4, 8),  j));
        inscripcionRepository.save(new Inscripcion("Copa 5", LocalDate.of(2026, 5, 1), true, LocalDate.of(2026, 5, 5),  j));

        // ACT
        List<Map<String, Object>> result = inscripcionRepository.getInscripcionesPorEstado();

        // ASSERT
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // La query devuelve UN único elemento (agregado global)
        Map<String, Object> stats = result.get(0);
        // Usar ((Number)...).intValue() por compatibilidad (H2 puede devolver Integer, Long o BigInteger)
        int activos    = ((Number) stats.get("activos")).intValue();
        int eliminados = ((Number) stats.get("eliminados")).intValue();

        assertEquals(2, activos,    "Deben haber 2 inscripciones activas");
        assertEquals(3, eliminados, "Deben haber 3 inscripciones eliminadas");
    }
}
```

### 9.3 Tests E2E (2)

#### E2E1 — JugadorE2ETest
**Ubicación:** `src/test/java/com/salesianos/gamearena/e2e/JugadorE2ETest.java`

**Tipo:** Test E2E con `@SpringBootTest` (contexto completo) + `@AutoConfigureMockMvc` (simula HTTP).

```java
@SpringBootTest                                     // Levanta toda la app Spring Boot
@AutoConfigureMockMvc                               // Configura MockMvc automáticamente
@ActiveProfiles("test")
class JugadorE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JugadorRepository jugadorRepository;

    @BeforeEach
    void setUp() {
        // @SpringBootTest NO hace rollback automático → limpiar a mano antes de cada test
        jugadorRepository.deleteAll();
    }

    @Test
    @DisplayName("E2E1 - Flujo completo de creación de jugador")
    void E2E1_flujoCreacionJugadorCompleto() throws Exception {

        // PASO 1: GET formulario → 200 OK + vista correcta + modelo con objeto vacío
        mockMvc.perform(get("/jugadores/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("jugadores/form"))
                .andExpect(model().attributeExists("jugador"));

        // PASO 2: POST con datos válidos → redirección a /jugadores
        mockMvc.perform(post("/jugadores")
                        .param("nickname",  "ElitePlayer")
                        .param("email",     "elite@gamearena.es")
                        .param("categoria", "Pro"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jugadores"));

        // PASO 3: verificar persistencia directa en BD
        boolean exists = jugadorRepository.findAll().stream()
                .anyMatch(j -> "ElitePlayer".equals(j.getNickname()) && "Pro".equals(j.getCategoria()));

        assertTrue(exists, "El jugador debe existir en BD tras la creación");
    }
}
```

#### E2E2 — InscripcionE2ETest
**Ubicación:** `src/test/java/com/salesianos/gamearena/e2e/InscripcionE2ETest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InscripcionE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JugadorRepository jugadorRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    private Long jugadorId;

    @BeforeEach
    void setUp() {
        // Borrar en orden (FK: primero inscripciones, luego jugadores)
        inscripcionRepository.deleteAll();
        jugadorRepository.deleteAll();

        // Crear un jugador válido para el POST
        Jugador j = jugadorRepository.save(new Jugador("TestPlayer", null, "Junior"));
        jugadorId = j.getId();
    }

    @Test
    @DisplayName("E2E2 - Inscripción inválida: eliminado=true sin fechaEliminacion → 200 + formulario")
    void E2E2_flujoInscripcionCasoInvalido() throws Exception {

        // POST con eliminado=true pero SIN fechaEliminacion
        // → @AssertTrue isEliminadoTieneFechaEliminacion() devuelve false
        // → controlador retorna formulario (200), NO redirige (302)
        mockMvc.perform(post("/inscripciones")
                        .param("jugadorId",        jugadorId.toString())
                        .param("torneo",            "Torneo Inválido")
                        .param("fechaInscripcion",  "2026-03-01")
                        .param("eliminado",         "true")
                        // Sin fechaEliminacion → viola @AssertTrue
                )
                .andExpect(status().isOk())
                .andExpect(view().name("inscripciones/form"));
    }
}
```

> **Import correcto en Spring Boot 4:**
> - `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
> - `@AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
> *(En Spring Boot 3 eran paquetes distintos — en SB4 se reorganizaron)*

---

## 10. CI/CD - GITHUB ACTIONS

### 10.1 Crear estructura
```
.github/
└── workflows/
    ├── ci-test.yml
    └── cd-deploy.yml
```

### 10.2 CI Workflow — `ci-test.yml`
```yaml
# =====================================================================
# CI Workflow - GameArena
# Compila y ejecuta todos los tests en cada push y PR a main/develop
# =====================================================================
name: CI - Compilar y Ejecutar Tests

on:
  push:
    branches:
      - main
      - develop
  pull_request:
    branches:
      - main
      - develop

jobs:
  build-and-test:
    name: Compilar y Testear
    runs-on: ubuntu-latest  # Ubuntu es más rápido y estable que Windows en CI

    steps:
      # Paso 1: Clonar el repositorio con todo su historial
      - name: Checkout del código fuente
        uses: actions/checkout@v4

      # Paso 2: Configurar JDK 21 (versión requerida por el proyecto)
      - name: Configurar JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'   # OpenJDK de Adoptium (gratuito y estable)
          cache: maven              # Cache de deps Maven para acelerar builds

      # Paso 3: Compilar sin tests (verificación rápida de sintaxis y estructura)
      - name: Compilar el proyecto con Maven
        run: mvn clean compile --no-transfer-progress

      # Paso 4: Ejecutar todos los tests (unitarios, integración y E2E)
      - name: Ejecutar tests con Maven
        run: mvn test --no-transfer-progress

      # Paso 5: Publicar resultados en la interfaz de GitHub (siempre, aunque fallen)
      - name: Publicar resultados de tests
        uses: dorny/test-reporter@v1
        if: always()
        with:
          name: Resultados JUnit
          path: target/surefire-reports/*.xml
          reporter: java-junit
          fail-on-error: false
```

### 10.3 CD Workflow — `cd-deploy.yml`
```yaml
# =====================================================================
# CD Workflow - GameArena
# Empaqueta el WAR y crea releases.
# Trigger: ejecución manual o PR merged a main
# =====================================================================
name: CD - Despliegue y Empaquetado

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Entorno destino del despliegue'
        required: true
        default: 'staging'
        type: choice
        options:
          - staging
          - production
  pull_request:
    branches:
      - main

jobs:
  deploy:
    name: Empaquetar y Desplegar
    runs-on: ubuntu-latest
    if: github.event_name == 'workflow_dispatch' || github.event_name == 'pull_request'

    steps:
      # Paso 1: Clonar el repositorio
      - name: Checkout del código fuente
        uses: actions/checkout@v4

      # Paso 2: Configurar Java 21 con cache de dependencias Maven
      - name: Configurar JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      # Paso 3: Tests antes de empaquetar (nunca desplegar sin pasar tests)
      - name: Ejecutar tests de verificación
        run: mvn test --no-transfer-progress

      # Paso 4: Empaquetar en WAR (-DskipTests porque ya se ejecutaron arriba)
      - name: Empaquetar proyecto en WAR
        run: mvn clean package -DskipTests --no-transfer-progress

      # Paso 5: Confirmar que el WAR fue generado y mostrar su tamaño
      - name: Verificar generación del artefacto WAR
        run: |
          echo "Archivos generados en target/:"
          ls -lh target/*.war
          echo "Verificación completada exitosamente"

      # Paso 6: Subir el WAR como artefacto de GitHub Actions (disponible 30 días)
      - name: Subir artefacto WAR
        uses: actions/upload-artifact@v4
        with:
          name: gamearena-war
          path: target/*.war
          retention-days: 30

      # Paso 7: Crear Release en GitHub (solo en ejecución manual con entorno producción)
      - name: Crear Release en GitHub
        if: github.event_name == 'workflow_dispatch' && github.event.inputs.environment == 'production'
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v${{ github.run_number }}-${{ github.event.inputs.environment }}
          name: GameArena Release ${{ github.run_number }}
          body: |
            ## GameArena - Release Automático

            **Entorno:** ${{ github.event.inputs.environment }}
            **Build:** #${{ github.run_number }}
            **Commit:** ${{ github.sha }}

            ### Instrucciones de despliegue
            1. Descargar el archivo WAR adjunto
            2. Copiar a directorio `webapps/` del servidor de aplicaciones
            3. Reiniciar el servidor si es necesario
          files: target/*.war
          draft: false
          prerelease: ${{ github.event.inputs.environment != 'production' }}
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 11. DATOS DE PRUEBA

### DataLoader
**Ubicación:** `config/DataLoader.java`

```java
@Component
@Profile("!test")   // No se ejecuta cuando el perfil activo es "test"
public class DataLoader implements CommandLineRunner {

    @Autowired
    private JugadorRepository jugadorRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Override
    public void run(String... args) throws Exception {
        if (jugadorRepository.count() > 0) return; // Evitar duplicados en reinicios

        // Jugadores de prueba en distintas categorías
        Jugador alex  = jugadorRepository.save(new Jugador("AlexStorm",   "alex@arena.es",  "Junior"));
        Jugador luna  = jugadorRepository.save(new Jugador("LunaByte",    "luna@arena.es",  "Junior"));
        Jugador mario = jugadorRepository.save(new Jugador("MarioBlast",  "mario@arena.es", "Senior"));
        Jugador sara  = jugadorRepository.save(new Jugador("SaraCode",    "sara@arena.es",  "Senior"));
        Jugador zack  = jugadorRepository.save(new Jugador("ZackPro",     "zack@arena.es",  "Pro"));

        // Inscripciones distribuidas en varios meses (activas y eliminadas)
        // Alex: 2 activas, 1 eliminada
        inscripcionRepository.save(new Inscripcion("Copa Otoño",        LocalDate.of(2025, 10, 5),  false, null,                        alex));
        inscripcionRepository.save(new Inscripcion("Liga Invierno",     LocalDate.of(2025, 12, 10), false, null,                        alex));
        inscripcionRepository.save(new Inscripcion("Torneo Navidad",    LocalDate.of(2025, 12, 20), true,  LocalDate.of(2025, 12, 22),  alex));

        // Luna: 1 activa, 1 eliminada
        inscripcionRepository.save(new Inscripcion("Copa Otoño",        LocalDate.of(2025, 10, 8),  true,  LocalDate.of(2025, 10, 15),  luna));
        inscripcionRepository.save(new Inscripcion("Liga Invierno",     LocalDate.of(2025, 12, 12), false, null,                        luna));

        // Mario: 4 activas (el más activo)
        inscripcionRepository.save(new Inscripcion("Copa Otoño",        LocalDate.of(2025, 9,  1),  false, null,                        mario));
        inscripcionRepository.save(new Inscripcion("Liga Invierno",     LocalDate.of(2025, 11, 5),  false, null,                        mario));
        inscripcionRepository.save(new Inscripcion("Torneo Navidad",    LocalDate.of(2025, 12, 18), false, null,                        mario));
        inscripcionRepository.save(new Inscripcion("Copa Primavera",    LocalDate.of(2026, 1,  15), false, null,                        mario));

        // Sara: 1 eliminada
        inscripcionRepository.save(new Inscripcion("Copa Primavera",    LocalDate.of(2026, 1,  20), true,  LocalDate.of(2026, 2,  1),   sara));

        // Zack: 1 activa, 1 eliminada
        inscripcionRepository.save(new Inscripcion("Copa Primavera",    LocalDate.of(2026, 2,  5),  false, null,                        zack));
        inscripcionRepository.save(new Inscripcion("Liga Invierno",     LocalDate.of(2025, 11, 20), true,  LocalDate.of(2025, 11, 25),  zack));
    }
}
```

---

## 12. ORDEN DE IMPLEMENTACIÓN RECOMENDADO

1. **Configuración inicial**
   - `application.properties` y `application-test.properties`
   - `GameArenaApplication.java`

2. **Entidades**
   - `Jugador.java` (con relación `@OneToMany`, helpers, validaciones básicas)
   - `Inscripcion.java` (con `@ManyToOne`, `@AssertTrue`, validaciones cruzadas)

3. **Capa de datos**
   - `JugadorRepository.java` + `InscripcionRepository.java` con `@Query`
   - `JugadorService.java` + `InscripcionService.java`

4. **Controladores Web MVC**
   - `layout.html` + `fragments/navbar.html`
   - CRUD Jugadores: `JugadorController` + vistas `jugadores/`
   - CRUD Inscripciones: `InscripcionController` + vistas `inscripciones/`
   - `HomeController` (redirect `/` → `/jugadores`)

5. **API REST**
   - `JugadorRestController` + `InscripcionRestController`
   - `KPIRestController`

6. **KPI Dashboard**
   - `KPIController` (solo devuelve la vista)
   - `kpi/dashboard.html` con los 4 canvas + JS Chart.js vía fetch

7. **Tests**
   - `InscripcionTest.java` (UT1, UT2)
   - `JugadorRepositoryTest.java` (IT1) + `InscripcionRepositoryTest.java` (IT2)
   - `JugadorE2ETest.java` (E2E1) + `InscripcionE2ETest.java` (E2E2)

8. **CI/CD**
   - `.github/workflows/ci-test.yml`
   - `.github/workflows/cd-deploy.yml`

9. **DataLoader** (opcional, para pruebas manuales)

---

## 13. CHECKLIST DE VALIDACIÓN

### Funcionalidad
- [ ] CRUD completo de Jugadores funciona
- [ ] CRUD completo de Inscripciones funciona
- [ ] Selector de jugador funciona en formulario de inscripción
- [ ] Validaciones de formulario muestran mensajes de error correctamente
- [ ] Mensajes flash (success/error) se muestran y se pueden cerrar
- [ ] Navegación entre secciones funciona

### API REST
- [ ] `GET /api/jugadores` devuelve lista JSON
- [ ] `POST /api/jugadores` crea y devuelve 201
- [ ] `PUT /api/jugadores/{id}` actualiza y devuelve 200 o 404
- [ ] `DELETE /api/jugadores/{id}` elimina y devuelve 204 o 404
- [ ] Lo mismo para `/api/inscripciones`
- [ ] Endpoints KPI devuelven JSON con el formato correcto

### KPI Dashboard
- [ ] Gráfico Jugadores por categoría (bar) se renderiza
- [ ] Gráfico Inscripciones por estado (pie) se renderiza
- [ ] Gráfico Inscripciones por mes (bar) se renderiza
- [ ] Gráfico Top 5 jugadores (radar) se renderiza
- [ ] Datos se cargan correctamente vía AJAX

### Tests
- [ ] UT1 pasa: `eliminado=true` sin fecha → violación detectada
- [ ] UT2 pasa: fecha eliminación anterior a inscripción → violación detectada
- [ ] IT1 pasa: agrupación por categoría retorna grupos correctos
- [ ] IT2 pasa: conteo activos/eliminados retorna valores correctos
- [ ] E2E1 pasa: flujo completo creación de jugador
- [ ] E2E2 pasa: inscripción inválida → HTTP 200 + formulario (no redirige)

### CI/CD
- [ ] Workflow CI se ejecuta automáticamente en push a main/develop
- [ ] Workflow CI ejecuta todos los tests y publica resultados
- [ ] Workflow CD genera el WAR correctamente
- [ ] Workflows tienen comentarios en cada paso
- [ ] Artefacto WAR se sube a GitHub Actions

---

## 14. COMANDOS ÚTILES

### Maven
```bash
# Compilar el proyecto
mvn clean compile

# Ejecutar todos los tests
mvn test

# Ejecutar solo tests unitarios (por nombre de clase)
mvn test -Dtest=InscripcionTest

# Ejecutar solo tests de integración
mvn test -Dtest=JugadorRepositoryTest,InscripcionRepositoryTest

# Ejecutar solo tests E2E
mvn test -Dtest=JugadorE2ETest,InscripcionE2ETest

# Empaquetar en WAR
mvn clean package

# Ejecutar la aplicación en local
mvn spring-boot:run
```

### Acceso a la aplicación
- **Web:** http://localhost:8080
- **H2 Console:** http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:gamearenadb`
  - User: `sa`
  - Password: *(vacío)*

---

## 15. NOTAS TÉCNICAS IMPORTANTES

### Validaciones cruzadas en Inscripcion
Las validaciones que dependen de varios campos (`eliminado` + `fechaEliminacion`) no pueden hacerse con anotaciones simples de Jakarta. Se usan métodos booleanos con `@AssertTrue` dentro de la propia entidad:

```java
@AssertTrue(message = "Si la inscripción está eliminada, la fecha de eliminación es obligatoria")
public boolean isEliminadoTieneFechaEliminacion() {
    if (eliminado) return fechaEliminacion != null;
    return true;
}

@AssertTrue(message = "La fecha de eliminación debe ser igual o posterior a la fecha de inscripción")
public boolean isFechaEliminacionValida() {
    if (fechaEliminacion != null && fechaInscripcion != null) {
        return !fechaEliminacion.isBefore(fechaInscripcion);
    }
    return true;
}
```

El nombre del `propertyPath` en los tests es el nombre del método **sin el prefijo `is`** y con la primera letra en minúscula:
- Método `isEliminadoTieneFechaEliminacion()` → path `"eliminadoTieneFechaEliminacion"`
- Método `isFechaEliminacionValida()` → path `"fechaEliminacionValida"`

### Relación @ManyToOne en formularios MVC
Spring MVC no puede hacer binding automático de `jugador` (objeto entidad) desde un `<select>`. La solución es:
1. El `<select>` envía `jugadorId` (el Long del ID)
2. El controlador recibe `jugadorId` como `@RequestParam`
3. Se asigna el jugador al objeto **antes** de validar con el `Validator` de Jakarta manualmente
4. Si hay violaciones, se registran en el `BindingResult`

### Layout Thymeleaf con fragments parametrizados
- El `th:fragment="layout(content, pageTitle, scripts)"` va en el elemento `<html>` raíz del layout
- Las páginas hijas usan `th:replace="~{layout :: layout(~{::content}, ~{::title}, _)}"` en su `<html>`
- `_` = pasar nulo (para páginas sin scripts extra)
- Dentro del layout, usar `th:insert="${content}"` (no `th:replace`) para mantener el div contenedor

### DataLoader y perfil de test
El `DataLoader` está anotado con `@Profile("!test")` para que no se ejecute durante los tests. Los tests E2E usan `@ActiveProfiles("test")` y limpian la BD en `@BeforeEach`.

### Imports correctos en Spring Boot 4
Spring Boot 4 reorganizó algunos paquetes de test respecto a SB3:

| Clase | Spring Boot 3 | Spring Boot 4 |
|-------|--------------|---------------|
| `@DataJpaTest` | `org.springframework.boot.test.autoconfigure.orm.jpa` | `org.springframework.boot.data.jpa.test.autoconfigure` |
| `@AutoConfigureMockMvc` | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |

### CDNs utilizados
```
Bootstrap 5.3.8 CSS:
https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css

Bootstrap 5.3.8 JS:
https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js

Bootstrap Icons 1.11.3:
https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css

Chart.js 4.4.0:
https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js
```

---

**Fin del Plan de Desarrollo — GameArena**
