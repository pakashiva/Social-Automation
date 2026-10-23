package com.shoppingmall.service.impl;

import com.shoppingmall.entity.NumberSequence;
import com.shoppingmall.repository.NumberSequenceRepository;
import com.shoppingmall.service.NumberSequenceService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NumberSequenceServiceImpl implements NumberSequenceService {

    private final NumberSequenceRepository sequences;
    private final Clock clock;
    private final EntityManager entityManager;

    public NumberSequenceServiceImpl(NumberSequenceRepository sequences, Clock clock, EntityManager entityManager) {
        this.sequences = sequences;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public String next(String name) {
        NumberSequence sequence = sequences.findBySequenceName(name).orElseThrow();
        entityManager.refresh(sequence, LockModeType.PESSIMISTIC_WRITE);
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        if ("EMPLOYEE".equals(name)) {
            return sequence.getPrefix() + "-" + String.format(Locale.ROOT, "%06d", value);
        }
        String day = LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE);
        return sequence.getPrefix() + "-" + day + "-" + String.format(Locale.ROOT, "%06d", value);
    }
}
