package edu.pe.cibertec.taller.servicio;

import edu.pe.cibertec.taller.excepcion.CitaNoCancelableException;
import edu.pe.cibertec.taller.excepcion.EspecialidadIncorrectaException;
import edu.pe.cibertec.taller.excepcion.HorarioNoPermitidoException;
import edu.pe.cibertec.taller.excepcion.MecanicoNoEncontradoException;
import edu.pe.cibertec.taller.modelo.*;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioCitasImplTest {

	@Mock
	private RepositorioMecanicos repositorioMecanicos;

	@Mock
	private RepositorioCitas repositorioCitas;

	@Mock
	private ProveedorFechaHora proveedorFechaHora;

	@Mock
	private ServicioNotificaciones servicioNotificaciones;

	private ServicioCitasImpl servicioCitas;

	private final String PLACA = "JAR-588";
	private final Long ID_MECANICO = 1L;
	private final String NOMBRE_MECANICO = "Jair Jara";

	@BeforeEach
	void inicializar() {
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
		LocalDateTime fecha = LocalDateTime
				.of(2026, 9 , 17, 8, 0);
		lenient().when(proveedorFechaHora.ahora()).thenReturn(fecha);
	}

	@Test
	@DisplayName("Agendar una cita valida la guarda, notifica y la retorna en estado PROGRAMADA")
	void agendarCitaExitosa() {
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 18, 10, 0);

		Mecanico mecanico =
				new Mecanico(ID_MECANICO, NOMBRE_MECANICO, TipoServicio.CAMBIO_ACEITE);

		when(repositorioMecanicos.findById(ID_MECANICO))
				.thenReturn(Optional.of(mecanico));

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Cita resultado = servicioCitas
				.agendarCita(ID_MECANICO, PLACA, TipoServicio.CAMBIO_ACEITE, fechaCita);

		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());

		assertEquals(1, resultado.getDuracionHoras());

		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(any(Cita.class));
	}


	@Test
	@DisplayName("Agendar con un mecanico inexistente lanza MecanicoNoEncontradoException")
	void agendarConMecanicoInexistente() {
		Long idInexistente = 99L;

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 18, 10, 0);

		when(repositorioMecanicos.findById(idInexistente))
				.thenReturn(Optional.empty());

		assertThrows(MecanicoNoEncontradoException.class, () -> {
			servicioCitas.agendarCita(idInexistente, PLACA, TipoServicio.CAMBIO_ACEITE, fechaCita);
		});

		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Test
	@DisplayName("Agendar cuando la especialidad no coincide lanza EspecialidadIncorrectaException")
	void agendarConEspecialidadIncorrecta() {

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 18, 10, 0);

		Mecanico mecanico =
				new Mecanico(ID_MECANICO, NOMBRE_MECANICO, TipoServicio.CAMBIO_ACEITE);

		when(repositorioMecanicos.findById(ID_MECANICO)).thenReturn(Optional.of(mecanico));

		assertThrows(EspecialidadIncorrectaException.class, () -> {
			servicioCitas.agendarCita(ID_MECANICO, PLACA, TipoServicio.REPARACION_MOTOR, fechaCita);
		});

		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Test
	@DisplayName("Un servicio pesado a las 07:00 se rechaza con HorarioNoPermitidoException")
	void agendarServicioPesadoALas07() {
		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 18, 7, 0);

		Mecanico mecanico =
				new Mecanico(ID_MECANICO, NOMBRE_MECANICO, TipoServicio.REPARACION_MOTOR);

		when(repositorioMecanicos.findById(ID_MECANICO)).thenReturn(Optional.of(mecanico));

		assertThrows(HorarioNoPermitidoException.class, () -> {
			servicioCitas.agendarCita(ID_MECANICO, PLACA, TipoServicio.REPARACION_MOTOR, fechaCita);
		});

		verify(repositorioCitas, never()).save(any());
	}

	@Test
	@DisplayName("Un servicio pesado a las 08:00 se acepta y se guarda")
	void agendarServicioPesadoALas08() {

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 18, 8, 0);
		Mecanico mecanico =
				new Mecanico(ID_MECANICO, NOMBRE_MECANICO, TipoServicio.REPARACION_MOTOR);

		when(repositorioMecanicos.findById(ID_MECANICO)).thenReturn(Optional.of(mecanico));

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Cita resultado = servicioCitas.agendarCita(ID_MECANICO, PLACA, TipoServicio.REPARACION_MOTOR, fechaCita);

		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
	}

	@Test
	@DisplayName("Un servicio pesado a las 11:00 se acepta y se guarda")
	void agendarServicioPesadoALas11() {

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 18, 11, 0);

		Mecanico mecanico =
				new Mecanico(ID_MECANICO, NOMBRE_MECANICO, TipoServicio.REPARACION_MOTOR);

		when(repositorioMecanicos.findById(ID_MECANICO)).thenReturn(Optional.of(mecanico));

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Cita resultado = servicioCitas.agendarCita(ID_MECANICO, PLACA, TipoServicio.REPARACION_MOTOR, fechaCita);

		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
	}

	@Test
	@DisplayName("Un servicio pesado a las 12:00 se rechaza con HorarioNoPermitidoException")
	void agendarServicioPesadoALas12() {

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 18, 12, 0);

		Mecanico mecanico =
				new Mecanico(ID_MECANICO, NOMBRE_MECANICO, TipoServicio.REPARACION_MOTOR);

		when(repositorioMecanicos.findById(ID_MECANICO)).thenReturn(Optional.of(mecanico));

		assertThrows(HorarioNoPermitidoException.class, () -> {
			servicioCitas.agendarCita(ID_MECANICO, PLACA, TipoServicio.REPARACION_MOTOR, fechaCita);
		});

		verify(repositorioCitas, never()).save(any());
	}

	@Test
	@DisplayName("Se cancela cuando faltan exactamente 24 horas: penalidad 0.0, estado CANCELADA y se notifica")
	void cancelarConAnticipacionSuficiente() {

		Long idCita = 100L;

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 18, 10, 0);

		LocalDateTime ahora24h =
				LocalDateTime.of(2026, 9, 17, 10, 0);

		when(proveedorFechaHora.ahora()).thenReturn(ahora24h);

		Mecanico mecanico =
				new Mecanico(ID_MECANICO, NOMBRE_MECANICO, TipoServicio.CAMBIO_ACEITE);

		Cita cita =
				new Cita(idCita, mecanico, PLACA, TipoServicio.CAMBIO_ACEITE, fechaCita, 1, EstadoCita.PROGRAMADA);

		when(repositorioCitas.findById(idCita)).thenReturn(Optional.of(cita));

		ResultadoCancelacion resultado = servicioCitas.cancelarCita(idCita);

		assertEquals(0.0, resultado.getMontoPenalidad());
		assertEquals(EstadoCita.CANCELADA, cita.getEstado());
		verify(repositorioCitas, times(1)).save(cita);
		verify(servicioNotificaciones, times(1)).notificarCitaCancelada(cita);
	}

	@Test
	@DisplayName("Se cancela cuando faltan 2 horas: aplica penalidad de 50.0")
	void cancelarConAvisoTardio() {
		Long idCita = 101L;
		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 18, 10, 0);

		LocalDateTime ahora2h =
				LocalDateTime.of(2026, 9, 18, 8, 0);

		when(proveedorFechaHora.ahora()).thenReturn(ahora2h);

		Mecanico mecanico =
				new Mecanico(ID_MECANICO, NOMBRE_MECANICO, TipoServicio.CAMBIO_ACEITE);

		Cita cita =
				new Cita(idCita, mecanico, PLACA, TipoServicio.CAMBIO_ACEITE, fechaCita, 1, EstadoCita.PROGRAMADA);

		when(repositorioCitas.findById(idCita)).thenReturn(Optional.of(cita));

		ResultadoCancelacion resultado = servicioCitas.cancelarCita(idCita);

		assertEquals(50.0, resultado.getMontoPenalidad());
	}

	@Test
	@DisplayName("Intentar cancelar una cita atendida lanza CitaNoCancelableException")
	void cancelarCitaYaAtendida() {

		Long idCita = 102L;

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 18, 10, 0);

		Mecanico mecanico =
				new Mecanico(ID_MECANICO, NOMBRE_MECANICO, TipoServicio.CAMBIO_ACEITE);

		Cita cita =
				new Cita(idCita, mecanico, PLACA, TipoServicio.CAMBIO_ACEITE, fechaCita, 1, EstadoCita.ATENDIDA);

		when(repositorioCitas.findById(idCita)).thenReturn(Optional.of(cita));

		assertThrows(CitaNoCancelableException.class, () -> {
			servicioCitas.cancelarCita(idCita);
		});

		verify(servicioNotificaciones, never()).notificarCitaCancelada(any());
	}

	@Test
	@DisplayName("Buscar mecanico disponible retorna el primero sin citas superpuestas")
	void buscarMecanicoDisponibleRetornaPrimeroLibre() {
		// Arrange
		// TODO: dos mecanicos de la misma especialidad, el primero ocupado

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Buscar mecanico cuando ninguno esta libre lanza SinDisponibilidadException")
	void buscarMecanicoSinDisponibilidad() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}
}
