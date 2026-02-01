package ec.edu.epn.petclinic.ControllerTest;

import ec.edu.epn.petclinic.owner.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PetController.class)
class PetControllerTest {

    private static final int TEST_OWNER_ID = 1;
    private static final int TEST_PET_ID = 1;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerRepository owners;

    @MockitoBean
    private PetTypeRepository types;

    private Owner owner;
    private PetType dogType;
    private Pet buddy;

    @BeforeEach
    void setup() {
        dogType = new PetType();
        dogType.setId(1);
        dogType.setName("dog");

        owner = new Owner();
        owner.setId(TEST_OWNER_ID);
        owner.setFirstName("George");

        buddy = new Pet();
        buddy.setId(TEST_PET_ID);
        buddy.setName("Buddy");
        buddy.setType(dogType);

        owner.getPets().add(buddy);
        given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(owner));
        given(this.types.findPetTypes()).willReturn(List.of(dogType));
    }

    @Test
    void testInitCreationForm() throws Exception {
        mockMvc.perform(get("/owners/{ownerId}/pets/new", TEST_OWNER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("pets/createOrUpdatePetForm"))
                .andExpect(model().attributeExists("pet"))
                .andExpect(model().attributeExists("owner"));
    }

    @Test
    void testProcessCreationFormSuccess() throws Exception {
        mockMvc.perform(post("/owners/{ownerId}/pets/new", TEST_OWNER_ID)
                        .param("name", "Rex")
                        .param("birthDate", "2024-01-01")
                        .param("type.id", "1")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/owners/{ownerId}"))
                .andExpect(flash().attribute("message", "New Pet has been Added"));
    }

    @Test
    void testProcessCreationFormHasErrors() throws Exception {
        mockMvc.perform(post("/owners/{ownerId}/pets/new", TEST_OWNER_ID)
                        .param("name", "Buddy") // duplicado
                        .param("birthDate", "2024-01-01")
                        .param("type", "1")
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("pet", "name"))
                .andExpect(view().name("pets/createOrUpdatePetForm"));
    }

    @Test
    void testInitUpdateForm() throws Exception {
        mockMvc.perform(get("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("pet"))
                .andExpect(view().name("pets/createOrUpdatePetForm"));
    }

    @Test
    void testProcessUpdateFormSuccess() throws Exception {
        mockMvc.perform(post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID)
                        .param("id", String.valueOf(TEST_PET_ID))
                        .param("name", "Buddy Updated")
                        .param("birthDate", "2024-01-01")
                        .param("type.id", "1")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/owners/{ownerId}"));
    }

    @Test
    void testProcessUpdateFormBirthDateInFuture() throws Exception {
        String futureDate = LocalDate.now().plusDays(10).toString();

        mockMvc.perform(post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID)
                        .param("id", String.valueOf(TEST_PET_ID))
                        .param("name", "Rex")
                        .param("birthDate", futureDate)
                        .param("type.id", "1")
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("pet", "birthDate"))
                .andExpect(view().name("pets/createOrUpdatePetForm"));
    }
}
