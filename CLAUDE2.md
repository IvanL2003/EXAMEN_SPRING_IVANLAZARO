# Plan de Desarrollo: BiblioBox - Sistema de Gestión de Biblioteca

## Información del Proyecto

**Nombre:** BiblioBox  
**Descripción:** Aplicación de gestión de préstamos de biblioteca escolar  
**Stack Tecnológico:**
- Java 21
- Spring Boot 4.0.0
- Maven
- H2 Database (desarrollo)
- Thymeleaf
- Bootstrap 5.3.8
- Chart.js
- JUnit 5

---

## 1. CONFIGURACIÓN INICIAL

### 1.1 Crear proyecto Spring Boot
- **Group:** com.salesianos
- **Artifact:** bibliobox
- **Packaging:** War
- **Dependencies:**
  - Spring Web
  - Spring Data JPA
  - H2 Database
  - Thymeleaf
  - Validation
  - Spring Boot DevTools

### 1.2 Configurar `application.properties`
```properties
server.port=8080

# H2 Database
spring.datasource.url=jdbc:h2:mem:biblioboxdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Thymeleaf
spring.thymeleaf.cache=false

# Logging
logging.level.org.springframework.web=DEBUG
logging.level.com.salesianos.bibliobox=DEBUG
```

---

## 2. ESTRUCTURA DE PAQUETES

Crear la siguiente estructura en `src/main/java/com/salesianos/bibliobox/`:

```
com.salesianos.bibliobox
├── config
├── controller
│   ├── web
│   └── api
├── entity
├── repository
├── service
└── BiblioBoxApplication.java
```

---

## 3. ENTIDADES (CAPA MODEL)

### 3.1 Entidad Alumno
**Ubicación:** `entity/Alumno.java`

**Requisitos:**
- `@Entity` con nombre de tabla "alumnos"
- Campos:
  - `id`: Long (PK, auto-generado)
  - `nombre`: String (obligatorio, mínimo 2 caracteres)
  - `curso`: String (obligatorio, ej: "1ºESO-A")
  - `email`: String (opcional pero validable)
- Relación `@OneToMany` con Prestamo
- Métodos helper para gestión bidireccional de préstamos

### 3.2 Entidad Préstamo
**Ubicación:** `entity/Prestamo.java`

**Requisitos:**
- `@Entity` con nombre de tabla "prestamos"
- Campos:
  - `id`: Long (PK, auto-generado)
  - `tituloLibro`: String (obligatorio)
  - `fechaPrestamo`: LocalDate (obligatorio)
  - `devuelto`: boolean (default: false)
  - `fechaDevolucion`: LocalDate (nullable)
  - `alumno`: Alumno (relación @ManyToOne, obligatorio)
- **Validaciones personalizadas:**
  - Si `devuelto=true` → `fechaDevolucion` es obligatoria
  - `fechaDevolucion` >= `fechaPrestamo`

---

## 4. REPOSITORIOS (CAPA DATA)

### 4.1 AlumnoRepository
**Ubicación:** `repository/AlumnoRepository.java`

**Métodos requeridos:**
- Heredar de `JpaRepository<Alumno, Long>`
- `findByCurso(String curso)`: List<Alumno>
- `@Query` para agrupar alumnos por curso (KPI)
- `@Query` para top 5 alumnos con más préstamos

### 4.2 PrestamoRepository
**Ubicación:** `repository/PrestamoRepository.java`

**Métodos requeridos:**
- Heredar de `JpaRepository<Prestamo, Long>`
- `findByAlumnoId(Long alumnoId)`: List<Prestamo>
- `findByDevueltoFalse()`: List<Prestamo>
- `@Query` para contar préstamos por estado
- `@Query` para agrupar préstamos por mes

---

## 5. SERVICIOS (CAPA BUSINESS)

### 5.1 AlumnoService
**Ubicación:** `service/AlumnoService.java`

