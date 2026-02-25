# CAMBIO.md — Guía de cambios frecuentes en el proyecto

Referencia rápida para los cambios más comunes que se pueden pedir:
cambiar la base de datos, cambiar entidades y tablas, añadir más workflows de CI/CD y añadir más tests.

---

## 1. CAMBIAR LA BASE DE DATOS

### Situación actual
```
App principal  →  MySQL   (application.properties)
Tests          →  H2      (application-test.properties + @ActiveProfiles("test"))
```

---

### 1.A De MySQL → H2 (volver a H2 como BD principal)

#### Paso 1 — `pom.xml`
Cambiar el scope de H2 de `test` a `runtime` y eliminar (o comentar) MySQL:

```xml
<!-- Quitar o comentar esto: -->
<!--
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
-->

<!-- H2 pasa a runtime (disponible en ejecución normal) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

#### Paso 2 — `application.properties`
Sustituir toda la sección de datasource:

```properties
server.port=8080

# H2 Database
spring.datasource.url=jdbc:h2:mem:biblioboxdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=1234

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# H2 Console (útil para inspeccionar la BD en el navegador)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Thymeleaf
spring.thymeleaf.cache=false

# Logging
logging.level.org.springframework.web=DEBUG
logging.level.com.salesianos.bibliobox=DEBUG
```

> `ddl-auto=create-drop` es lo normal con H2: crea las tablas al arrancar y las borra al parar.
> La consola H2 queda en http://localhost:8080/h2-console

#### Paso 3 — `application-test.properties`
Los tests ya usaban H2 → no cambia nada. Puede dejarse igual o apuntar a una URL distinta:

```properties
spring.datasource.url=jdbc:h2:mem:bibliobox-test;DB_CLOSE_DELAY=-1
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
```

---

### 1.B De H2 → MySQL (lo que ya está configurado)

#### Paso 1 — Crear la BD en MySQL antes de arrancar
```sql
CREATE DATABASE bibliobox CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### Paso 2 — `pom.xml`
```xml
<!-- MySQL - Base de datos principal -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- H2 - Solo para tests -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

#### Paso 3 — `application.properties`
```properties
server.port=8080

# MySQL - Base de datos principal
spring.datasource.url=jdbc:mysql://localhost:3306/bibliobox?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
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
logging.level.com.salesianos.bibliobox=DEBUG
```

> `ddl-auto=update` crea las tablas si no existen y las actualiza al cambiar el esquema.
> **No usar `create-drop` con MySQL** o perderás todos los datos al parar la app.

#### Paso 4 — `application-test.properties`
Los tests siguen usando H2 → no cambia nada.

---

### 1.C De MySQL → PostgreSQL

#### Paso 1 — Crear la BD en PostgreSQL
```sql
CREATE DATABASE bibliobox;
```

#### Paso 2 — `pom.xml`
Cambiar el conector MySQL por el de PostgreSQL:

```xml
<!-- Quitar mysql-connector-j y poner: -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- H2 sigue siendo scope=test -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

#### Paso 3 — `application.properties`
```properties
server.port=8080

# PostgreSQL - Base de datos principal
spring.datasource.url=jdbc:postgresql://localhost:5432/bibliobox
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=1234

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Thymeleaf
spring.thymeleaf.cache=false

# Logging
logging.level.org.springframework.web=DEBUG
logging.level.com.salesianos.bibliobox=DEBUG
```

#### Paso 4 — `application-test.properties`
Los tests siguen usando H2 → no cambia nada.

> **Nota sobre H2 con PostgreSQL:** H2 tiene un modo de compatibilidad con PostgreSQL.
> Si alguna `@Query` JPQL nativa da problemas en H2, añadir al test properties:
> `spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`

---

### Tabla resumen — qué cambia en cada BD

| Elemento | H2 | MySQL | PostgreSQL |
|---|---|---|---|
| Dependencia `pom.xml` | `com.h2database:h2` runtime | `com.mysql:mysql-connector-j` runtime | `org.postgresql:postgresql` runtime |
| JDBC URL | `jdbc:h2:mem:nombre` | `jdbc:mysql://localhost:3306/nombre?...` | `jdbc:postgresql://localhost:5432/nombre` |
| Driver class | `org.h2.Driver` | `com.mysql.cj.jdbc.Driver` | `org.postgresql.Driver` |
| Dialecto JPA | `H2Dialect` | `MySQLDialect` | `PostgreSQLDialect` |
| `ddl-auto` recomendado | `create-drop` | `update` | `update` |
| Consola web | Sí (`/h2-console`) | No (usar MySQL Workbench) | No (usar pgAdmin) |
| Tests (`application-test.properties`) | igual | H2 igual | H2 igual |

---

## 2. MÁS WORKFLOWS DE GITHUB ACTIONS

Los workflows van en `.github/workflows/`. Cada archivo `.yml` es un workflow independiente.

---

### 2.A Workflow de calidad de código con Checkstyle

**Archivo:** `.github/workflows/code-quality.yml`

Comprueba que el código cumple las reglas de estilo (indentación, nombres, etc.) en cada PR.

