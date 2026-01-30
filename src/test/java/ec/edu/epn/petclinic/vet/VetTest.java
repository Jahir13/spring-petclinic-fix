package ec.edu.epn.petclinic.owner.model;

import ec.edu.epn.petclinic.vet.Vet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VetTest {

    @Test
    void shouldSetVetLastName() {
        Vet vet = new Vet();
        vet.setLastName("Perez");

        assertEquals("Perez", vet.getLastName());
    }
}