**Métodos:**
- `findAll()`: List<Alumno>
- `findById(Long id)`: Optional<Alumno>
- `save(Alumno alumno)`: Alumno
- `deleteById(Long id)`: void
- `getAlumnosPorCurso()`: List<Map<String, Object>>
- `getTop5AlumnosConMasPrestamos()`: List<Map<String, Object>>

### 5.2 PrestamoService
**Ubicación:** `service/PrestamoService.java`

**Métodos:**
- `findAll()`: List<Prestamo>
- `findById(Long id)`: Optional<Prestamo>
- `save(Prestamo prestamo)`: Prestamo
- `deleteById(Long id)`: void
- `getPrestamosPorEstado()`: List<Map<String, Object>>
- `getPrestamosPorMes()`: List<Map<String, Object>>

---

## 6. CONTROLADORES WEB (MVC)

### 6.1 HomeController
**Ubicación:** `controller/web/HomeController.java`
- `GET /` → redirect a `/alumnos`

### 6.2 AlumnoController
**Ubicación:** `controller/web/AlumnoController.java`

**Endpoints:**
- `GET /alumnos` → listar todos
- `GET /alumnos/new` → formulario crear
- `POST /alumnos` → crear alumno
- `GET /alumnos/edit/{id}` → formulario editar
- `POST /alumnos/edit/{id}` → actualizar alumno
- `GET /alumnos/delete/{id}` → eliminar alumno

### 6.3 PrestamoController
**Ubicación:** `controller/web/PrestamoController.java`

**Endpoints:**
- `GET /prestamos` → listar todos
- `GET /prestamos/new` → formulario crear (incluye selector de alumno)
- `POST /prestamos` → crear préstamo
- `GET /prestamos/edit/{id}` → formulario editar
- `POST /prestamos/edit/{id}` → actualizar préstamo
- `GET /prestamos/delete/{id}` → eliminar préstamo

### 6.4 KPIController
**Ubicación:** `controller/web/KPIController.java`
- `GET /kpi` → mostrar dashboard con gráficos

---

## 7. API REST CONTROLLERS

### 7.1 AlumnoRestController
**Ubicación:** `controller/api/AlumnoRestController.java`

**Endpoints:**
- `GET /api/alumnos` → listar todos
- `GET /api/alumnos/{id}` → obtener por ID
- `POST /api/alumnos` → crear
- `PUT /api/alumnos/{id}` → actualizar
- `DELETE /api/alumnos/{id}` → eliminar

### 7.2 PrestamoRestController
**Ubicación:** `controller/api/PrestamoRestController.java`

**Endpoints:**
- `GET /api/prestamos` → listar todos
- `GET /api/prestamos/{id}` → obtener por ID
- `POST /api/prestamos` → crear
- `PUT /api/prestamos/{id}` → actualizar
- `DELETE /api/prestamos/{id}` → eliminar

### 7.3 KPIRestController
**Ubicación:** `controller/api/KPIRestController.java`

**Endpoints:**
- `GET /api/kpi/alumnos-por-curso`
- `GET /api/kpi/prestamos-por-estado`
- `GET /api/kpi/prestamos-por-mes`
- `GET /api/kpi/top-alumnos`

---

## 8. VISTAS THYMELEAF

### 8.1 Layout Base
**Ubicación:** `templates/layout.html`

**Requisitos:**
- Navbar con Bootstrap 5.3.8
- Links a: Alumnos, Préstamos, KPI Dashboard
- Sistema de mensajes flash (success/error)
- Bootstrap Icons

### 8.2 Vistas de Alumnos
**Ubicación:** `templates/alumnos/`

**Archivos:**
- `list.html`: tabla con listado, botones editar/eliminar
- `form.html`: formulario crear/editar con validaciones

### 8.3 Vistas de Préstamos
**Ubicación:** `templates/prestamos/`

**Archivos:**
- `list.html`: tabla con listado, badges de estado
- `form.html`: formulario con selector de alumno, campos de fechas

### 8.4 Vista KPI Dashboard
**Ubicación:** `templates/kpi/dashboard.html`

