package com.poli.restaurante.realtime;

import com.poli.restaurante.domain.EstadoMesa;
import java.time.Instant;

public record MesaEvento(Long mesaId, Integer numero, EstadoMesa estado, Instant fecha) {
}
