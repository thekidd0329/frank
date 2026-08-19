package com.frank.policy;

@VintfStability
interface IFrankPolicy {
    boolean requestCapability(int uid, String capability, String rationale);
    void revokeCapability(int uid, String capability);
    boolean hasCapability(int uid, String capability);
    String getDecisionReason(int uid, String capability);
}
