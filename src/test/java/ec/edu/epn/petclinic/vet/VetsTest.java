package ec.edu.epn.petclinic.owner;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VetsTest {

    @Test
    void shouldReturnCorrectVetListSize() {
        Vet vet = new Vet();
        vet.setFirstName("Juan");

        Vets vets = new Vets();
        vets.getVetList().add(vet);

        List<Vet> result = vets.getVetList();

        assertEquals(1, result.size());
    }
}
