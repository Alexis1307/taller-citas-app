Feature: Gestion de citas del taller mecanico

  Scenario: Registro exitoso de mantenimiento ligero con otro mecanico
    Given que el taller esta operativo un dia antes de las citas
    And existe otro mecanico disponible con id 2 y especialidad "MANTENIMIENTO_LIGERO"
    When intento agendar un "MANTENIMIENTO_LIGERO" para la placa "JAR-588" con el mecanico id 2 el "2026-09-18T10:00"
    Then la cita se agenda exitosamente y se envia la notificacion

  Scenario: Intento de agendar con mecanico ocupado iniciando a las 11:00
    Given que el mecanico con id 1 tiene una cita de 10:00 a 12:00 el "2026-09-18"
    When intento agendar un CAMBIO_ACEITE con el mecanico id 1 el "2026-09-18T11:00"
    Then la solicitud es rechazada por horario ocupado

  Scenario: Intento de agendar con mecanico ocupado iniciando a las 12:00
    Given que el mecanico con id 1 tiene una cita de 10:00 a 12:00 el "2026-09-18"
    When intento agendar un CAMBIO_ACEITE con el mecanico id 1 el "2026-09-18T12:00"
    Then la cita se agenda exitosamente