```yaml
name: Calidad de Código - Checkstyle

on:
  pull_request:
    branches:
      - main
      - develop

jobs:
  checkstyle:
    name: Analizar estilo de código
    runs-on: ubuntu-latest

    steps:
      # Paso 1: Clonar el repositorio
      - name: Checkout del código fuente
        uses: actions/checkout@v4

      # Paso 2: Configurar JDK 21
      - name: Configurar JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      # Paso 3: Ejecutar Checkstyle con Maven
      # Requiere el plugin checkstyle en el pom.xml (ver nota abajo)
      - name: Ejecutar Checkstyle
        run: mvn checkstyle:check --no-transfer-progress

      # Paso 4: Subir el informe de Checkstyle como artefacto
      - name: Subir informe Checkstyle
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: checkstyle-report
          path: target/checkstyle-result.xml
          retention-days: 7
```

> Para activarlo, añadir en `pom.xml` dentro de `<build><plugins>`:
> ```xml
> <plugin>
>     <groupId>org.apache.maven.plugins</groupId>
>     <artifactId>maven-checkstyle-plugin</artifactId>
>     <version>3.3.1</version>
>     <configuration>
>         <configLocation>google_checks.xml</configLocation>
>         <failsOnError>true</failsOnError>
>     </configuration>
> </plugin>
> ```

---

### 2.B Workflow de tests con MySQL real (integración real)

**Archivo:** `.github/workflows/ci-mysql.yml`

Levanta un MySQL real en el runner de GitHub (service container) y ejecuta los tests contra él.
Útil para verificar que las `@Query` funcionan igual en MySQL que en H2.

```yaml
name: CI - Tests con MySQL real

on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main

jobs:
  test-mysql:
    name: Tests contra MySQL
    runs-on: ubuntu-latest

    # Service container: levanta MySQL antes de ejecutar los steps
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: 1234
          MYSQL_DATABASE: bibliobox_test
        ports:
          - 3306:3306
        # Esperar a que MySQL esté listo para aceptar conexiones
        options: >-
          --health-cmd="mysqladmin ping -h localhost"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=5

    steps:
      # Paso 1: Clonar el repositorio
      - name: Checkout del código fuente
        uses: actions/checkout@v4

      # Paso 2: Configurar JDK 21
      - name: Configurar JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      # Paso 3: Ejecutar tests con perfil mysql-test
      # Las variables de entorno sobreescriben el application.properties
      - name: Ejecutar tests contra MySQL
        run: mvn test --no-transfer-progress
        env:
          SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/bibliobox_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
          SPRING_DATASOURCE_USERNAME: root
          SPRING_DATASOURCE_PASSWORD: 1234
          SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.MySQLDialect
          SPRING_JPA_HIBERNATE_DDL_AUTO: create-drop

      # Paso 4: Publicar resultados
      - name: Publicar resultados de tests
        uses: dorny/test-reporter@v1
        if: always()
        with:
          name: Resultados JUnit (MySQL)
          path: target/surefire-reports/*.xml
          reporter: java-junit
          fail-on-error: false
```

---

### 2.C Workflow de seguridad con OWASP Dependency Check

**Archivo:** `.github/workflows/security.yml`

Analiza las dependencias del proyecto en busca de vulnerabilidades conocidas (CVEs).
Se ejecuta una vez a la semana y en cada PR a main.

```yaml
name: Seguridad - OWASP Dependency Check

on:
  schedule:
    # Ejecutar todos los lunes a las 08:00 UTC
    - cron: '0 8 * * 1'
  pull_request:
    branches:
      - main

jobs:
  dependency-check:
    name: Análisis de vulnerabilidades
    runs-on: ubuntu-latest

    steps:
      # Paso 1: Clonar el repositorio
      - name: Checkout del código fuente
        uses: actions/checkout@v4

      # Paso 2: Configurar JDK 21
      - name: Configurar JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      # Paso 3: Ejecutar OWASP Dependency Check
      # Genera un informe HTML con las vulnerabilidades encontradas
      - name: Ejecutar análisis OWASP
        run: mvn org.owasp:dependency-check-maven:check --no-transfer-progress
        continue-on-error: true   # No bloquear el pipeline, solo informar

      # Paso 4: Subir el informe HTML como artefacto descargable
      - name: Subir informe de seguridad
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: owasp-dependency-report
          path: target/dependency-check-report.html
          retention-days: 30
```

---

### 2.D Workflow de cobertura de tests con JaCoCo

**Archivo:** `.github/workflows/coverage.yml`

Mide el porcentaje de código cubierto por los tests y publica el informe.

```yaml
name: Cobertura de Tests - JaCoCo

on:
  push:
    branches:
      - main
      - develop

jobs:
  coverage:
    name: Medir cobertura de tests
    runs-on: ubuntu-latest

    steps:
      # Paso 1: Clonar el repositorio
      - name: Checkout del código fuente
        uses: actions/checkout@v4

      # Paso 2: Configurar JDK 21
      - name: Configurar JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      # Paso 3: Ejecutar tests y generar informe JaCoCo
      # JaCoCo se activa con el goal verify (no solo test)
      - name: Ejecutar tests con cobertura JaCoCo
        run: mvn verify --no-transfer-progress

      # Paso 4: Subir el informe HTML de cobertura
      - name: Subir informe de cobertura
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: jacoco-coverage-report
          path: target/site/jacoco/
          retention-days: 14
```

