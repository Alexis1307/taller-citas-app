package edu.pe.cibertec.taller.bdd;

import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import edu.pe.cibertec.taller.excepcion.HorarioOcupadoException;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GestionCitasSteps {

	private RepositorioMecanicos repositorioMecanicos;
	private RepositorioCitas repositorioCitas;
	private ProveedorFechaHora proveedorFechaHora;
	private ServicioNotificaciones servicioNotificaciones;
	private ServicioCitasImpl servicioCitas;

	private Cita citaResultado;
	private Exception excepcionLanzada;

	@Before
	public void inicializar() {
		repositorioMecanicos = mock(RepositorioMecanicos.class);
		repositorioCitas = mock(RepositorioCitas.class);
		proveedorFechaHora = mock(ProveedorFechaHora.class);
		servicioNotificaciones = mock(ServicioNotificaciones.class);
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);

		lenient().when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.of(2026, 9, 17, 8, 0));
		lenient().when(repositorioCitas.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));
		citaResultado = null;
		excepcionLanzada = null;
	}

	@Given("que el taller esta operativo un dia antes de las citas")
	public void queElTallerEstaOperativoUnDiaAntesDeLasCitas() {
	}

	@And("existe otro mecanico disponible con id {long} y especialidad {string}")
	public void existeOtroMecanicoDisponibleConIdYEspecialidad(Long id, String especialidadStr) {
		TipoServicio especialidad = TipoServicio.valueOf(especialidadStr);
		Mecanico otroMecanico = new Mecanico(id, "Carlos Perez", especialidad);
		when(repositorioMecanicos.findById(id)).thenReturn(Optional.of(otroMecanico));
		when(repositorioCitas.findByMecanicoIdAndEstado(id, EstadoCita.PROGRAMADA)).thenReturn(Collections.emptyList());
	}

	@When("intento agendar un {string} para la placa {string} con el mecanico id {long} el {string}")
	public void intentoAgendarUnServicioParaLaPlacaConElMecanicoIdEl(String servicioStr, String placa, Long idMecanico, String fechaIso) {
		TipoServicio servicio = TipoServicio.valueOf(servicioStr);
		LocalDateTime fechaHora = LocalDateTime.parse(fechaIso);
		try {
			citaResultado = servicioCitas.agendarCita(idMecanico, placa, servicio, fechaHora);
		} catch (Exception e) {
			excepcionLanzada = e;
		}
	}

	@Then("la cita se agenda exitosamente y se envia la notificacion")
	public void laCitaSeAgendaExitosamenteYSeEnviaLaNotificacion() {
		assertNotNull(citaResultado);
		assertEquals(EstadoCita.PROGRAMADA, citaResultado.getEstado());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(any(Cita.class));
	}

	@Given("que el mecanico con id {long} tiene una cita de 10:00 a 12:00 el {string}")
	public void queElMecanicoConIdTieneUnaCitaDe10a12El(Long id, String fechaStr) {
		Mecanico mecanico = new Mecanico(id, "Jair Jara", TipoServicio.CAMBIO_ACEITE);
		when(repositorioMecanicos.findById(id)).thenReturn(Optional.of(mecanico));

		LocalDateTime inicioOcupado = LocalDateTime.of(2026, 9, 18, 10, 0);
		Cita citaOcupada = new Cita(10L, mecanico, "ABC-123", TipoServicio.MANTENIMIENTO_LIGERO, inicioOcupado, 2, EstadoCita.PROGRAMADA);

		when(repositorioCitas.findByMecanicoIdAndEstado(id, EstadoCita.PROGRAMADA)).thenReturn(List.of(citaOcupada));
	}

	@When("intento agendar un CAMBIO_ACEITE con el mecanico id {long} el {string}")
	public void intentoAgendarUnCambioAceiteConElMecanicoIdEl(Long idMecanico, String fechaIso) {
		LocalDateTime fechaHora = LocalDateTime.parse(fechaIso);
		try {
			citaResultado = servicioCitas.agendarCita(idMecanico, "JAR-588", TipoServicio.CAMBIO_ACEITE, fechaHora);
		} catch (Exception e) {
			excepcionLanzada = e;
		}
	}

	@Then("la solicitud es rechazada por horario ocupado")
	public void laSolicitudEsRechazadaPorHorarioOcupado() {
		assertNotNull(excepcionLanzada);
		assertTrue(excepcionLanzada instanceof HorarioOcupadoException);
		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Then("la cita se agenda exitosamente")
	public void laCitaSeAgendaExitosamente() {
		assertNull(excepcionLanzada);
		assertNotNull(citaResultado);
		assertEquals(EstadoCita.PROGRAMADA, citaResultado.getEstado());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
	}
}