//! Semantic newborn kernel for Frank.
//!
//! This is intentionally storage-layout agnostic. Candidate A may later pack the
//! same semantics into 128 bits, but the invariant comes first:
//! reinforcement preserves structure, neglect weakens force toward zero, and
//! time alone never inverts polarity.

use std::collections::BTreeMap;

pub type Locus = u64;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum Polarity {
    Positive,
    Negative,
    Neutral,
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Commitment {
    pub locus: Locus,
    pub polarity: Polarity,
    pub force: f32,
    pub last_reinforced_tick: u64,
}

impl Commitment {
    pub fn new(locus: Locus, polarity: Polarity, force: f32, tick: u64) -> Self {
        Self {
            locus,
            polarity,
            force: force.clamp(0.0, 1.0),
            last_reinforced_tick: tick,
        }
    }

    pub fn reinforced(mut self, delta: f32, tick: u64) -> Self {
        self.force = (self.force + delta.max(0.0)).clamp(0.0, 1.0);
        self.last_reinforced_tick = tick;
        self
    }

    /// Passive degradation only reduces force. It never changes polarity.
    pub fn decayed(mut self, factor: f32) -> Self {
        self.force = (self.force * factor.clamp(0.0, 1.0)).clamp(0.0, 1.0);
        self
    }
}

#[derive(Default, Debug)]
pub struct NewbornCore {
    field: BTreeMap<Locus, Commitment>,
}

impl NewbornCore {
    pub fn newborn() -> Self {
        Self::default()
    }

    pub fn get(&self, locus: Locus) -> Option<&Commitment> {
        self.field.get(&locus)
    }

    pub fn snapshot(&self) -> Vec<Commitment> {
        self.field.values().copied().collect()
    }

    pub fn restore(&mut self, commitments: impl IntoIterator<Item = Commitment>) {
        self.field.clear();
        for commitment in commitments {
            self.field.insert(commitment.locus, commitment);
        }
    }

    pub fn absorb(&mut self, incoming: Commitment) {
        match self.field.get(&incoming.locus).copied() {
            None => {
                self.field.insert(incoming.locus, incoming);
            }
            Some(existing) if existing.polarity == incoming.polarity => {
                let force = (existing.force + incoming.force * 0.5).clamp(0.0, 1.0);
                self.field.insert(
                    incoming.locus,
                    Commitment {
                        force,
                        last_reinforced_tick: incoming.last_reinforced_tick,
                        ..existing
                    },
                );
            }
            Some(existing) => {
                let force = (existing.force - incoming.force).abs();
                let polarity = if existing.force >= incoming.force {
                    existing.polarity
                } else {
                    incoming.polarity
                };

                self.field.insert(
                    incoming.locus,
                    Commitment {
                        locus: incoming.locus,
                        polarity,
                        force,
                        last_reinforced_tick: incoming.last_reinforced_tick,
                    },
                );
            }
        }
    }

    pub fn decay_all(&mut self, factor: f32) {
        for commitment in self.field.values_mut() {
            commitment.force = (commitment.force * factor.clamp(0.0, 1.0)).clamp(0.0, 1.0);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn decay_moves_toward_zero_without_inverting_polarity() {
        let mut core = NewbornCore::newborn();
        core.absorb(Commitment::new(7, Polarity::Positive, 0.8, 1));
        core.decay_all(0.5);

        let c = core.get(7).unwrap();
        assert_eq!(c.polarity, Polarity::Positive);
        assert!((c.force - 0.4).abs() < f32::EPSILON);
    }

    #[test]
    fn stronger_contradiction_preserves_residual_difference() {
        let mut core = NewbornCore::newborn();
        core.absorb(Commitment::new(9, Polarity::Positive, 0.4, 1));
        core.absorb(Commitment::new(9, Polarity::Negative, 0.9, 2));

        let c = core.get(9).unwrap();
        assert_eq!(c.polarity, Polarity::Negative);
        assert!((c.force - 0.5).abs() < f32::EPSILON);
    }
}
