use crate::memory::{concept_tokens, normalize, MemoryStore};
use std::cmp::Ordering;
use std::collections::BTreeSet;

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Curiosity {
    pub key: String,
    pub question: String,
    pub score: i32,
    pub source: String,
}

#[derive(Debug, Default)]
pub struct CuriosityEngine;

impl CuriosityEngine {
    pub fn generate(&self, utterance: &str, memory: &MemoryStore) -> Vec<Curiosity> {
        let mut out = Vec::new();
        let mut seen = BTreeSet::new();

        for candidate in named_candidates(utterance) {
            let n = normalize(&candidate);
            if n.is_empty() || memory.knows_concept(&n) {
                continue;
            }
            let key = format!("concept:{}", n);
            if seen.insert(key.clone()) && !memory.was_question_asked(&key) {
                out.push(Curiosity {
                    key,
                    question: format!("What is {}?", candidate.trim()),
                    score: 100 + candidate.len() as i32,
                    source: "unresolved named concept".into(),
                });
            }
        }

        for token in concept_tokens(utterance) {
            if memory.knows_concept(&token) || token.len() < 4 {
                continue;
            }
            let key = format!("concept:{}", token);
            if seen.insert(key.clone()) && !memory.was_question_asked(&key) {
                let novelty = token.chars().filter(|c| c.is_uppercase()).count() as i32;
                out.push(Curiosity {
                    key,
                    question: format!("What does {} mean here?", token),
                    score: 40 + token.len() as i32 + novelty,
                    source: "unresolved conversation concept".into(),
                });
            }
        }

        if let Some((subject, value)) = parse_relation(utterance) {
            let subject_n = normalize(&subject);
            if !subject_n.is_empty() && memory.facts_about(&subject_n).is_empty() {
                let key = format!("relationship:{}", subject_n);
                if seen.insert(key.clone()) && !memory.was_question_asked(&key) {
                    out.push(Curiosity {
                        key,
                        question: format!("What should I understand about {}?", subject),
                        score: 75 + value.len() as i32,
                        source: "thin relationship knowledge".into(),
                    });
                }
            }
        }

        out.sort_by(|a, b| match b.score.cmp(&a.score) {
            Ordering::Equal => a.key.cmp(&b.key),
            other => other,
        });
        out
    }

    pub fn next_question(&self, utterance: &str, memory: &MemoryStore) -> Option<Curiosity> {
        self.generate(utterance, memory).into_iter().next()
    }
}

fn named_candidates(text: &str) -> Vec<String> {
    let mut candidates = Vec::new();
    let mut run: Vec<String> = Vec::new();

    for raw in text.split_whitespace() {
        let cleaned = raw.trim_matches(|c: char| !c.is_alphanumeric() && c != '-' && c != '_');
        let starts_upper = cleaned.chars().next().map(|c| c.is_uppercase()).unwrap_or(false);
        let looks_acronym = cleaned.len() >= 2
            && cleaned.chars().filter(|c| c.is_alphabetic()).all(|c| c.is_uppercase());

        if !cleaned.is_empty() && (starts_upper || looks_acronym) && !sentence_leader(cleaned) {
            run.push(cleaned.to_string());
        } else if !run.is_empty() {
            candidates.push(run.join(" "));
            run.clear();
        }
    }
    if !run.is_empty() {
        candidates.push(run.join(" "));
    }
    candidates
}

fn sentence_leader(s: &str) -> bool {
    matches!(
        s,
        "I" | "I'm" | "Im" | "The" | "A" | "An" | "This" | "That" | "It" | "My" | "We" | "You" | "What"
            | "Why" | "How" | "When" | "Where" | "Who" | "Because" | "So" | "And" | "But"
    )
}

fn parse_relation(text: &str) -> Option<(String, String)> {
    let lower = text.to_lowercase();
    for marker in [" is my ", " is a ", " is an ", " means ", " is "] {
        if let Some(idx) = lower.find(marker) {
            let left = text[..idx].trim().trim_matches(|c: char| !c.is_alphanumeric() && c != ' ');
            let right = text[idx + marker.len()..].trim().trim_matches(|c: char| c == '.' || c == '!' || c == '?');
            if !left.is_empty() && !right.is_empty() && left.split_whitespace().count() <= 5 {
                return Some((left.to_string(), right.to_string()));
            }
        }
    }
    None
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::env;
    use std::fs;
    use std::time::{SystemTime, UNIX_EPOCH};

    fn memory() -> MemoryStore {
        let n = SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_nanos();
        MemoryStore::load(env::temp_dir().join(format!("frank-curiosity-{n}.tsv"))).unwrap()
    }

    #[test]
    fn asks_about_new_named_concept() {
        let m = memory();
        let q = CuriosityEngine.next_question("I talked to Akira about Merkaba today.", &m).unwrap();
        assert!(q.question.contains("Akira") || q.question.contains("Merkaba"));
        let _ = fs::remove_file(m.path());
    }

    #[test]
    fn does_not_repeat_marked_question() {
        let mut m = memory();
        let engine = CuriosityEngine;
        let q = engine.next_question("Akira told me something.", &m).unwrap();
        m.mark_question_asked(&q.key).unwrap();
        let remaining = engine.generate("Akira told me something.", &m);
        assert!(remaining.iter().all(|x| x.key != q.key));
        let _ = fs::remove_file(m.path());
    }
}