**Requisitos:**
- 4 gráficos con Chart.js:
  1. **Alumnos por curso** (bar chart)
  2. **Préstamos por estado** (pie chart)
  3. **Préstamos por mes** (bar chart)
  4. **Top 5 alumnos** (radar chart)
- Carga de datos vía AJAX desde endpoints API

---

## 9. TESTS

### 9.1 Tests Unitarios (2)
**Ubicación:** `src/test/java/com/salesianos/bibliobox/entity/PrestamoTest.java`

**Tests requeridos:**
- `UT1_devueltoTrueExigeFechaDevolucion`: validar que si devuelto=true, fechaDevolucion es obligatoria
- `UT2_fechaDevolucionDebeSerMayorOIgualFechaPrestamo`: validar regla de fechas

### 9.2 Tests de Integración (2)
**Ubicación:** `src/test/java/com/salesianos/bibliobox/repository/`

**Tests requeridos:**
- `IT1_repositorioAlumnosYAgrupacionPorCurso` (AlumnoRepositoryTest.java)
- `IT2_conteoDevueltoNoDevuelto` (PrestamoRepositoryTest.java)

### 9.3 Tests E2E (2)
**Ubicación:** `src/test/java/com/salesianos/bibliobox/e2e/`

**Tests requeridos:**
- `E2E1_flujoCreacionAlumnoCompleto` (AlumnoE2ETest.java): flujo completo de crear alumno
- `E2E2_flujoPrestamoCasoInvalido` (PrestamoE2ETest.java): intento de crear préstamo inválido (devuelto=true sin fecha) → debe retornar 400

---

## 10. CI/CD - GITHUB ACTIONS

### 10.1 Crear estructura
```
.github/
└── workflows/
    ├── ci-test.yml
    └── cd-deploy.yml
```

### 10.2 CI Test Workflow
**Archivo:** `.github/workflows/ci-test.yml`

**Requisitos:**
- Trigger en push/PR a main y develop
- Checkout código
- Setup JDK 21
- Compilar proyecto (`mvn clean compile`)
- Ejecutar tests (`mvn test`)
- Generar reporte cobertura (opcional)
- Publicar resultados tests
- **IMPORTANTE:** Incluir comentarios explicando cada paso

### 10.3 CD Deploy Workflow
**Archivo:** `.github/workflows/cd-deploy.yml`

**Requisitos:**
- Trigger: manual (`workflow_dispatch`) y en PR merged a main
- Checkout código
- Setup JDK 21
- Ejecutar tests
- Empaquetar en WAR (`mvn clean package -DskipTests`)
- Verificar generación de WAR
- Subir artefacto WAR
- Crear release (solo en ejecución manual en producción)
- **IMPORTANTE:** Incluir comentarios explicando cada paso

---

## 11. DATOS DE PRUEBA (OPCIONAL)

### DataLoader
**Ubicación:** `config/DataLoader.java`

**Requisitos:**
- Implementar `CommandLineRunner`
- Crear 3-5 alumnos de prueba con diferentes cursos
- Crear 5-10 préstamos de prueba (algunos devueltos, otros pendientes)
- Distribuir préstamos en diferentes meses para KPI

---

## 12. CHECKLIST DE VALIDACIÓN

### Funcionalidad
- [ ] CRUD completo de Alumnos funciona
- [ ] CRUD completo de Préstamos funciona
- [ ] Selector de alumno funciona en formulario de préstamo
- [ ] Validaciones de formulario funcionan
- [ ] Mensajes flash se muestran correctamente
- [ ] Navegación entre páginas funciona

### API REST
- [ ] Todos los endpoints de alumnos responden correctamente
- [ ] Todos los endpoints de préstamos responden correctamente
- [ ] Endpoints KPI devuelven datos en formato correcto

### KPI Dashboard
- [ ] Gráfico 1 (Alumnos por curso) se muestra
- [ ] Gráfico 2 (Préstamos por estado) se muestra
- [ ] Gráfico 3 (Préstamos por mes) se muestra
- [ ] Gráfico 4 (Top alumnos) se muestra
- [ ] Datos se cargan correctamente vía AJAX

