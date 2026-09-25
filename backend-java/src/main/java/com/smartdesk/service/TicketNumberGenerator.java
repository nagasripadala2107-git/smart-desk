package com.smartdesk.service;

import com.smartdesk.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TicketNumberGenerator {

    private final TicketRepository ticketRepository;
    private final AtomicLong fallbackCounter = new AtomicLong(1);

    public TicketNumberGenerator(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public synchronized String generateNextTicketNumber() {
        int currentYear = Year.now().getValue();
        String prefix = "SD-" + currentYear + "-";

        return ticketRepository.findLatestTicketNumberWithPrefix(prefix)
                .map(latest -> {
                    try {
                        String suffix = latest.substring(prefix.length());
                        long seq = Long.parseLong(suffix) + 1;
                        return String.format("%s%06d", prefix, seq);
                    } catch (Exception e) {
                        return String.format("%s%06d", prefix, fallbackCounter.getAndIncrement());
                    }
                })
                .orElseGet(() -> String.format("%s000001", prefix));
    }
}
