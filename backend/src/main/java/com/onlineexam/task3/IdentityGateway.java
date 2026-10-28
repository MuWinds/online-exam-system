package com.onlineexam.task3;

import static com.onlineexam.task3.Models.*;

/** Task1 integration point. Resolve a verified bearer token, never trust client-supplied user IDs. */
public interface IdentityGateway {
    Actor resolve(String authorization);
}
