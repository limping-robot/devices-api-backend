package com.devices;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared HTTP API checks; subclasses supply datasource (Testcontainers vs Compose Postgres).
 */
@Transactional
abstract class DeviceApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Test
    void createListFilterGet() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phone\",\"brand\":\"Acme\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Phone"))
                .andExpect(jsonPath("$.brand").value("Acme"))
                .andExpect(jsonPath("$.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tab\",\"brand\":\"acme\",\"state\":\"INACTIVE\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/devices").param("brand", "acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/devices").param("state", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/devices").param("brand", "Acme").param("state", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        var list = mockMvc.perform(get("/api/devices").param("state", "AVAILABLE"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var id = objectMapper.readTree(list).get("content").get(0).get("id").asLong();

        mockMvc.perform(get("/api/devices/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(get("/api/devices/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void putTransitionToInUseRequiresSameNameAndBrand() throws Exception {
        var res = mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"brand\":\"B\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var id = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(put("/api/devices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other\",\"brand\":\"B\",\"state\":\"IN_USE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void putPatchAndCreationTimeImmutable() throws Exception {
        var res = mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"brand\":\"Y\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var id = objectMapper.readTree(res).get("id").asLong();
        var createdAt = objectMapper.readTree(res).get("createdAt").asText();

        mockMvc.perform(put("/api/devices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X2\",\"brand\":\"Y2\",\"state\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("INACTIVE"));

        mockMvc.perform(get("/api/devices/" + id))
                .andExpect(jsonPath("$.createdAt").value(createdAt));

        mockMvc.perform(patch("/api/devices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"AVAILABLE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/devices/" + id))
                .andExpect(jsonPath("$.createdAt").value(createdAt));
    }

    @Test
    void inUseCannotRenameOrDelete() throws Exception {
        var res = mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"brand\":\"B\",\"state\":\"IN_USE\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var id = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(put("/api/devices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other\",\"brand\":\"B\",\"state\":\"IN_USE\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/devices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/devices/" + id))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/devices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"AVAILABLE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/devices/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidPatchState() throws Exception {
        var res = mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"brand\":\"B\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var id = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(patch("/api/devices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchCreatedAtRejected() throws Exception {
        var res = mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"brand\":\"B\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var id = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(patch("/api/devices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"createdAt\": \"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchIntoInUseCannotRenameInSameRequest() throws Exception {
        var res = mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"brand\":\"B\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var id = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(patch("/api/devices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"IN_USE\",\"name\":\"Other\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void patchOutOfUseMayRenameInSameRequest() throws Exception {
        var res = mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"brand\":\"B\",\"state\":\"IN_USE\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var id = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(patch("/api/devices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"AVAILABLE\",\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.state").value("AVAILABLE"));
    }

    @Test
    void paginationPageSizeIsRespected() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Device" + i + "\",\"brand\":\"Brand\"}"))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/devices").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void paginationSecondPageReturnsRemainingItems() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Device" + i + "\",\"brand\":\"Brand\"}"))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/devices").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void paginationRespectsFilteredCount() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Device" + i + "\",\"brand\":\"Acme\"}"))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other\",\"brand\":\"Other\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/devices").param("brand", "acme").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3));
    }
}
