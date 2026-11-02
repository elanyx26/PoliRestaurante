package com.poli.restaurante.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica por WebSocket solo después de confirmar la transacción: un rollback nunca genera notificaciones.
 * <ul>
 *   <li>/topic/cocina – cocina (y admin): pedidos nuevos, editados o con cambio de estado</li>
 *   <li>/user/queue/pedidos – cada mesero recibe los eventos de sus propios pedidos</li>
 *   <li>/topic/admin/pedidos – panel del administrador</li>
 *   <li>/topic/mesas – meseros (y admin): mesas que se ocupan o se liberan</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class NotificacionesListener {

    private final SimpMessagingTemplate mensajeria;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alCambiarPedido(PedidoEvento evento) {
        mensajeria.convertAndSend("/topic/cocina", evento);
        mensajeria.convertAndSend("/topic/admin/pedidos", evento);
        mensajeria.convertAndSendToUser(String.valueOf(evento.meseroId()), "/queue/pedidos", evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alCambiarMesa(MesaEvento evento) {
        mensajeria.convertAndSend("/topic/mesas", evento);
    }
}
