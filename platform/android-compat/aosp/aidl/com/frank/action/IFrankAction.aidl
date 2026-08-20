package com.frank.action;

/**
 * Experimental backend-neutral action boundary.
 *
 * Do not freeze this interface until the Android action contract is stable.
 * Cognition proposes actions; a privileged Frank action service performs
 * policy, consent, and backend selection before any external effect occurs.
 */
interface IFrankAction {
    int requestAction(
        String actionId,
        String target,
        int backendHint,
        boolean consentGranted
    );
}
