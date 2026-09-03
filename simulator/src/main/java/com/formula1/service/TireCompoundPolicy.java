package com.formula1.service;

import com.formula1.model.LapResult;
import com.formula1.model.PitStopRecord;
import com.formula1.model.TireCompound;

/** Selecciona el siguiente compuesto sin ejecutar ni representar el cambio. */
@FunctionalInterface
public interface TireCompoundPolicy {

    TireCompound select(TireCompound current, PitStopRecord stop, LapResult context);
}
