package com.test.demo.webhook.gitlab.processor;

import com.test.demo.webhook.gitlab.dto.MergeRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.function.Predicate;

@Slf4j
@Component
public class MergeRequestValidator {
    private final Set<String> targetBranches;

    public MergeRequestValidator(Set<String> targetBranches) {
        this.targetBranches = targetBranches;
    }

    public boolean validate(MergeRequestEvent event) {
        return isValidEvent(event) &&
               isMergeEvent(event) &&
               hasValidTargetBranch(event);
    }

    private boolean isValidEvent(MergeRequestEvent event) {
        // Combine checks directly
        boolean valid = event != null &&
                        "merge_request".equals(event.objectKind()) &&
                        event.attributes() != null &&
                        event.attributes().lastCommit() != null &&
                        StringUtils.hasText(event.attributes().lastCommit().id()) &&
                        event.attributes().targetProjectId() != null &&
                        event.attributes().iid() != null;

        if (!valid) {
            log.warn("Skipping event: Invalid object_kind or missing required fields. Event: {}", event); // Log event for context
            return false;
        }
        return true;
    }

    private boolean isMergeEvent(MergeRequestEvent event) {
        if (!"merge".equalsIgnoreCase(event.attributes().action())) {
            log.info("Skipping MR !{}: Action is '{}' not 'merge'", 
                event.attributes().iid(), event.attributes().action());
            return false;
        }
        return true;
    }

    private boolean hasValidTargetBranch(MergeRequestEvent event) {
        String targetBranch = event.attributes().targetBranch();
        // Add null check for targetBranch
        if (targetBranch == null || !targetBranches.contains(targetBranch)) {
            log.info("Skipping MR !{}: Target branch '{}' is null or not in configured list {}",
                event.attributes().iid(), targetBranch, targetBranches);
            return false;
        }
        return true;
    }
}
