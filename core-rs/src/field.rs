//! Sparse Residual Commitment Field.
//! Authoritative ground state for the experimental developmental kernel.

use std::collections::HashMap;

use crate::commitment::{CommitmentError, LocusId, ResidualCommitment};

pub const DEFAULT_PRUNE: f32 = 1e-6;

#[derive(Clone, Debug, Default)]
pub struct CommitmentField {
    map: HashMap<LocusId, ResidualCommitment>,
    pub current_tick: u32,
    pub retention_per_tick: f32,
    pub prune_threshold: f32,
}

impl CommitmentField {
    pub fn new(retention_per_tick: f32) -> Self {
        Self { map: HashMap::new(), current_tick: 0, retention_per_tick, prune_threshold: DEFAULT_PRUNE }
    }

    pub fn len(&self) -> usize { self.map.len() }
    pub fn is_empty(&self) -> bool { self.map.is_empty() }
    pub fn contains(&self, locus: LocusId) -> bool { self.map.contains_key(&locus) }
    pub fn tick(&mut self) { self.current_tick = self.current_tick.saturating_add(1); }
    pub fn set_tick(&mut self, tick: u32) { self.current_tick = tick; }

    pub fn insert_raw(&mut self, commitment: ResidualCommitment) -> Result<(), CommitmentError> {
        commitment.validate()?;
        self.map.insert(commitment.locus_id, commitment);
        Ok(())
    }

    pub fn remove(&mut self, locus: LocusId) -> Option<ResidualCommitment> { self.map.remove(&locus) }

    pub fn materialize(&mut self, locus: LocusId) -> Result<Option<ResidualCommitment>, CommitmentError> {
        let Some(c) = self.map.get(&locus).copied() else { return Ok(None); };
        match c.materialize_at(self.current_tick, self.retention_per_tick, self.prune_threshold)? {
            Some(updated) => { self.map.insert(locus, updated); Ok(Some(updated)) }
            None => { self.map.remove(&locus); Ok(None) }
        }
    }

    pub fn apply_evidence(&mut self, locus: LocusId, evidence_force: f32) -> Result<Option<ResidualCommitment>, CommitmentError> {
        let Some(c) = self.map.get(&locus).copied() else { return Ok(None); };
        match c.apply_evidence(self.current_tick, evidence_force, self.retention_per_tick, self.prune_threshold)? {
            Some(updated) => { self.map.insert(locus, updated); Ok(Some(updated)) }
            None => { self.map.remove(&locus); Ok(None) }
        }
    }

    pub fn birth(&mut self, locus: LocusId, force: f32) -> Result<ResidualCommitment, CommitmentError> {
        if self.map.contains_key(&locus) { return Err(CommitmentError::ForceOutOfRange); }
        let c = ResidualCommitment::new(locus, force, self.current_tick)?;
        self.map.insert(locus, c);
        Ok(c)
    }

    /// Birth when absent; otherwise apply signed evidence. Exact cancellation stays pruned.
    pub fn birth_or_reinforce(&mut self, locus: LocusId, force: f32) -> Result<Option<ResidualCommitment>, CommitmentError> {
        if self.map.contains_key(&locus) {
            self.apply_evidence(locus, force)
        } else {
            if force.abs() <= self.prune_threshold { return Ok(None); }
            self.birth(locus, force).map(Some)
        }
    }

    pub fn snapshot(&self) -> Vec<ResidualCommitment> {
        let mut v: Vec<_> = self.map.values().copied().collect();
        v.sort_by_key(|c| c.locus_id);
        v
    }

    pub fn loci(&self) -> impl Iterator<Item = LocusId> + '_ { self.map.keys().copied() }

    pub fn effective_force(&self, locus: LocusId) -> Result<Option<f32>, CommitmentError> {
        match self.map.get(&locus) {
            Some(c) => Ok(Some(c.effective_force(self.current_tick, self.retention_per_tick)?)),
            None => Ok(None),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn cancellation_prunes_without_rebirth() {
        let mut f = CommitmentField::new(1.0);
        f.set_tick(1);
        f.birth(0xAA, 0.50).unwrap();
        let result = f.birth_or_reinforce(0xAA, -0.50).unwrap();
        assert!(result.is_none());
        assert!(!f.contains(0xAA));
    }
}
