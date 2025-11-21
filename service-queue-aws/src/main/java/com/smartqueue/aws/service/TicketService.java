// java
package com.smartqueue.aws.service;

import com.smartqueue.aws.model.Ticket;
import com.smartqueue.aws.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;

    public List<Ticket> getTicketsByUserId(String userId) {
        log.debug("Fetching tickets for userId: {}", userId);
        return ticketRepository.findByUserId(userId);
    }
}