> Para activarlo, añadir en `pom.xml` dentro de `<build><plugins>`:
> ```xml
> <plugin>
>     <groupId>org.jacoco</groupId>
>     <artifactId>jacoco-maven-plugin</artifactId>
>     <version>0.8.11</version>
>     <executions>
>         <execution>
>             <goals><goal>prepare-agent</goal></goals>
>         </execution>
>         <execution>
>             <id>report</id>
>             <phase>verify</phase>
>             <goals><goal>report</goal></goals>
>         </execution>
>     </executions>
> </plugin>
> ```

---

### Tabla resumen — workflows disponibles

| Archivo | Cuándo se ejecuta | Qué hace |
|---|---|---|
| `ci-test.yml` | Push/PR a main y develop | Compila y pasa todos los tests con H2 |
| `cd-deploy.yml` | Manual o PR a main | Empaqueta el WAR y crea releases |
| `code-quality.yml` | PR a main y develop | Checkstyle: valida el estilo del código |
| `ci-mysql.yml` | Push/PR a main | Tests completos contra MySQL real |
| `security.yml` | Lunes 08:00 + PR a main | OWASP: busca vulnerabilidades en dependencias |
| `coverage.yml` | Push a main y develop | JaCoCo: mide la cobertura de tests |

---

## 3. MÁS TESTS

Los tests del proyecto siguen 3 niveles. Aquí se explica cómo añadir más de cada tipo.

---

### Nomenclatura que se sigue

| Prefijo | Tipo | Herramienta |
|---|---|---|
| `UT` | Unitario | Validator Jakarta (sin Spring) |
| `IT` | Integración | `@DataJpaTest` + H2 |
| `E2E` | End-to-End | `@SpringBootTest` + MockMvc |

---

### 3.A Más tests unitarios (UT) — validaciones de entidad

Los tests unitarios validan las reglas de negocio de las entidades sin levantar Spring.
Se añaden en la misma clase `PrestamoTest` o en una nueva clase para otra entidad.

**Ejemplo UT3 — título de libro no puede estar en blanco:**
```java
@Test
@DisplayName("UT3 - tituloLibro en blanco genera violación @NotBlank")
void UT3_tituloLibroEnBlancoGeneraViolacion() {

    Alumno alumno = new Alumno("Test", "1ºESO-A", null);

    // INVÁLIDO: tituloLibro vacío — viola @NotBlank
    Prestamo prestamo = new Prestamo(
            "",                          // <-- blank: viola @NotBlank
            LocalDate.of(2026, 1, 1),
            false,
            null,
            alumno
    );

    Set<ConstraintViolation<Prestamo>> violations = validator.validate(prestamo);

    boolean hasTituloViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("tituloLibro"));

    assertTrue(hasTituloViolation, "Debe haber violación cuando tituloLibro está en blanco");
}
```

**Ejemplo UT4 — préstamo válido NO genera violaciones:**
```java
@Test
@DisplayName("UT4 - préstamo completamente válido no genera ninguna violación")
void UT4_prestamoValidoNoGeneraViolaciones() {

    Alumno alumno = new Alumno("Test", "2ºESO-B", null);

    // VÁLIDO: devuelto=true con fechaDevolucion posterior a fechaPrestamo
    Prestamo prestamo = new Prestamo(
            "El Quijote",
            LocalDate.of(2026, 1, 10),
            true,
            LocalDate.of(2026, 1, 20),  // posterior a fechaPrestamo: válida
            alumno
    );

    Set<ConstraintViolation<Prestamo>> violations = validator.validate(prestamo);

    // No debe haber ninguna violación
    assertTrue(violations.isEmpty(), "Un préstamo válido no debe generar violaciones");
}
```

---

### 3.B Más tests de integración (IT) — repositorios

Los tests de integración verifican que las `@Query` JPQL funcionan correctamente contra H2.
Se añaden en las clases `AlumnoRepositoryTest` o `PrestamoRepositoryTest`.

**Ejemplo IT3 — findByDevueltoFalse devuelve solo préstamos pendientes:**
```java
// En PrestamoRepositoryTest
@Test
@DisplayName("IT3 - findByDevueltoFalse devuelve solo préstamos pendientes")
void IT3_findByDevueltoFalseDevuelveSoloPendientes() {

    // ARRANGE
    Alumno alumno = alumnoRepository.save(new Alumno("Alumno IT3", "3ºESO-A", null));

    // 1 devuelto, 2 pendientes
    prestamoRepository.save(new Prestamo("Libro A", LocalDate.of(2026, 1, 1), true,  LocalDate.of(2026, 1, 10), alumno));
    prestamoRepository.save(new Prestamo("Libro B", LocalDate.of(2026, 2, 1), false, null,                      alumno));
    prestamoRepository.save(new Prestamo("Libro C", LocalDate.of(2026, 3, 1), false, null,                      alumno));

    // ACT
    List<Prestamo> pendientes = prestamoRepository.findByDevueltoFalse();

    // ASSERT: solo deben devolverse los 2 pendientes
    assertEquals(2, pendientes.size(), "Debe haber exactamente 2 préstamos pendientes");
    assertTrue(pendientes.stream().noneMatch(Prestamo::isDevuelto),
            "Ningún préstamo devuelto por findByDevueltoFalse debe tener devuelto=true");
}
```