### Tests
- [ ] UT1 pasa correctamente
- [ ] UT2 pasa correctamente
- [ ] IT1 pasa correctamente
- [ ] IT2 pasa correctamente
- [ ] E2E1 pasa correctamente
- [ ] E2E2 pasa correctamente

### CI/CD
- [ ] Workflow CI se ejecuta en push
- [ ] Workflow CI ejecuta todos los tests
- [ ] Workflow CD genera archivo WAR
- [ ] Workflows tienen comentarios explicativos
- [ ] Artefactos se suben correctamente

---

## 13. COMANDOS ÚTILES

### Maven
```bash
# Compilar
mvn clean compile

# Ejecutar tests
mvn test

# Empaquetar en WAR
mvn clean package

# Ejecutar aplicación
mvn spring-boot:run
```

### Acceso a la aplicación
- **Web:** http://localhost:8080
- **H2 Console:** http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:biblioboxdb`
  - User: `sa`
  - Password: (vacío)

---

## 14. ORDEN DE IMPLEMENTACIÓN RECOMENDADO

1. **Configuración inicial y entidades**
   - Configurar application.properties
   - Crear entidades Alumno y Prestamo con validaciones

2. **Capa de datos**
   - Crear repositorios con queries personalizados
   - Crear servicios

3. **Controladores Web (MVC)**
   - Crear layout base
   - Implementar CRUD de Alumnos (controlador + vistas)
   - Implementar CRUD de Préstamos (controlador + vistas)

4. **API REST**
   - Implementar controladores REST
   - Implementar endpoints KPI

5. **KPI Dashboard**
   - Crear vista con contenedores para gráficos
   - Implementar JavaScript para cargar datos
   - Configurar Chart.js para cada gráfico

6. **Tests**
   - Implementar tests unitarios
   - Implementar tests de integración
   - Implementar tests E2E

7. **CI/CD**
   - Crear workflows de GitHub Actions
   - Probar workflows

8. **DataLoader (opcional)**
   - Implementar carga de datos de prueba

---

## 15. NOTAS IMPORTANTES

### Validaciones de Préstamo
- La validación `devuelto=true` → `fechaDevolucion obligatoria` debe implementarse con `@AssertTrue` en la entidad
- La validación `fechaDevolucion >= fechaPrestamo` también con `@AssertTrue`

### Bootstrap y Chart.js
- Bootstrap 5.3.8: https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css
- Bootstrap Icons: https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css
- Chart.js 4.4.0: https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js

### Relaciones JPA
- `@ManyToOne` en Prestamo hacia Alumno con `fetch = FetchType.LAZY`
- `@OneToMany` en Alumno hacia Prestamo con `cascade = CascadeType.ALL, orphanRemoval = true`
- Métodos helper para mantener sincronización bidireccional

### Tests E2E
- Usar `@SpringBootTest` y `@AutoConfigureMockMvc`
- Usar `@Transactional` para rollback automático
- Verificar tanto respuestas HTTP como estado de la base de datos

---

## 16. RECURSOS DE REFERENCIA

### Documentación
- Spring Boot 4.0.0: https://docs.spring.io/spring-boot/docs/4.0.0/reference/html/
- Thymeleaf: https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html
- Chart.js: https://www.chartjs.org/docs/latest/

### Validaciones Jakarta
- `@NotBlank`: campo no puede ser null ni vacío
- `@NotNull`: campo no puede ser null
- `@Size`: longitud del String
- `@Email`: formato de email válido
- `@AssertTrue`: validación personalizada (retorna boolean)

### Anotaciones Spring
- `@Entity`: marca clase como entidad JPA
- `@Repository`: marca interfaz como repositorio
- `@Service`: marca clase como servicio
- `@Controller`: controlador MVC
- `@RestController`: controlador REST
- `@Transactional`: gestión de transacciones

---

**Fin del Plan de Desarrollo**
