package com.formula1.util;

import com.formula1.adapter.out.seed.SeedLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba que todos los modelos del catálogo tienen sus imágenes en el
 * classpath. Es el tipo de fallo que no rompe la compilación y solo se ve al
 * abrir la pantalla con un hueco donde debería estar el coche.
 */
class VehicleImagesTest {

    @Test
    void losDiezModelosDelCatalogoTienenFotoPrincipalYAuxiliares() {
        SeedLoader.Seed seed = SeedLoader.cargar();
        assertFalse(seed.getVehiculos().isEmpty(), "El catálogo llegó vacío");

        seed.getVehiculos().forEach(vehiculo -> {
            List<String> vistas = VehicleImages.de(vehiculo.getModelo());
            assertFalse(vistas.isEmpty(),
                    "Sin imágenes para el modelo " + vehiculo.getModelo());
            assertTrue(VehicleImages.tieneGaleria(vehiculo.getModelo()),
                    "El modelo " + vehiculo.getModelo() + " no tiene vistas auxiliares");
        });
    }

    @Test
    void laPrincipalVaLaPrimeraYCoincideConLaDelSeed() {
        SeedLoader.Seed seed = SeedLoader.cargar();
        seed.getVehiculos().forEach(vehiculo -> {
            List<String> vistas = VehicleImages.de(vehiculo.getModelo());
            assertTrue(vistas.get(0).endsWith("/principal.jpg"),
                    "La primera vista de " + vehiculo.getModelo() + " no es la principal");
            // El seed apunta a la misma imagen que abre la galería: si se
            // separasen, la tarjeta y el visor mostrarían coches distintos.
            assertEquals(vistas.get(0), vehiculo.getImagen(),
                    "El seed de " + vehiculo.getModelo() + " apunta a otra imagen");
        });
    }

    @Test
    void unModeloDesconocidoNoDaImagenesNiFalla() {
        assertTrue(VehicleImages.de("NO-EXISTE").isEmpty());
        assertTrue(VehicleImages.de(null).isEmpty());
        assertTrue(VehicleImages.de("   ").isEmpty());
        assertFalse(VehicleImages.tieneGaleria("NO-EXISTE"));
    }

    @Test
    void losSieteCircuitosDelCatalogoTienenSuTrazado() {
        SeedLoader.Seed seed = SeedLoader.cargar();
        assertFalse(seed.getCircuitos().isEmpty());

        seed.getCircuitos().forEach(circuito -> {
            String ruta = circuito.getImagen();
            assertTrue(ruta != null && ruta.startsWith("/images/circuits/"),
                    "El circuito " + circuito.getNombre() + " no apunta a un trazado local: " + ruta);
            // Spa se llamaba «circuito-de-Spa-francorchamps.png» en origen: en
            // Linux, con una mayúscula de más el recurso no aparece.
            assertNotNull(VehicleImagesTest.class.getResource(ruta),
                    "No existe el trazado de " + circuito.getNombre() + " en " + ruta);
        });
    }

    private static void assertNotNull(Object valor, String mensaje) {
        assertTrue(valor != null, mensaje);
    }
}