**Ejemplo IT4 — findByAlumnoId devuelve solo los préstamos de ese alumno:**
```java
// En PrestamoRepositoryTest
@Test
@DisplayName("IT4 - findByAlumnoId devuelve solo los préstamos del alumno indicado")
void IT4_findByAlumnoIdDevuelveSoloSusPrestamos() {

    // ARRANGE: 2 alumnos con préstamos distintos
    Alumno alumno1 = alumnoRepository.save(new Alumno("Alumno Uno", "1ºESO-A", null));
    Alumno alumno2 = alumnoRepository.save(new Alumno("Alumno Dos", "1ºESO-B", null));

    prestamoRepository.save(new Prestamo("Libro X", LocalDate.of(2026, 1, 1), false, null, alumno1));
    prestamoRepository.save(new Prestamo("Libro Y", LocalDate.of(2026, 2, 1), false, null, alumno1));
    prestamoRepository.save(new Prestamo("Libro Z", LocalDate.of(2026, 3, 1), false, null, alumno2));

    // ACT: buscar solo los del alumno1
    List<Prestamo> prestamosAlumno1 = prestamoRepository.findByAlumnoId(alumno1.getId());

    // ASSERT
    assertEquals(2, prestamosAlumno1.size(), "El alumno1 debe tener 2 préstamos");
    assertTrue(prestamosAlumno1.stream()
                    .allMatch(p -> p.getAlumno().getId().equals(alumno1.getId())),
            "Todos los préstamos deben pertenecer al alumno1");
}
```

**Ejemplo IT5 — top5 alumnos devuelve como máximo 5 resultados:**
```java
// En AlumnoRepositoryTest
@Test
@DisplayName("IT5 - getTop5AlumnosConMasPrestamos devuelve máximo 5 resultados")
void IT5_top5DevuelveMaximo5() {

    // ARRANGE: crear 7 alumnos con distinto número de préstamos
    for (int i = 1; i <= 7; i++) {
        Alumno a = alumnoRepository.save(new Alumno("Alumno " + i, "1ºESO-A", null));
        // El alumno i tiene i préstamos (para que haya orden claro)
        for (int j = 0; j < i; j++) {
            prestamoRepository.save(new Prestamo("Libro", LocalDate.of(2026, 1, j + 1), false, null, a));
        }
    }

    // ACT
    List<Map<String, Object>> top5 = alumnoRepository.getTop5AlumnosConMasPrestamos();

    // ASSERT: nunca más de 5 resultados
    assertTrue(top5.size() <= 5, "El resultado no debe superar los 5 alumnos");
}
```

> Para IT5 necesitas `@Autowired PrestamoRepository` también en `AlumnoRepositoryTest`,
> ya que `@DataJpaTest` inyecta todos los repositorios disponibles.

---

### 3.C Más tests E2E — controladores web

Los tests E2E levantan toda la app con Spring completo y simulan peticiones HTTP reales.

**Ejemplo E2E3 — GET /alumnos devuelve 200 con la lista:**
```java
// Clase: AlumnoE2ETest
@Test
@DisplayName("E2E3 - GET /alumnos devuelve 200 y la vista correcta")
void E2E3_getAlumnosDevuelve200() throws Exception {

    mockMvc.perform(get("/alumnos"))
            .andExpect(status().isOk())
            .andExpect(view().name("alumnos/list"))
            // El modelo debe contener el atributo "alumnos" (lista, posiblemente vacía)
            .andExpect(model().attributeExists("alumnos"));
}
```

**Ejemplo E2E4 — GET /alumnos/{id} inexistente redirige con mensaje de error:**
```java
// Clase: AlumnoE2ETest
@Test
@DisplayName("E2E4 - GET /alumnos/{id} con ID inexistente redirige a /alumnos")
void E2E4_getAlumnoInexistenteRedirige() throws Exception {

    mockMvc.perform(get("/alumnos/99999"))
            // Debe redirigir (302) al listado
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/alumnos"));
}
```

**Ejemplo E2E5 — POST /alumnos con datos inválidos vuelve al formulario:**
```java
// Clase: AlumnoE2ETest
@Test
@DisplayName("E2E5 - POST /alumnos con nombre en blanco vuelve al formulario con errores")
void E2E5_postAlumnoInvalidoVuelveAlFormulario() throws Exception {

    mockMvc.perform(post("/alumnos")
                    .param("nombre", "")          // INVÁLIDO: vacío, viola @NotBlank
                    .param("curso",  "1ºESO-A")
                    .param("email",  ""))
            // Debe volver al formulario (200), no redirigir
            .andExpect(status().isOk())
            .andExpect(view().name("alumnos/form"))
            // El modelo debe tener errores en el campo "nombre"
            .andExpect(model().attributeHasFieldErrors("alumno", "nombre"));
}
```

