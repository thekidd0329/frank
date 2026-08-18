from __future__ import annotations

from .models import ConfidenceFeatures


def provisional_confidence(features: ConfidenceFeatures) -> float:
    """
    Deliberately simple bootstrap score.

    This is NOT treated as calibrated probability. The evaluation harness will
    later fit a calibration layer from empirical outcomes.
    """
    positive = (
        features.identity_certainty
        + features.source_agreement
        + features.evidence_freshness
        + features.evidence_quantity
        + (1.0 - features.contradiction_severity)
        + (1.0 - features.inference_distance)
    ) / 6.0

    if features.missing_critical_info:
        positive *= 0.6

    return max(0.0, min(1.0, positive))
