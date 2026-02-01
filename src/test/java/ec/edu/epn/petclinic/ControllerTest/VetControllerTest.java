package ec.edu.epn.petclinic.ControllerTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

import ec.edu.epn.petclinic.vet.Specialty;
import ec.edu.epn.petclinic.vet.Vet;
import ec.edu.epn.petclinic.vet.VetController;
import ec.edu.epn.petclinic.vet.VetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VetController.class)
class VetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VetRepository vets;

    private Vet james;

    @BeforeEach
    void setup() {
        james = new Vet();
        james.setId(1);
        james.setFirstName("James");
        james.setLastName("Carter");

        Specialty radiology = new Specialty();
        radiology.setId(1);
        radiology.setName("radiology");
        james.addSpecialty(radiology);
    }

    @Test
    void testShowVetListHtml() throws Exception {
        List<Vet> vetList = Arrays.asList(james);
        Page<Vet> page = new PageImpl<>(vetList);

        given(this.vets.findAll(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/vets.html").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("listVets"))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(view().name("vets/vetList"));
    }

    @Test
    void testShowResourcesVetListJson() throws Exception {
        given(this.vets.findAll()).willReturn(Arrays.asList(james));

        mockMvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.vetList[0].firstName").value("James"))
                .andExpect(jsonPath("$.vetList[0].lastName").value("Carter"))
                .andExpect(jsonPath("$.vetList[0].specialties[0].name").value("radiology"));
    }
}