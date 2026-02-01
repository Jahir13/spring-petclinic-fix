package ec.edu.epn.petclinic.ControllerTest;

import ec.edu.epn.petclinic.owner.Owner;
import ec.edu.epn.petclinic.owner.OwnerController;
import ec.edu.epn.petclinic.owner.OwnerRepository;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OwnerController.class)
class OwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerRepository owners;

    @Test
    void testInitCreationForm() throws Exception {
        mockMvc.perform(get("/owners/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("owners/createOrUpdateOwnerForm"))
                .andExpect(model().attributeExists("owner"));
    }

    @Test
    void testProcessCreationFormSuccess() throws Exception {
        mockMvc.perform(post("/owners/new")
                        .param("firstName", "Juan")
                        .param("lastName", "Perez")
                        .param("address", "Av. Amazonas")
                        .param("city", "Quito")
                        .param("telephone", "0987654321")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/owners/*"));
    }

    @Test
    void testProcessCreationFormHasErrors() throws Exception {
        mockMvc.perform(post("/owners/new")
                        .param("firstName", "")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("owners/createOrUpdateOwnerForm"));
    }

    @Test
    void testInitFindForm() throws Exception {
        mockMvc.perform(get("/owners/find"))
                .andExpect(status().isOk())
                .andExpect(view().name("owners/findOwners"));
    }

    @Test
    void testProcessFindFormMultipleResults() throws Exception {
        Owner o1 = new Owner(); o1.setId(1);
        Owner o2 = new Owner(); o2.setId(2);

        given(this.owners.findByLastNameStartingWith(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(o1, o2)));

        mockMvc.perform(get("/owners")
                        .param("lastName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("owners/ownersList"))
                .andExpect(model().attributeExists("listOwners"));
    }

    @Test
    void testProcessFindFormSingleResult() throws Exception {
        Owner o1 = new Owner(); o1.setId(1);

        given(this.owners.findByLastNameStartingWith(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(o1)));

        mockMvc.perform(get("/owners")
                        .param("lastName", "Perez"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owners/1"));
    }

    @Test
    void testInitUpdateOwnerForm() throws Exception {
        Owner owner = new Owner();
        owner.setId(1);
        given(this.owners.findById(1)).willReturn(Optional.of(owner));

        mockMvc.perform(get("/owners/{ownerId}/edit", 1))
                .andExpect(status().isOk())
                .andExpect(view().name("owners/createOrUpdateOwnerForm"))
                .andExpect(model().attributeExists("owner"));
    }

    @Test
    void testProcessUpdateOwnerFormSuccess() throws Exception {
        Owner owner = new Owner();
        owner.setId(1);
        given(this.owners.findById(1)).willReturn(Optional.of(owner));

        mockMvc.perform(post("/owners/{ownerId}/edit", 1)
                        .param("firstName", "Juan Modificado")
                        .param("lastName", "Perez")
                        .param("address", "Nueva Direccion")
                        .param("city", "Quito")
                        .param("telephone", "0999999999")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/owners/{ownerId}"));
    }

    @Test
    void testShowOwner() throws Exception {
        Owner owner = new Owner();
        owner.setId(1);
        given(this.owners.findById(1)).willReturn(Optional.of(owner));

        mockMvc.perform(get("/owners/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("owners/ownerDetails"))
                .andExpect(model().attributeExists("owner"));
    }

    @Test
    void testShowOwnerNotFound() {
        given(this.owners.findById(999)).willReturn(Optional.empty());

        assertThrows(ServletException.class, () -> {
            mockMvc.perform(get("/owners/999"));
        });
    }
}