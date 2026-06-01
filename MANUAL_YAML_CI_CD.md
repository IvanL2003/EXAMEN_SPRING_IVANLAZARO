# Manual: Como crear archivos YAML de CI/CD para GitHub Actions

## 1. Donde se colocan los archivos

Los archivos YAML **deben** estar en la carpeta `.github/workflows/` dentro de la raiz del proyecto.
Si no existe la carpeta, hay que crearla:

```
mi-proyecto/
├── .github/
│   └── workflows/
│       ├── ci-test.yml       <-- Workflow de CI
│       └── cd-release.yml    <-- Workflow de CD
├── src/
├── pom.xml
└── ...
```

> **IMPORTANTE:** GitHub solo detecta los workflows que estan en `.github/workflows/`. Si los pones en otro sitio, no se ejecutaran.

---

## 2. Estructura basica de un YAML de GitHub Actions

Todo workflow tiene 3 secciones principales:

```yaml
# 1) NOMBRE: identifica el workflow en la pestana Actions de GitHub
name: Nombre del Workflow

# 2) ON (TRIGGER): cuando se ejecuta
on:
  push:
    branches:
      - main

# 3) JOBS: las tareas que ejecuta
jobs:
  nombre-del-job:
    runs-on: ubuntu-latest   # Sistema operativo del runner
    steps:
      - name: Paso 1
        run: echo "Hola mundo"
```

---

## 3. Triggers (on) - Cuando se activa un workflow

### 3.1 Al hacer push a una rama

```yaml
on:
  push:
    branches:
      - main        # Solo cuando se hace push a main
      - dev         # O a dev
```

### 3.2 Al crear un tag

```yaml
on:
  push:
    tags:
      - 'v*'        # Cualquier tag que empiece por "v" (v1.0.0, v2.3.1, etc.)
      - 'v*.*.*'    # Formato mas estricto: v1.0.0
```

### 3.3 Al completarse otro workflow

```yaml
on:
  workflow_run:
    workflows: ["CI - Tests"]    # Nombre exacto del otro workflow
    types:
      - completed                # Se activa cuando termina (exitoso o no)
```

### 3.4 Pull request

```yaml
on:
  pull_request:
    branches:
      - main
```

---

## 4. Jobs y Steps - Que hace el workflow

### 4.1 Definir el sistema operativo (runs-on)

```yaml
jobs:
  mi-job:
    runs-on: ubuntu-latest      # Linux
    # runs-on: windows-latest   # Windows
    # runs-on: macos-latest     # macOS
```

### 4.2 Ejecutar en multiples SO con matrix

```yaml
jobs:
  test:
    strategy:
      matrix:
        os: [windows-latest, macos-latest]    # Se ejecuta en AMBOS
    runs-on: ${{ matrix.os }}                 # Usa el valor de la matriz
```

Esto crea **dos ejecuciones paralelas**, una en Windows y otra en macOS.

### 4.3 Steps comunes para proyectos Java/Maven

#### Checkout del codigo (SIEMPRE el primer paso)

```yaml
steps:
  - name: Checkout del codigo
    uses: actions/checkout@v4
```

#### Configurar Java

```yaml
  - name: Configurar JDK 21
    uses: actions/setup-java@v4
    with:
      java-version: '21'
      distribution: 'temurin'
```

#### Comandos Maven

```yaml
  # Limpiar la carpeta target
  - name: Limpiar
    run: mvn clean

  # Ejecutar tests
  - name: Tests
    run: mvn test

  # Empaquetar (generar JAR/WAR) sin ejecutar tests
  - name: Empaquetar
    run: mvn package -DskipTests

  # Limpiar + tests en un solo comando
  - name: Clean y test
    run: mvn clean test
```

---

## 5. Ejemplo completo: CI (Integracion Continua)

Este workflow se activa cuando alguien sube codigo a la rama `dev`, limpia el proyecto y ejecuta los tests en Windows y macOS:

```yaml
# Descripcion: Workflow de CI que valida el codigo al subirlo a dev
name: CI - Tests

# Trigger: se activa al hacer push a la rama dev
on:
  push:
    branches:
      - dev

jobs:
  test:
    # Matrix: ejecuta en Windows y macOS en paralelo
    strategy:
      matrix:
        os: [windows-latest, macos-latest]

    runs-on: ${{ matrix.os }}

    steps:
      # 1. Descargar el codigo del repositorio
      - name: Checkout del codigo
        uses: actions/checkout@v4

      # 2. Instalar Java 21
      - name: Configurar JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      # 3. Limpiar carpeta target (elimina artefactos antiguos)
      - name: Limpiar carpeta target
        run: mvn clean

      # 4. Ejecutar tests (compila y lanza los tests con H2 en memoria)
      - name: Ejecutar tests
        run: mvn test
```

---

## 6. Ejemplo completo: CD (Entrega Continua / Release)

Este workflow se activa tras el CI o al crear un tag, y genera una Release en GitHub con el JAR:

```yaml
# Descripcion: Genera una release cuando se crea un tag de version
name: CD - Release

# Triggers:
# 1. Cuando el workflow de CI termina correctamente
# 2. Cuando se hace push de un tag (ej: v1.0.0)
on:
  workflow_run:
    workflows: ["CI - Tests"]
    types:
      - completed

  push:
    tags:
      - 'v*'

jobs:
  release:
    # Condicion: solo ejecutar si CI fue exitoso o si es un push de tag
    if: ${{ github.event_name == 'push' || github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest

    # Permisos: necesarios para crear releases y subir archivos
    permissions:
      contents: write

    steps:
      # 1. Descargar el codigo
      - name: Checkout del codigo
        uses: actions/checkout@v4

      # 2. Instalar Java 21
      - name: Configurar JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      # 3. Generar el JAR (sin tests, ya pasaron en CI)
      - name: Empaquetar proyecto
        run: mvn package -DskipTests

      # 4. Crear la release en GitHub con el JAR adjunto
      - name: Crear Release en GitHub
        uses: softprops/action-gh-release@v1
        if: startsWith(github.ref, 'refs/tags/')
        with:
          files: target/*.jar
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 7. Como crear un tag y disparar la release

Desde la terminal:

```bash
# Crear el tag localmente
git tag v1.0.0

# Subir el tag al remoto (esto dispara el workflow de CD)
git push origin v1.0.0
```

---

## 8. Variables importantes de GitHub Actions

| Variable | Que contiene | Ejemplo |
|---|---|---|
| `${{ github.ref }}` | Referencia completa (rama o tag) | `refs/tags/v1.0.0` |
| `${{ github.ref_name }}` | Solo el nombre | `v1.0.0` |
| `${{ github.event_name }}` | Tipo de evento que disparo el workflow | `push` |
| `${{ secrets.GITHUB_TOKEN }}` | Token automatico para operar con la API de GitHub | (generado por GitHub) |
| `${{ matrix.os }}` | Valor actual de la matriz | `windows-latest` |

---

## 9. Resumen rapido

| Concepto | CI (Integracion Continua) | CD (Entrega Continua) |
|---|---|---|
| **Objetivo** | Validar que el codigo compila y los tests pasan | Generar una release descargable |
| **Trigger** | Push a una rama (ej: `dev`) | Push de un tag (ej: `v1.0.0`) |
| **Comandos** | `mvn clean` + `mvn test` | `mvn package -DskipTests` |
| **Resultado** | Tests OK / Tests FAIL | Release en GitHub con JAR/WAR |

---

## 10. Como generar un WAR en vez de un JAR

Por defecto, Spring Boot genera un **JAR** ejecutable. Si necesitas desplegar en un servidor externo (Tomcat, WildFly, etc.), necesitas un **WAR**.

### 10.1 Paso 1: Cambiar el packaging en pom.xml

```xml
<packaging>war</packaging>
```

### 10.2 Paso 2: Marcar el Tomcat embebido como "provided"

Esto evita que el Tomcat de Spring Boot se empaquete dentro del WAR, ya que el servidor externo aporta el suyo:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
```

### 10.3 Paso 3: Extender SpringBootServletInitializer

Tu clase principal debe extender `SpringBootServletInitializer` para que el servidor externo pueda arrancar la aplicacion:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class MiAplicacion extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MiAplicacion.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(MiAplicacion.class, args);
    }
}
```

### 10.4 Paso 4: Generar el WAR

```bash
mvn clean package -DskipTests
```

El archivo `.war` se genera en la carpeta `target/`.

### 10.5 Adaptar el YAML de CD para WAR

Si tu workflow de CD generaba una release con JAR, cambia el patron de archivos:

```yaml
      - name: Crear Release en GitHub
        uses: softprops/action-gh-release@v1
        if: startsWith(github.ref, 'refs/tags/')
        with:
          files: target/*.war    # <-- Cambiado de *.jar a *.war
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### 10.6 Diferencias JAR vs WAR

| Aspecto | JAR | WAR |
|---|---|---|
| **Packaging en pom.xml** | `<packaging>jar</packaging>` | `<packaging>war</packaging>` |
| **Tomcat** | Embebido (incluido dentro) | Proporcionado por el servidor externo |
| **Ejecucion** | `java -jar app.jar` | Copiar a `webapps/` del servidor |
| **SpringBootServletInitializer** | No necesario | Obligatorio |
| **Dependencia Tomcat** | Scope por defecto (compile) | `<scope>provided</scope>` |

> **NOTA:** Un WAR generado con Spring Boot tambien se puede ejecutar con `java -jar app.war` si mantienes el `main()`. Es decir, funciona en ambos modos.

---

## 11. Errores comunes

1. **El YAML no se detecta**: Asegurate de que esta en `.github/workflows/`, no en otra carpeta.
2. **Error de indentacion**: YAML usa espacios (NO tabs). Cada nivel son 2 espacios.
3. **El nombre del workflow no coincide**: En `workflow_run.workflows`, el nombre debe ser **exactamente** igual al `name:` del otro workflow.
4. **Falta permisos**: Para crear releases necesitas `permissions: contents: write`.
5. **El tag no dispara el workflow**: Asegurate de hacer `git push origin <tag>`, no solo `git tag`.
6. **Release con JAR cuando deberia ser WAR**: Revisa que el patron en `files:` coincide con tu packaging (`target/*.jar` o `target/*.war`).