**Ejemplo E2E6 — GET /alumnos/delete/{id} elimina y redirige:**
```java
// Clase: AlumnoE2ETest
@Test
@DisplayName("E2E6 - GET /alumnos/delete/{id} elimina el alumno y redirige")
void E2E6_deleteAlumnoExistenteRedirige() throws Exception {

    // ARRANGE: crear un alumno en BD para después borrarlo
    Alumno alumno = alumnoRepository.save(new Alumno("A borrar", "1ºESO-A", null));
    Long id = alumno.getId();

    // ACT + ASSERT: la petición de borrado redirige al listado
    mockMvc.perform(get("/alumnos/delete/" + id))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/alumnos"));

    // Verificar que ya no existe en BD
    assertFalse(alumnoRepository.existsById(id),
            "El alumno debe haber sido eliminado de la BD");
}
```

**Ejemplo E2E7 — GET /api/alumnos devuelve JSON:**
```java
// Nueva clase: AlumnoRestE2ETest (mismas anotaciones que AlumnoE2ETest)
@Test
@DisplayName("E2E7 - GET /api/alumnos devuelve 200 con JSON")
void E2E7_getApiAlumnosDevuelveJson() throws Exception {

    // ARRANGE: insertar un alumno para que la lista no esté vacía
    alumnoRepository.save(new Alumno("Juan API", "2ºESO-B", "juan@test.es"));

    mockMvc.perform(get("/api/alumnos")
                    .accept(org.springframework.http.MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            // El content-type de la respuesta debe ser JSON
            .andExpect(content().contentTypeCompatibleWith(
                    org.springframework.http.MediaType.APPLICATION_JSON))
            // La respuesta debe ser un array JSON no vacío
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
}
```

> Para usar `jsonPath` necesitas este import estático:
> `import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;`
> y añadir en el `pom.xml` (scope test):
> ```xml
> <dependency>
>     <groupId>org.hamcrest</groupId>
>     <artifactId>hamcrest</artifactId>
>     <scope>test</scope>
> </dependency>
> ```

---

### Tabla resumen — todos los tests

| ID | Tipo | Clase | Qué verifica |
|---|---|---|---|
| UT1 | Unitario | `PrestamoTest` | `devuelto=true` sin fecha → violación |
| UT2 | Unitario | `PrestamoTest` | fecha devolución anterior al préstamo → violación |
| UT3 | Unitario | `PrestamoTest` | título en blanco → violación `@NotBlank` |
| UT4 | Unitario | `PrestamoTest` | préstamo completamente válido → sin violaciones |
| IT1 | Integración | `AlumnoRepositoryTest` | `@Query` GROUP BY curso agrupa correctamente |
| IT2 | Integración | `PrestamoRepositoryTest` | `@Query` SUM cuenta devueltos y pendientes |
| IT3 | Integración | `PrestamoRepositoryTest` | `findByDevueltoFalse` filtra correctamente |
| IT4 | Integración | `PrestamoRepositoryTest` | `findByAlumnoId` devuelve solo los suyos |
| IT5 | Integración | `AlumnoRepositoryTest` | `top5` nunca devuelve más de 5 |
| E2E1 | E2E | `AlumnoE2ETest` | Flujo completo creación alumno |
| E2E2 | E2E | `PrestamoE2ETest` | Préstamo inválido → 200 + formulario |
| E2E3 | E2E | `AlumnoE2ETest` | GET /alumnos → 200 + vista correcta |
| E2E4 | E2E | `AlumnoE2ETest` | GET /alumnos/99999 → redirect |
| E2E5 | E2E | `AlumnoE2ETest` | POST alumno inválido → 200 + errores en modelo |
| E2E6 | E2E | `AlumnoE2ETest` | DELETE alumno → redirect + eliminado de BD |
| E2E7 | E2E | `AlumnoRestE2ETest` | GET /api/alumnos → 200 JSON |

---

## 4. CAMBIAR ENTIDADES Y TABLAS

Esta sección cubre todos los escenarios posibles al modificar el modelo de datos:
añadir campos, cambiar nombres, añadir una entidad nueva o eliminar una existente.

---

### 4.A Añadir un campo nuevo a una entidad existente

**Ejemplo:** añadir `telefono` a `Alumno`.

#### Paso 1 — La entidad
Añadir el campo con sus anotaciones de validación:
```java
// En Alumno.java
@Size(min = 9, max = 15, message = "El teléfono debe tener entre 9 y 15 caracteres")
private String telefono;   // nullable: sin @NotBlank, el campo es opcional

// Getter y setter obligatorios (no hay Lombok)
public String getTelefono() { return telefono; }
public void setTelefono(String telefono) { this.telefono = telefono; }
```

