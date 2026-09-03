package com.formula1.domain.service;

import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.PitStopRecord;
import com.formula1.domain.model.TireCompound;

/** Selecciona el siguiente compuesto sin ejecutar ni representar el cambio. */
@FunctionalInterface
public interface TireCompoundPolicy {

    TireCompound select(TireCompound current, PitStopRecord stop, LapResult context);
}
