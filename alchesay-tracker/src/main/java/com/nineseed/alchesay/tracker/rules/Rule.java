package com.nineseed.alchesay.tracker.rules;

import com.nineseed.alchesay.tracker.workflow.Context;
import tools.jackson.databind.JsonNode;

public interface Rule {
    boolean test(JsonNode rootNode, Context context);
}