#### Paso 2 — La BD (automático con `ddl-auto=update`)
Con MySQL y `ddl-auto=update`, Hibernate añade la columna sola al arrancar.
No hay que tocar nada en la BD manualmente.

> Con H2 y `ddl-auto=create-drop` (tests) también es automático.
> Con `ddl-auto=none` (producción estricta) habría que escribir el ALTER TABLE a mano:
> ```sql
> ALTER TABLE alumnos ADD COLUMN telefono VARCHAR(15);
> ```

#### Paso 3 — El formulario Thymeleaf
Añadir el campo en `templates/alumnos/form.html`:
```html
<div class="mb-3">
    <label class="form-label">Teléfono</label>
    <input type="text" th:field="*{telefono}" class="form-control"
           th:classappend="${#fields.hasErrors('telefono')} ? 'is-invalid'">
    <div class="invalid-feedback" th:errors="*{telefono}"></div>
</div>
```

#### Paso 4 — La vista de detalle y lista
- En `detail.html`: añadir `<td th:text="${alumno.telefono ?: '-'}"></td>`
- En `list.html`: añadir la columna si tiene sentido mostrarla

#### Paso 5 — El constructor (si se usa en DataLoader o tests)
Si el constructor tiene parámetros fijos, añadir el nuevo campo:
```java
// Constructor actualizado
public Alumno(String nombre, String curso, String email, String telefono) {
    this.nombre = nombre;
    this.curso = curso;
    this.email = email;
    this.telefono = telefono;
}
```
Y actualizar las llamadas en `DataLoader.java` y en los tests (`PrestamoTest`, `AlumnoRepositoryTest`, `AlumnoE2ETest`).

> **Truco:** si el campo es opcional y no quieres cambiar todos los constructores,
> añade solo el setter y asigna el valor después de construir el objeto:
> ```java
> Alumno a = new Alumno("Ana", "1ºESO-A", null);
> a.setTelefono("612345678");
> alumnoRepository.save(a);
> ```

---

### 4.B Renombrar un campo existente

**Ejemplo:** renombrar `tituloLibro` → `titulo` en `Prestamo`.

#### Paso 1 — La entidad
```java
// Antes:
private String tituloLibro;

// Después:
private String titulo;

// Actualizar getter y setter:
public String getTitulo() { return titulo; }
public void setTitulo(String titulo) { this.titulo = titulo; }
```

#### Paso 2 — La BD
Con `ddl-auto=update` Hibernate **no renombra columnas automáticamente** — crea una columna nueva `titulo` vacía y deja `titulo_libro` intacta con sus datos.

Si se quiere conservar los datos hay que hacer la migración a mano:
```sql
-- En MySQL: copiar datos y eliminar la columna vieja
ALTER TABLE prestamos ADD COLUMN titulo VARCHAR(255);
UPDATE prestamos SET titulo = titulo_libro;
ALTER TABLE prestamos DROP COLUMN titulo_libro;
```

Si los datos no importan (desarrollo), simplemente reiniciar con `ddl-auto=create-drop` limpia todo.

#### Paso 3 — Todas las referencias en el código
Buscar y reemplazar `tituloLibro` / `getTituloLibro` / `setTituloLibro` en:
- `Prestamo.java` ← campo, getter, setter, constructor
- `PrestamoController.java` ← si hay referencias directas
- `PrestamoRepository.java` ← si hay `@Query` que usan el nombre del campo JPQL
- `templates/prestamos/form.html` ← `th:field="*{titulo}"`
- `templates/prestamos/list.html` ← `th:text="${prestamo.titulo}"`
- `templates/prestamos/detail.html`
- `DataLoader.java` ← constructor o setter
- Todos los tests que construyan un `Prestamo`

> **Atajo en Eclipse/IntelliJ:** click derecho sobre el campo → Refactor → Rename.
> Renombra el campo y todos sus usos en Java de golpe. Las plantillas HTML hay que actualizarlas a mano.

---

### 4.C Cambiar el nombre de la tabla

**Ejemplo:** renombrar la tabla `alumnos` → `estudiantes`.

#### Paso 1 — La entidad
```java
// Antes:
@Entity
@Table(name = "alumnos")
public class Alumno { ... }

// Después:
@Entity
@Table(name = "estudiantes")
public class Alumno { ... }
```

#### Paso 2 — La BD
Con `ddl-auto=update` Hibernate **no renombra tablas** — crea una tabla nueva `estudiantes` vacía y deja `alumnos` intacta.

Migración manual si se necesitan conservar datos:
```sql
RENAME TABLE alumnos TO estudiantes;
```

O si los datos no importan, cambiar a `ddl-auto=create-drop` para recrear todo limpio.

#### Paso 3 — Revisar `@JoinColumn` en entidades relacionadas
En `Prestamo.java`, la FK sigue apuntando a la columna correcta porque usa el ID, no el nombre de tabla. No hace falta cambiarlo:
```java
@JoinColumn(name = "alumno_id", nullable = false)  // columna en tabla prestamos, no cambia
```

---

### 4.D Añadir una entidad completamente nueva

