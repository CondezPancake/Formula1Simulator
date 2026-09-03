package com.formula1.service;

import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.PitStopDecision;

import java.util.Optional;

/** Política sustituible que decide si el estado actual exige entrar a boxes. */
@FunctionalInterface
public interface PitStopPolicy {

    Optional<PitStopDecision> evaluate(LapResult result, int segment, int totalSegments);
}
