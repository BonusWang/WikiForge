package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.CreatePersonalRecordRequest;
import com.wikiforge.core.application.dto.PersonalRecordObsidianNoteResponse;
import com.wikiforge.core.application.dto.PersonalRecordPageResponse;
import com.wikiforge.core.application.dto.PersonalRecordResponse;
import com.wikiforge.core.application.dto.PersonalRecordSummaryResponse;
import com.wikiforge.core.application.service.PersonalRecordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/personal-records")
public class PersonalRecordController {

    private final PersonalRecordService personalRecordService;

    public PersonalRecordController(PersonalRecordService personalRecordService) {
        this.personalRecordService = personalRecordService;
    }

    @PostMapping
    public ApiResponse<PersonalRecordResponse> createRecord(
            @Valid @RequestBody CreatePersonalRecordRequest request
    ) {
        return ApiResponse.ok(personalRecordService.createRecord(request));
    }

    @GetMapping
    public ApiResponse<PersonalRecordPageResponse> listRecords(
            @RequestParam(required = false) String recordType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sourceChannel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(personalRecordService.listRecords(recordType, status, sourceChannel, page, pageSize));
    }

    @GetMapping("/summary")
    public ApiResponse<PersonalRecordSummaryResponse> summary(
            @RequestParam(defaultValue = "all") String period
    ) {
        return ApiResponse.ok(personalRecordService.summary(period));
    }

    @GetMapping("/{recordUid}")
    public ApiResponse<PersonalRecordResponse> getRecord(@PathVariable String recordUid) {
        return ApiResponse.ok(personalRecordService.getRecord(recordUid));
    }

    @PostMapping("/{recordUid}/obsidian-note")
    public ApiResponse<PersonalRecordObsidianNoteResponse> writeObsidianNote(@PathVariable String recordUid) {
        return ApiResponse.ok(personalRecordService.writeObsidianNote(recordUid));
    }
}
