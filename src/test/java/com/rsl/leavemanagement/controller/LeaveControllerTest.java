package com.rsl.leavemanagement.controller;

import com.rsl.leavemanagement.dto.LeaveRequestDto;
import com.rsl.leavemanagement.dto.LeaveResponseDto;
import com.rsl.leavemanagement.exception.InvalidLeaveRequestException;
import com.rsl.leavemanagement.exception.LeaveNotFoundException;
import com.rsl.leavemanagement.model.LeaveStatus;
import com.rsl.leavemanagement.model.LeaveType;
import com.rsl.leavemanagement.service.LeaveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveController.class)
class LeaveControllerTest {

    private static final String VALID_BODY = """
            {
              "employeeId": "EMP-001",
              "leaveType": "CASUAL",
              "startDate": "2026-10-05",
              "endDate": "2026-10-07",
              "reason": "Personal work"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaveService leaveService;

    @Test
    @DisplayName("GET /leaves returns 200 with all leaves")
    void getAllLeaves() throws Exception {
        when(leaveService.getAllLeaves()).thenReturn(List.of(
                response(1L, LeaveStatus.PENDING),
                response(2L, LeaveStatus.APPROVED)));

        mockMvc.perform(get("/leaves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].startDate").value("2026-10-05"))
                .andExpect(jsonPath("$[1].status").value("APPROVED"));
    }

    @Test
    @DisplayName("GET /leaves/{id} returns 200 with the leave")
    void getLeaveById() throws Exception {
        when(leaveService.getLeaveById(1L)).thenReturn(response(1L, LeaveStatus.PENDING));

        mockMvc.perform(get("/leaves/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.employeeId").value("EMP-001"))
                .andExpect(jsonPath("$.leaveType").value("CASUAL"))
                .andExpect(jsonPath("$.reason").value("Personal work"));
    }

    @Test
    @DisplayName("GET /leaves/{id} returns 404 when the leave does not exist")
    void getLeaveById_notFound() throws Exception {
        when(leaveService.getLeaveById(99L)).thenThrow(new LeaveNotFoundException(99L));

        mockMvc.perform(get("/leaves/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Leave request not found with id: 99"))
                .andExpect(jsonPath("$.path").value("/leaves/99"));
    }

    @Test
    @DisplayName("GET /leaves/{id} returns 400 for a non-numeric id")
    void getLeaveById_invalidId() throws Exception {
        mockMvc.perform(get("/leaves/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("abc")));
    }

    @Test
    @DisplayName("POST /leaves returns 201 with Location header and created leave")
    void createLeave() throws Exception {
        when(leaveService.createLeave(any(LeaveRequestDto.class))).thenReturn(response(1L, LeaveStatus.PENDING));

        mockMvc.perform(post("/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/leaves/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /leaves returns 400 with field errors when required fields are missing")
    void createLeave_validationFails() throws Exception {
        mockMvc.perform(post("/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.employeeId").value("employeeId is required"))
                .andExpect(jsonPath("$.fieldErrors.leaveType").value("leaveType is required"))
                .andExpect(jsonPath("$.fieldErrors.startDate").value("startDate is required"))
                .andExpect(jsonPath("$.fieldErrors.endDate").value("endDate is required"))
                .andExpect(jsonPath("$.fieldErrors.reason").value("reason is required"));

        verify(leaveService, never()).createLeave(any());
    }

    @Test
    @DisplayName("POST /leaves returns 400 for an unknown leave type")
    void createLeave_invalidEnum() throws Exception {
        String body = VALID_BODY.replace("CASUAL", "HOLIDAY");

        mockMvc.perform(post("/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Malformed request body")));
    }

    @Test
    @DisplayName("POST /leaves returns 400 when the service rejects a business rule")
    void createLeave_businessRuleViolation() throws Exception {
        when(leaveService.createLeave(any(LeaveRequestDto.class)))
                .thenThrow(new InvalidLeaveRequestException("endDate must be on or after startDate"));

        mockMvc.perform(post("/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("endDate must be on or after startDate"));
    }

    @Test
    @DisplayName("PUT /leaves/{id} returns 200 with the updated leave")
    void updateLeave() throws Exception {
        when(leaveService.updateLeave(eq(1L), any(LeaveRequestDto.class)))
                .thenReturn(response(1L, LeaveStatus.APPROVED));

        mockMvc.perform(put("/leaves/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PUT /leaves/{id} returns 404 when the leave does not exist")
    void updateLeave_notFound() throws Exception {
        when(leaveService.updateLeave(eq(99L), any(LeaveRequestDto.class)))
                .thenThrow(new LeaveNotFoundException(99L));

        mockMvc.perform(put("/leaves/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /leaves/{id} returns 400 when the reason is blank")
    void updateLeave_validationFails() throws Exception {
        String body = VALID_BODY.replace("Personal work", " ");

        mockMvc.perform(put("/leaves/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").value("reason is required"));
    }

    @Test
    @DisplayName("DELETE /leaves/{id} returns 204")
    void deleteLeave() throws Exception {
        mockMvc.perform(delete("/leaves/1"))
                .andExpect(status().isNoContent());

        verify(leaveService).deleteLeave(1L);
    }

    @Test
    @DisplayName("DELETE /leaves/{id} returns 404 when the leave does not exist")
    void deleteLeave_notFound() throws Exception {
        doThrow(new LeaveNotFoundException(99L)).when(leaveService).deleteLeave(99L);

        mockMvc.perform(delete("/leaves/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    private static LeaveResponseDto response(Long id, LeaveStatus status) {
        return new LeaveResponseDto(id, "EMP-001", LeaveType.CASUAL,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7), "Personal work", status);
    }
}