**Ejemplo:** añadir la entidad `Categoria` (categorías de libros) relacionada con `Prestamo`.

Seguir este orden estricto — cada capa depende de la anterior:

#### Paso 1 — La entidad
```java
// entity/Categoria.java
@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    private String nombre;   // ej: "Novela", "Ciencia", "Historia"

    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Prestamo> prestamos = new ArrayList<>();

    // Constructor vacío obligatorio para JPA
    public Categoria() {}

    public Categoria(String nombre) {
        this.nombre = nombre;
    }

    // Getters, setters y helpers addPrestamo/removePrestamo
}
```

Actualizar `Prestamo.java` para añadir la relación:
```java
// En Prestamo.java — añadir campo y relación
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "categoria_id")   // nullable: una categoria es opcional
@JsonBackReference
private Categoria categoria;

// Getter y setter
public Categoria getCategoria() { return categoria; }
public void setCategoria(Categoria categoria) { this.categoria = categoria; }
```

#### Paso 2 — El repositorio
```java
// repository/CategoriaRepository.java
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    // Spring Data genera findAll, findById, save, deleteById automáticamente
    // Añadir métodos derivados o @Query según necesidad
}
```

#### Paso 3 — El servicio
```java
// service/CategoriaService.java
@Service
@Transactional
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<Categoria> findAll() { return categoriaRepository.findAll(); }

    @Transactional(readOnly = true)
    public Optional<Categoria> findById(Long id) { return categoriaRepository.findById(id); }

    public Categoria save(Categoria categoria) { return categoriaRepository.save(categoria); }

    public void deleteById(Long id) { categoriaRepository.deleteById(id); }
}
```

#### Paso 4 — El controlador web
```java
// controller/web/CategoriaController.java
@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("categorias", categoriaService.findAll());
        return "categorias/list";
    }

    @GetMapping("/new")
    public String mostrarFormCrear(Model model) {
        model.addAttribute("categoria", new Categoria());
        return "categorias/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("categoria") Categoria categoria,
                        BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "categorias/form";
        categoriaService.save(categoria);
        redirectAttributes.addFlashAttribute("successMessage", "Categoría creada correctamente");
        return "redirect:/categorias";
    }

    @GetMapping("/edit/{id}")
    public String mostrarFormEditar(@PathVariable Long id, Model model,
                                    RedirectAttributes redirectAttributes) {
        return categoriaService.findById(id).map(c -> {
            model.addAttribute("categoria", c);
            return "categorias/form";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Categoría no encontrada");
            return "redirect:/categorias";
        });
    }

    @PostMapping("/edit/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("categoria") Categoria categoria,
                             BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "categorias/form";
        categoria.setId(id);
        categoriaService.save(categoria);
        redirectAttributes.addFlashAttribute("successMessage", "Categoría actualizada");
        return "redirect:/categorias";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoriaService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Categoría eliminada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "No se puede eliminar: tiene préstamos asociados");
        }
        return "redirect:/categorias";
    }
}
```

#### Paso 5 — El controlador REST
```java
// controller/api/CategoriaRestController.java
@RestController
@RequestMapping("/api/categorias")
public class CategoriaRestController {

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public List<Categoria> listar() { return categoriaService.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Categoria> obtener(@PathVariable Long id) {
        return categoriaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Categoria> crear(@Valid @RequestBody Categoria categoria) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.save(categoria));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Categoria> actualizar(@PathVariable Long id,
                                                 @Valid @RequestBody Categoria categoria) {
        return categoriaService.findById(id).map(existing -> {
            categoria.setId(id);
            return ResponseEntity.ok(categoriaService.save(categoria));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return categoriaService.findById(id).map(c -> {
            categoriaService.deleteById(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
```

#### Paso 6 — Las vistas
Crear `templates/categorias/list.html` y `templates/categorias/form.html` siguiendo el mismo patrón que las vistas de `alumnos/` (layout con `th:replace`, flash messages, tabla con botones CRUD).

#### Paso 7 — La navbar
Añadir el enlace en `templates/fragments/navbar.html`:
```html
<li class="nav-item">
    <a class="nav-link" th:href="@{/categorias}">Categorías</a>
</li>
```

#### Paso 8 — El formulario de Prestamo
Si se quiere seleccionar la categoría al crear un préstamo, actualizar `PrestamoController`:
```java
// En los métodos mostrarFormCrear y mostrarFormEditar:
model.addAttribute("categorias", categoriaService.findAll());
```

Y en `templates/prestamos/form.html`:
```html
<select name="categoriaId" class="form-select">
    <option value="">-- Sin categoría --</option>
    <option th:each="c : ${categorias}"
            th:value="${c.id}"
            th:text="${c.nombre}"
            th:selected="${prestamo.categoria != null and prestamo.categoria.id == c.id}">
    </option>
</select>
```

Igual que con `alumnoId`, recibir `categoriaId` como `@RequestParam` en el POST y asignarlo manualmente antes de validar.

