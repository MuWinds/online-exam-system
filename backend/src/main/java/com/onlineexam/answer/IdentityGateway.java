package com.onlineexam.answer;

import static com.onlineexam.answer.Models.*;

/** Task1 integration point. Resolve a verified bearer token, never trust client-supplied user IDs. */
public interface IdentityGateway {
    Actor resolve(String authorization);
}
