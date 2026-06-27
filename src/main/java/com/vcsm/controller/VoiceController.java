package com.vcsm.controller;

import com.vcsm.service.LanguageDetectionService;
import com.vcsm.service.HindiCommandMapper;
import com.vcsm.model.VoiceCommand;
import com.vcsm.service.OmnidimService;
import com.vcsm.service.SentimentAnalysisService;
import com.vcsm.service.IvrService;
import com.vcsm.dto.IvrNode;
import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.vcsm.dto.ErrorResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = "*")
public class VoiceController {

    @Autowired
    private OmnidimService omnidimService;
    
    @Autowired
    private SentimentAnalysisService sentimentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LanguageDetectionService languageDetectionService;

    @Autowired
    private HindiCommandMapper hindiCommandMapper;

    @Autowired
    private IvrService ivrService;

    @PostMapping("/command")
    public ResponseEntity<Map<String, Object>> command(@RequestBody Map<String, String> body) {
        String transcript = body.get("transcript");
        
        if (transcript == null || transcript.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Transcript required", "success", false));
        }
        
        // Authentication check FIRST
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", 401);
            error.put("error", "Unauthorized");
            error.put("message", "Authentication required");
            error.put("success", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        User user = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + auth.getName()));
        
        // Detect language
        String language = languageDetectionService.detectLanguage(transcript);
        
        Map<String, Object> response = new HashMap<>();
        response.put("originalText", transcript);
        response.put("detectedLanguage", language);
        
        // Process based on language
        if (language.equals("hi")) {
            // Hindi command
            String action = hindiCommandMapper.mapCommand(transcript);
            if (action != null) {
                response.put("action", action);
                response.put("response", hindiCommandMapper.getResponse(action, null));
                response.put("success", true);
            } else {
                response.put("action", "unknown");
                response.put("response", hindiCommandMapper.getDefaultResponse());
                response.put("success", false);
            }
        } else {
            // English command - route dynamically via active IVR JSON flow tree
            Map<String, Object> ivrResponse = ivrService.processInteraction(user.getEmail(), transcript);
            response.put("action", ivrResponse.get("action"));
            response.put("response", ivrResponse.get("prompt"));
            response.put("success", ivrResponse.get("success"));
            response.put("currentNodeId", ivrResponse.get("currentNodeId"));
        }
        
        // Analyze sentiment dynamically from authenticated user
        Long userId = user.getId();
        sentimentService.analyzeAndProcess(userId, transcript);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<VoiceCommand>> history() {
        return ResponseEntity.ok(omnidimService.getRecentCommands());
    }

    @GetMapping("/flow-config")
    public ResponseEntity<IvrNode> getFlowConfig() {
        return ResponseEntity.ok(ivrService.getActiveFlow());
    }

    @PostMapping("/flow-config")
    public ResponseEntity<Map<String, Object>> saveFlowConfig(@RequestBody Map<String, String> body) {
        String flowJson = body.get("flowJson");
        if (flowJson == null || flowJson.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "flowJson is required", "success", false));
        }
        ivrService.saveFlow(flowJson);
        return ResponseEntity.ok(Map.of("message", "IVR Flow Configuration updated successfully", "success", true));
    }
}
