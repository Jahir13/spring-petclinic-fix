package ec.edu.epn.petclinic.ControllerTest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Optional;
import ec.edu.epn.petclinic.owner.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VisitController.class)
class VisitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerRepository owners;

    @BeforeEach
    void setup() {
        Pet pet = new Pet();
        pet.setId(1);
        pet.setName("Buddy");
        Owner owner = new Owner();
        owner.setId(1);
        owner.getPets().add(pet);
        given(this.owners.findById(1)).willReturn(Optional.of(owner));
    }

    @Test
    void testInitNewVisitForm() throws Exception {
        mockMvc.perform(get("/owners/{ownerId}/pets/{petId}/visits/new", 1, 1))
                .andExpect(status().isOk())
                .andExpect(view().name("pets/createOrUpdateVisitForm"))
                .andExpect(model().attributeExists("visit"))
                .andExpect(model().attributeExists("pet"));
    }

    @Test
    void testProcessNewVisitFormSuccess() throws Exception {
        mockMvc.perform(post("/owners/{ownerId}/pets/{petId}/visits/new", 1, 1)
                        .param("description", "Chequeo de rutina")
                        .param("date", "2026-02-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/owners/{ownerId}"));
    }

    @Test
    void testProcessNewVisitFormHasErrors() throws Exception {
        mockMvc.perform(post("/owners/{ownerId}/pets/{petId}/visits/new", 1, 1)
                        .param("description", "")
                        .param("date", "2026-02-01"))
                .andExpect(model().hasErrors())
                .andExpect(view().name("pets/createOrUpdateVisitForm"));
    }
}