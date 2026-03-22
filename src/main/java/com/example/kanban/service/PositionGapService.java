package com.example.kanban.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PositionGapService {

    private static final BigDecimal POSITION_GAP = new BigDecimal("100.00");
    private static final BigDecimal FLOOR = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    public BigDecimal nextPosition(BigDecimal maxPosition) {
        if (maxPosition == null) {
            return POSITION_GAP;
        }
        return maxPosition.add(POSITION_GAP).setScale(2, RoundingMode.UNNECESSARY);
    }

    public Optional<BigDecimal> positionBetween(BigDecimal previousPosition, BigDecimal nextPosition) {
        BigDecimal lower = previousPosition == null ? FLOOR : previousPosition;
        BigDecimal upper = nextPosition == null ? null : nextPosition;

        if (upper == null) {
            return Optional.of(nextPosition(lower));
        }

        BigDecimal midpoint = lower.add(upper.subtract(lower).divide(new BigDecimal("2"), 2, RoundingMode.DOWN));
        if (midpoint.compareTo(lower) <= 0 || midpoint.compareTo(upper) >= 0) {
            return Optional.empty();
        }
        return Optional.of(midpoint);
    }

    public BigDecimal rebalancePosition(int index) {
        return POSITION_GAP.multiply(BigDecimal.valueOf(index + 1L)).setScale(2, RoundingMode.UNNECESSARY);
    }
}
