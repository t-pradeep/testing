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
        Predicate<MergeRequestEvent> isValid = e -> 
            "merge_request".equals(e.objectKind()) && 
            e.attributes() != null &&
            e.attributes().lastCommit() != null &&
            StringUtils.hasText(e.attributes().lastCommit().id()) &&
            e.attributes().targetProjectId() != null &&
            e.attributes().iid() != null;

        if (!isValid.test(event)) {
            log.warn("Skipping event: Invalid object_kind or missing required fields");
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
        if (!targetBranches.contains(event.attributes().targetBranch())) {
            log.info("Skipping MR !{}: Target branch '{}' not in configured list",
                event.attributes().iid(), event.attributes().targetBranch());
            return false;
        }
        return true;
    }
}