#### Paso 9 — El DataLoader
Añadir datos de prueba para la nueva entidad:
```java
// En DataLoader.java
@Autowired
private CategoriaRepository categoriaRepository;

// Dentro del método run():
Categoria novela   = categoriaRepository.save(new Categoria("Novela"));
Categoria ciencia  = categoriaRepository.save(new Categoria("Ciencia"));
Categoria historia = categoriaRepository.save(new Categoria("Historia"));

// Luego asignar al crear préstamos:
prestamoRepository.save(new Prestamo("El Quijote", fechaPrestamo, true, fechaDevolucion, alumno));
// y después: prestamo.setCategoria(novela); o pasarlo en el constructor si lo admite
```

---

### 4.E Eliminar una entidad existente

**Ejemplo:** eliminar la entidad `Prestamo` del proyecto.

> ⚠️ Antes de borrar, asegurarse de que ninguna otra entidad tiene FK hacia la que se va a eliminar.

#### Orden de borrado (siempre de hijo a padre)

1. **Vistas:** borrar carpeta `templates/prestamos/`
2. **Navbar:** quitar el enlace `<a th:href="@{/prestamos}">` de `navbar.html`
3. **Controladores:** borrar `PrestamoController.java` y `PrestamoRestController.java`
4. **Servicio:** borrar `PrestamoService.java`
5. **Repositorio:** borrar `PrestamoRepository.java`
6. **Entidad padre:** en `Alumno.java`, eliminar la relación `@OneToMany` y la lista `prestamos`
7. **Entidad:** borrar `Prestamo.java`
8. **DataLoader:** eliminar las líneas que crean préstamos y el `@Autowired PrestamoRepository`
9. **Tests:** borrar `PrestamoTest.java`, `PrestamoRepositoryTest.java`, `PrestamoE2ETest.java`
10. **KPIRestController:** eliminar los endpoints que dependían de `PrestamoService`
11. **BD:** con `ddl-auto=update` Hibernate **no borra tablas automáticamente**. La tabla `prestamos` quedará huérfana en MySQL. Borrarla a mano si molesta:
    ```sql
    DROP TABLE prestamos;
    ```

---

### 4.F Cambiar una relación entre entidades

#### De `@OneToMany` / `@ManyToOne` a `@ManyToMany`

**Ejemplo actual:** un `Alumno` tiene muchos `Prestamo` (1:N).
**Nuevo escenario:** un `Libro` puede tener muchos `Alumno` y un `Alumno` puede tener muchos `Libro` (N:M).

```java
// En Alumno.java — cambiar la relación
@ManyToMany
@JoinTable(
    name = "alumno_libro",                        // tabla intermedia
    joinColumns = @JoinColumn(name = "alumno_id"),
    inverseJoinColumns = @JoinColumn(name = "libro_id")
)
@JsonManagedReference
private List<Libro> libros = new ArrayList<>();

// En Libro.java — lado inverso
@ManyToMany(mappedBy = "libros")
@JsonBackReference
private List<Alumno> alumnos = new ArrayList<>();
```

Con `ddl-auto=update`, Hibernate crea la tabla intermedia `alumno_libro` automáticamente.

---

### Tabla resumen — qué hace `ddl-auto` en cada situación

| Acción en el código | `create-drop` (H2/dev) | `update` (MySQL/prod) | Acción manual necesaria |
|---|---|---|---|
| Añadir campo nuevo | ✅ Auto | ✅ Auto (ADD COLUMN) | No |
| Renombrar campo | ✅ Recrea tabla | ❌ Crea columna nueva vacía | `ALTER TABLE ... RENAME COLUMN` |
| Eliminar campo | ✅ Recrea tabla | ❌ La columna queda en BD | `ALTER TABLE ... DROP COLUMN` |
| Cambiar tipo de dato | ✅ Recrea tabla | ❌ Puede fallar si hay datos | `ALTER TABLE ... MODIFY COLUMN` |
| Añadir tabla nueva | ✅ Auto | ✅ Auto (CREATE TABLE) | No |
| Renombrar tabla (`@Table`) | ✅ Recrea tabla | ❌ Crea tabla nueva vacía | `RENAME TABLE` |
| Eliminar entidad | ✅ Recrea sin ella | ❌ La tabla queda en BD | `DROP TABLE` |
| Añadir relación N:M | ✅ Auto | ✅ Auto (CREATE TABLE intermedia) | No |

> **Regla de oro:** `ddl-auto=update` solo es seguro para **añadir** cosas.
> Para **renombrar o eliminar**, siempre hay que hacer la migración SQL a mano en MySQL.

---

## 5. COMANDOS RÁPIDOS

```bash
# Ejecutar SOLO los tests unitarios
mvn test -Dtest=PrestamoTest

# Ejecutar SOLO los tests de integración
mvn test -Dtest=AlumnoRepositoryTest,PrestamoRepositoryTest

# Ejecutar SOLO los tests E2E
mvn test -Dtest=AlumnoE2ETest,PrestamoE2ETest

# Ejecutar todos los tests
mvn test

# Ver el informe de tests generado por Surefire
# (después de mvn test)
# Abrir: target/surefire-reports/index.html
```
