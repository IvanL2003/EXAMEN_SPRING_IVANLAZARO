package com.salesianos.consultame.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesianos.consultame.entity.Cita;
import com.salesianos.consultame.entity.Doctor;
import com.salesianos.consultame.repository.CitaRepository;
import com.salesianos.consultame.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Tests E2E con MockMvc para Doctores y Citas.
 *
 * CONFIGURACION:
 * - @SpringBootTest: levanta el contexto completo de Spring Boot para pruebas E2E.
 * - @AutoConfigureMockMvc: configura MockMvc para simular peticiones HTTP sin servidor real.
 * - @ActiveProfiles("test"): activa el perfil "test" que usa H2 en memoria (no MySQL).
 *
 * IMPORTANTE: No se utiliza ningun JSON hardcodeado. Todos los objetos se crean
 * programaticamente con setters y se serializan con ObjectMapper.
 * Los tests se ejecutan unicamente en memoria con H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ConsultameE2ETest {

    // MockMvc permite simular peticiones HTTP (GET, POST, PUT, DELETE)
    // sin necesidad de levantar un servidor real
    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper convierte objetos Java a JSON y viceversa
    // Es inyectado por Spring Boot con la configuracion de Jackson
    @Autowired
    private ObjectMapper objectMapper;

    // Repositorios para limpiar la base de datos antes de cada test
    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private CitaRepository citaRepository;

    /**
     * Se ejecuta ANTES de cada test para limpiar la base de datos H2.
     * Esto garantiza que cada test es independiente y no depende de datos
     * creados por otros tests. Se eliminan primero las citas porque
     * tienen FK hacia doctores.
     */
    @BeforeEach
    void limpiarBaseDeDatos() {
        // Primero eliminamos citas (tienen FK a doctores)
        citaRepository.deleteAll();
        // Luego eliminamos doctores (ya no hay FK que los referencie)
        doctorRepository.deleteAll();
    }

    /**
     * TEST A - Flujo completo: crear un doctor con dos citas.
     *
     * Este test verifica el flujo completo de creacion de datos:
     * 1. Se crea UN doctor usando POST /api/doctores
     * 2. Se comprueba que el status HTTP del guardado es 200 (OK)
     * 3. Se crean DOS citas para ese doctor usando POST /api/citas
     * 4. Se comprueba que el status de cada guardado es 200 (OK)
     * 5. Se lista todos los doctores con GET /api/doctores
     * 6. Se verifica que el doctor creado aparece en el listado
     *
     * Los objetos se crean campo a campo con setters para demostrar
     * la construccion programatica sin JSON hardcodeado.
     */
    @Test
    void testA_crearDoctorConDosCitasYVerificarEnListado() throws Exception {

        // =====================================================================
        // PASO 1: Crear un doctor "Dr. Garcia" con todos sus campos
        // Se construye el objeto Doctor campo a campo usando setters
        // y se envia como JSON al endpoint POST /api/doctores
        // =====================================================================
        Doctor doctor = new Doctor();
        doctor.setNombre("Dr. Garcia");
        doctor.setEspecialidad("general");
        doctor.setTelefono("612345678");
        doctor.setEmail("garcia@consultame.com");
        doctor.setConsultorio("Delicias");

        // Se serializa el objeto a JSON con ObjectMapper y se envia por POST
        // Comprobamos que el servidor responde con status 200 (OK)
        MvcResult resultDoctor = mockMvc.perform(post("/api/doctores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctor)))
                // Verificamos que el status del guardado es 200
                .andExpect(status().isOk())
                // Verificamos que la respuesta JSON contiene el nombre correcto
                .andExpect(jsonPath("$.nombre").value("Dr. Garcia"))
                .andReturn();

        // Deserializamos la respuesta JSON para obtener el doctor guardado con su ID
        Doctor doctorGuardado = objectMapper.readValue(
                resultDoctor.getResponse().getContentAsString(), Doctor.class);

        // =====================================================================
        // PASO 2: Crear la primera cita para el doctor
        // Se construye el objeto Cita con un doctor que tiene solo el ID
        // El servicio se encargara de resolver el ID contra la BBDD
        // =====================================================================
        Cita cita1 = new Cita();
        cita1.setPaciente("Juan Perez");
        cita1.setFecha(LocalDate.of(2026, 4, 10));
        cita1.setHora("09:00");
        cita1.setMotivo("Revision general");
        // Creamos una referencia al doctor usando solo su ID
        Doctor refDoctor1 = new Doctor();
        refDoctor1.setId(doctorGuardado.getId());
        cita1.setDoctor(refDoctor1);

        // Enviamos la cita al endpoint POST /api/citas
        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita1)))
                // Verificamos que el status del guardado es 200 (OK)
                .andExpect(status().isOk())
                // Verificamos que el paciente se guardo correctamente
                .andExpect(jsonPath("$.paciente").value("Juan Perez"));

        // =====================================================================
        // PASO 3: Crear la segunda cita para el mismo doctor
        // Se repite el proceso para una segunda cita
        // =====================================================================
        Cita cita2 = new Cita();
        cita2.setPaciente("Maria Lopez");
        cita2.setFecha(LocalDate.of(2026, 4, 11));
        cita2.setHora("10:30");
        cita2.setMotivo("Dolor de cabeza");
        Doctor refDoctor2 = new Doctor();
        refDoctor2.setId(doctorGuardado.getId());
        cita2.setDoctor(refDoctor2);

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita2)))
                // Verificamos que el status del guardado es 200 (OK)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente").value("Maria Lopez"));

        // =====================================================================
        // PASO 4: Listar todos los doctores y verificar que Dr. Garcia existe
        // Se hace GET /api/doctores y se comprueba que el doctor
        // que acabamos de crear aparece en el listado
        // =====================================================================
        mockMvc.perform(get("/api/doctores"))
                // El listado debe devolver status 200
                .andExpect(status().isOk())
                // Debe haber al menos 1 doctor en el listado
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                // El doctor "Dr. Garcia" debe estar presente en la lista
                .andExpect(jsonPath("$[?(@.nombre == 'Dr. Garcia')]").exists());
    }

    /**
     * TEST B - Crear dos doctores "Mark" y "Carry" con una cita cada uno.
     *
     * Este test verifica la creacion de multiples doctores a traves de
     * la capa MVC correspondiente (controlador REST):
     * 1. Se crea el doctor "Mark" y se guarda via POST /api/doctores
     * 2. Se crea el doctor "Carry" y se guarda via POST /api/doctores
     * 3. Se crea una cita para "Mark" via POST /api/citas
     * 4. Se crea una cita para "Carry" via POST /api/citas
     * 5. Se comprueba que ambos doctores existen en el listado
     *
     * Todos los objetos se construyen con setters (sin JSON hardcodeado)
     * y se guardan a traves de la capa MVC del controlador REST.
     */
    @Test
    void testB_crearMarkYCarryConCitaYVerificarExistencia() throws Exception {

        // =====================================================================
        // PASO 1: Crear el doctor "Mark"
        // Se construye con setters y se guarda via capa MVC (POST REST)
        // =====================================================================
        Doctor mark = new Doctor();
        mark.setNombre("Mark");
        mark.setEspecialidad("cirugia");
        mark.setTelefono("699111222");
        mark.setEmail("mark@consultame.com");
        mark.setConsultorio("Sagasta");

        // Guardamos a Mark a traves de la capa MVC y obtenemos el resultado
        MvcResult resMark = mockMvc.perform(post("/api/doctores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mark)))
                // Comprobamos que el guardado devuelve status 200
                .andExpect(status().isOk())
                // Verificamos que el nombre en la respuesta es "Mark"
                .andExpect(jsonPath("$.nombre").value("Mark"))
                .andReturn();

        // Deserializamos para obtener el ID asignado por la BBDD
        Doctor markGuardado = objectMapper.readValue(
                resMark.getResponse().getContentAsString(), Doctor.class);

        // =====================================================================
        // PASO 2: Crear el doctor "Carry"
        // Se construye con setters y se guarda via capa MVC (POST REST)
        // =====================================================================
        Doctor carry = new Doctor();
        carry.setNombre("Carry");
        carry.setEspecialidad("general");
        carry.setTelefono("699333444");
        carry.setEmail("carry@consultame.com");
        carry.setConsultorio("Clinico");

        MvcResult resCarry = mockMvc.perform(post("/api/doctores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(carry)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Carry"))
                .andReturn();

        Doctor carryGuardado = objectMapper.readValue(
                resCarry.getResponse().getContentAsString(), Doctor.class);

        // =====================================================================
        // PASO 3: Crear una cita para Mark
        // Se construye la cita con referencia al doctor Mark por su ID
        // =====================================================================
        Cita citaMark = new Cita();
        citaMark.setPaciente("Paciente de Mark");
        citaMark.setFecha(LocalDate.of(2026, 5, 1));
        citaMark.setHora("08:00");
        citaMark.setMotivo("Consulta preoperatoria");
        Doctor refMark = new Doctor();
        refMark.setId(markGuardado.getId());
        citaMark.setDoctor(refMark);

        // Guardamos la cita a traves de la capa MVC (controlador REST)
        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaMark)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente").value("Paciente de Mark"));

        // =====================================================================
        // PASO 4: Crear una cita para Carry
        // Se construye la cita con referencia al doctor Carry por su ID
        // =====================================================================
        Cita citaCarry = new Cita();
        citaCarry.setPaciente("Paciente de Carry");
        citaCarry.setFecha(LocalDate.of(2026, 5, 2));
        citaCarry.setHora("11:00");
        citaCarry.setMotivo("Revision anual");
        Doctor refCarry = new Doctor();
        refCarry.setId(carryGuardado.getId());
        citaCarry.setDoctor(refCarry);

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaCarry)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente").value("Paciente de Carry"));

        // =====================================================================
        // PASO 5: Listar todos los doctores y comprobar que Mark y Carry existen
        // Se hace GET /api/doctores y se verifica que ambos estan en la lista
        // =====================================================================
        mockMvc.perform(get("/api/doctores"))
                .andExpect(status().isOk())
                // Verificamos que hay exactamente 2 doctores en la BBDD
                .andExpect(jsonPath("$", hasSize(2)))
                // Comprobamos que "Mark" existe en el listado
                .andExpect(jsonPath("$[?(@.nombre == 'Mark')]").exists())
                // Comprobamos que "Carry" existe en el listado
                .andExpect(jsonPath("$[?(@.nombre == 'Carry')]").exists());
    }

    /**
     * TEST C - Modificar los datos de un doctor existente con PUT.
     *
     * Este test verifica la operacion de actualizacion (PUT) de un doctor:
     * 1. Se crea un doctor "Dr. Lopez" via POST /api/doctores
     * 2. Se comprueba que se guarda correctamente con status 200
     * 3. Se modifican sus datos (especialidad y consultorio) via PUT /api/doctores/{id}
     * 4. Se comprueba que la respuesta del PUT es 200 y contiene los datos actualizados
     * 5. Se consulta el doctor por GET /api/doctores/{id} para verificar la persistencia
     *
     * Este test cubre el ejercicio 4 del examen: modificar datos de un doctor.
     */
    @Test
    void testC_modificarDatosDeUnDoctor() throws Exception {

        // =====================================================================
        // PASO 1: Crear el doctor "Dr. Lopez" con datos iniciales
        // Se construye el objeto con setters y se guarda via POST
        // =====================================================================
        Doctor doctor = new Doctor();
        doctor.setNombre("Dr. Lopez");
        doctor.setEspecialidad("general");
        doctor.setTelefono("611222333");
        doctor.setEmail("lopez@consultame.com");
        doctor.setConsultorio("Delicias");

        // Guardamos el doctor y obtenemos su ID generado por la BBDD
        MvcResult resCrear = mockMvc.perform(post("/api/doctores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Dr. Lopez"))
                .andExpect(jsonPath("$.especialidad").value("general"))
                .andReturn();

        // Deserializamos para obtener el ID del doctor creado
        Doctor doctorCreado = objectMapper.readValue(
                resCrear.getResponse().getContentAsString(), Doctor.class);

        // =====================================================================
        // PASO 2: Modificar los datos del doctor con PUT
        // Cambiamos la especialidad a "cirugia" y el consultorio a "Clinico"
        // El resto de campos tambien se envian para mantener la integridad
        // =====================================================================
        Doctor doctorModificado = new Doctor();
        doctorModificado.setNombre("Dr. Lopez");
        doctorModificado.setEspecialidad("cirugia");
        doctorModificado.setTelefono("611222333");
        doctorModificado.setEmail("lopez.cirugia@consultame.com");
        doctorModificado.setConsultorio("Clinico");

        // Enviamos la peticion PUT con el ID del doctor existente
        mockMvc.perform(put("/api/doctores/" + doctorCreado.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctorModificado)))
                // Verificamos que el PUT devuelve status 200 (actualizacion exitosa)
                .andExpect(status().isOk())
                // Comprobamos que los campos se actualizaron correctamente
                .andExpect(jsonPath("$.especialidad").value("cirugia"))
                .andExpect(jsonPath("$.consultorio").value("Clinico"))
                .andExpect(jsonPath("$.email").value("lopez.cirugia@consultame.com"));

        // =====================================================================
        // PASO 3: Verificar que los cambios persisten consultando por GET
        // Hacemos GET /api/doctores/{id} para confirmar que la BBDD
        // tiene los datos actualizados y no los originales
        // =====================================================================
        mockMvc.perform(get("/api/doctores/" + doctorCreado.getId()))
                .andExpect(status().isOk())
                // Verificamos que los datos son los modificados, no los originales
                .andExpect(jsonPath("$.especialidad").value("cirugia"))
                .andExpect(jsonPath("$.consultorio").value("Clinico"));
    }

    /**
     * TEST D - Verificar que el listado de citas devuelve las citas correctas.
     *
     * Este test verifica la consulta de citas a traves de la API REST:
     * 1. Se crea un doctor via POST /api/doctores
     * 2. Se crean tres citas para distintos pacientes via POST /api/citas
     * 3. Se lista todas las citas con GET /api/citas
     * 4. Se verifica que las tres citas existen en el listado
     * 5. Se comprueba que el tamano del listado es exactamente 3
     *
     * Este test cubre los ejercicios 3 y 4 del examen: consultar e insertar citas.
     */
    @Test
    void testD_listarCitasDeUnDoctorConTresPacientes() throws Exception {

        // =====================================================================
        // PASO 1: Crear un doctor "Dra. Martinez" como requisito previo
        // Las citas necesitan un doctor asociado por la relacion FK
        // =====================================================================
        Doctor doctora = new Doctor();
        doctora.setNombre("Dra. Martinez");
        doctora.setEspecialidad("otro");
        doctora.setTelefono("655666777");
        doctora.setEmail("martinez@consultame.com");
        doctora.setConsultorio("Sagasta");

        MvcResult resDoc = mockMvc.perform(post("/api/doctores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctora)))
                .andExpect(status().isOk())
                .andReturn();

        Doctor docGuardada = objectMapper.readValue(
                resDoc.getResponse().getContentAsString(), Doctor.class);

        // =====================================================================
        // PASO 2: Crear tres citas para la doctora con distintos pacientes
        // Cada cita tiene fecha, hora y motivo diferentes
        // =====================================================================
        String[] pacientes = {"Ana Torres", "Pedro Ruiz", "Laura Gomez"};
        String[] horas = {"09:00", "10:30", "12:00"};
        String[] motivos = {"Gripe", "Dolor lumbar", "Revision anual"};

        // Iteramos para crear las tres citas programaticamente
        for (int i = 0; i < 3; i++) {
            Cita cita = new Cita();
            cita.setPaciente(pacientes[i]);
            cita.setFecha(LocalDate.of(2026, 6, 10 + i));
            cita.setHora(horas[i]);
            cita.setMotivo(motivos[i]);
            // Referencia al doctor por ID
            Doctor refDoc = new Doctor();
            refDoc.setId(docGuardada.getId());
            cita.setDoctor(refDoc);

            // Guardamos cada cita y verificamos status 200
            mockMvc.perform(post("/api/citas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cita)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paciente").value(pacientes[i]));
        }

        // =====================================================================
        // PASO 3: Listar todas las citas y verificar que las tres existen
        // GET /api/citas debe devolver exactamente 3 resultados
        // =====================================================================
        mockMvc.perform(get("/api/citas"))
                .andExpect(status().isOk())
                // Verificamos que hay exactamente 3 citas en la BBDD
                .andExpect(jsonPath("$", hasSize(3)))
                // Comprobamos que cada paciente aparece en el listado
                .andExpect(jsonPath("$[?(@.paciente == 'Ana Torres')]").exists())
                .andExpect(jsonPath("$[?(@.paciente == 'Pedro Ruiz')]").exists())
                .andExpect(jsonPath("$[?(@.paciente == 'Laura Gomez')]").exists());
    }

    /**
     * TEST E - Consultar un doctor por ID y verificar que no existe uno inexistente.
     *
     * Este test verifica el endpoint GET /api/doctores/{id}:
     * 1. Se crea un doctor via POST /api/doctores
     * 2. Se consulta ese doctor por su ID con GET /api/doctores/{id}
     * 3. Se verifica que devuelve status 200 y los datos correctos
     * 4. Se consulta un ID inexistente (99999) con GET /api/doctores/99999
     * 5. Se verifica que devuelve status 404 (Not Found)
     *
     * Este test valida tanto el caso positivo (doctor existe)
     * como el negativo (doctor no existe) del endpoint de consulta.
     */
    @Test
    void testE_consultarDoctorPorIdExistenteYNoExistente() throws Exception {

        // =====================================================================
        // PASO 1: Crear un doctor para tener un ID valido en la BBDD
        // =====================================================================
        Doctor doctor = new Doctor();
        doctor.setNombre("Dr. Fernandez");
        doctor.setEspecialidad("cirugia");
        doctor.setTelefono("677888999");
        doctor.setEmail("fernandez@consultame.com");
        doctor.setConsultorio("Clinico");

        MvcResult res = mockMvc.perform(post("/api/doctores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctor)))
                .andExpect(status().isOk())
                .andReturn();

        // Obtenemos el ID real asignado por la BBDD
        Doctor guardado = objectMapper.readValue(
                res.getResponse().getContentAsString(), Doctor.class);

        // =====================================================================
        // PASO 2: Consultar el doctor por su ID real (caso positivo)
        // Debe devolver status 200 y los datos del doctor
        // =====================================================================
        mockMvc.perform(get("/api/doctores/" + guardado.getId()))
                .andExpect(status().isOk())
                // Verificamos que el nombre coincide con el que creamos
                .andExpect(jsonPath("$.nombre").value("Dr. Fernandez"))
                // Verificamos que la especialidad es correcta
                .andExpect(jsonPath("$.especialidad").value("cirugia"))
                // Verificamos que el consultorio es correcto
                .andExpect(jsonPath("$.consultorio").value("Clinico"));

        // =====================================================================
        // PASO 3: Consultar un ID que NO existe en la BBDD (caso negativo)
        // Debe devolver status 404 (Not Found) porque el doctor no existe
        // =====================================================================
        mockMvc.perform(get("/api/doctores/99999"))
                // El servidor debe responder con 404 al no encontrar el doctor
                .andExpect(status().isNotFound());
    }

    /**
     * TEST F - Crear un doctor sin citas y verificar que el listado de citas esta vacio.
     *
     * Este test verifica el comportamiento cuando no hay citas en la BBDD:
     * 1. Se crea un doctor via POST /api/doctores (sin asignarle citas)
     * 2. Se lista todas las citas con GET /api/citas
     * 3. Se verifica que el listado esta vacio (tamano 0)
     * 4. Se lista los doctores para confirmar que el doctor si existe
     *
     * Este test valida el caso limite de un listado vacio y que la creacion
     * de un doctor no genera citas automaticamente.
     */
    @Test
    void testF_doctorSinCitasListadoCitasVacio() throws Exception {

        // =====================================================================
        // PASO 1: Crear un doctor sin asignarle ninguna cita
        // Solo creamos el doctor, no creamos citas
        // =====================================================================
        Doctor doctor = new Doctor();
        doctor.setNombre("Dr. Solitario");
        doctor.setEspecialidad("general");
        doctor.setTelefono("600111222");
        doctor.setEmail("solitario@consultame.com");
        doctor.setConsultorio("Delicias");

        mockMvc.perform(post("/api/doctores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctor)))
                // Verificamos que el doctor se guardo correctamente
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Dr. Solitario"));

        // =====================================================================
        // PASO 2: Listar todas las citas y verificar que esta vacio
        // No hemos creado ninguna cita, asi que el listado debe estar vacio
        // =====================================================================
        mockMvc.perform(get("/api/citas"))
                .andExpect(status().isOk())
                // El listado de citas debe tener tamano 0
                .andExpect(jsonPath("$", hasSize(0)));

        // =====================================================================
        // PASO 3: Verificar que el doctor si existe en su propio listado
        // Confirmamos que el doctor se creo pero no tiene citas
        // =====================================================================
        mockMvc.perform(get("/api/doctores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Dr. Solitario"));
    }
}
