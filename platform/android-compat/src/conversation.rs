use crate::curiosity::{Curiosity, CuriosityEngine};
use crate::memory::{normalize, Fact, MemoryStore};
use std::io;

#[derive(Debug)]
pub struct TurnResult {
    pub reply: String,
    pub learned: Vec<Fact>,
    pub question: Option<String>,
    pub curiosity_source: Option<String>,
}

pub struct ConversationEngine {
    memory: MemoryStore,
    curiosity: CuriosityEngine,
    pending: Option<Curiosity>,
    turns: u64,
}

impl ConversationEngine {
    pub fn new(memory: MemoryStore) -> Self {
        Self {
            memory,
            curiosity: CuriosityEngine,
            pending: None,
            turns: 0,
        }
    }

    pub fn memory(&self) -> &MemoryStore {
        &self.memory
    }

    pub fn memory_mut(&mut self) -> &mut MemoryStore {
        &mut self.memory
    }

    pub fn pending_question(&self) -> Option<&str> {
        self.pending.as_ref().map(|q| q.question.as_str())
    }

    pub fn handle(&mut self, input: &str) -> io::Result<TurnResult> {
        let input = input.trim();
        if input.is_empty() {
            return Ok(TurnResult {
                reply: "I'm listening.".into(),
                learned: vec![],
                question: None,
                curiosity_source: None,
            });
        }

        self.turns += 1;
        let mut learned = Vec::new();

        if let Some(pending) = self.pending.take() {
            if let Some(fact) = learn_from_answer(&pending, input) {
                self.memory.remember_fact(fact.clone())?;
                learned.push(fact);
            }
        }

        for fact in extract_facts(input) {
            if self.memory.remember_fact(fact.clone())? {
                learned.push(fact);
            }
        }

        let answer = answer_from_memory(input, &self.memory);
        let mut reply = answer.unwrap_or_else(|| acknowledgement(input, &learned, self.turns));

        let next = self.curiosity.next_question(input, &self.memory);
        let (question, curiosity_source) = if let Some(q) = next {
            self.memory.mark_question_asked(&q.key)?;
            let question = q.question.clone();
            let source = q.source.clone();
            self.pending = Some(q);
            if !reply.is_empty() {
                reply.push(' ');
            }
            reply.push_str(&question);
            (Some(question), Some(source))
        } else {
            (None, None)
        };

        Ok(TurnResult {
            reply,
            learned,
            question,
            curiosity_source,
        })
    }
}

fn learn_from_answer(pending: &Curiosity, answer: &str) -> Option<Fact> {
    if answer.trim().is_empty() || looks_like_refusal(answer) {
        return None;
    }
    if let Some(subject) = pending.key.strip_prefix("concept:") {
        return Some(Fact::new(subject, "definition", answer));
    }
    if let Some(subject) = pending.key.strip_prefix("relationship:") {
        return Some(Fact::new(subject, "context", answer));
    }
    None
}

fn looks_like_refusal(s: &str) -> bool {
    let n = normalize(s);
    matches!(n.as_str(), "idk" | "i don't know" | "dont know" | "not sure" | "skip" | "no idea")
}

pub fn extract_facts(input: &str) -> Vec<Fact> {
    let text = input.trim().trim_end_matches(['.', '!', '?']);
    let lower = text.to_lowercase();
    let mut out = Vec::new();

    if let Some(value) = strip_prefix_ci(text, "my name is ") {
        out.push(Fact::new("user", "name", value));
    }
    if let Some(value) = strip_prefix_ci(text, "i live in ") {
        out.push(Fact::new("user", "lives_in", value));
    }
    if let Some(value) = strip_prefix_ci(text, "i like ") {
        out.push(Fact::new("user", "likes", value));
    }
    if let Some(value) = strip_prefix_ci(text, "i love ") {
        out.push(Fact::new("user", "loves", value));
    }
    if let Some(value) = strip_prefix_ci(text, "i hate ") {
        out.push(Fact::new("user", "dislikes", value));
    }
    if let Some(value) = strip_prefix_ci(text, "i am ").or_else(|| strip_prefix_ci(text, "i'm ")) {
        out.push(Fact::new("user", "is", value));
    }

    for marker in [" is my ", " means ", " is an ", " is a ", " is "] {
        if let Some(idx) = lower.find(marker) {
            let subject = text[..idx].trim();
            let value = text[idx + marker.len()..].trim();
            if !subject.is_empty()
                && !value.is_empty()
                && subject.split_whitespace().count() <= 6
                && !subject.eq_ignore_ascii_case("i")
            {
                let relation = match marker {
                    " is my " => "relationship",
                    " means " => "definition",
                    _ => "is",
                };
                out.push(Fact::new(subject, relation, value));
            }
            break;
        }
    }

    dedupe(out)
}

fn answer_from_memory(input: &str, memory: &MemoryStore) -> Option<String> {
    let trimmed = input.trim().trim_end_matches(['?', '.', '!']);
    let lower = trimmed.to_lowercase();
    let subject = if lower.starts_with("what is ") {
        Some(trimmed[8..].trim())
    } else if lower.starts_with("what's ") {
        Some(trimmed[7..].trim())
    } else if lower.starts_with("who is ") {
        Some(trimmed[7..].trim())
    } else if lower.starts_with("who's ") {
        Some(trimmed[6..].trim())
    } else if lower.starts_with("what do you know about ") {
        Some(trimmed[23..].trim())
    } else {
        None
    }?;

    let facts = memory.facts_about(subject);
    if facts.is_empty() {
        return Some(format!("I don't know enough about {} yet.", subject));
    }

    let pieces: Vec<String> = facts
        .iter()
        .take(4)
        .map(|f| match f.relation.as_str() {
            "definition" | "is" => f.value.clone(),
            "relationship" => format!("your {}", f.value),
            relation => format!("{}: {}", relation.replace('_', " "), f.value),
        })
        .collect();
    Some(format!("What I remember about {}: {}.", subject, pieces.join("; ")))
}

fn acknowledgement(input: &str, learned: &[Fact], turn: u64) -> String {
    if learned.len() >= 2 {
        return format!("Okay. I pulled {} things out of that and saved them.", learned.len());
    }
    if let Some(fact) = learned.first() {
        return match fact.relation.as_str() {
            "definition" => format!("Okay — now I have a meaning for {}.", fact.subject),
            "relationship" => format!("Got it. I saved how {} relates to you.", fact.subject),
            _ => "Got it. I'm keeping that.".into(),
        };
    }

    if input.ends_with('?') {
        "I don't have enough learned context to answer that confidently yet.".into()
    } else {
        match turn % 4 {
            0 => "I'm following.".into(),
            1 => "Okay.".into(),
            2 => "I have that.".into(),
            _ => "I'm building the picture.".into(),
        }
    }
}

fn strip_prefix_ci<'a>(text: &'a str, prefix: &str) -> Option<&'a str> {
    let lower = text.to_lowercase();
    if lower.starts_with(prefix) {
        Some(text[prefix.len()..].trim())
    } else {
        None
    }
}

fn dedupe(facts: Vec<Fact>) -> Vec<Fact> {
    let mut out: Vec<Fact> = Vec::new();
    for fact in facts {
        if !out.iter().any(|f| f.subject == fact.subject && f.relation == fact.relation && f.value == fact.value) {
            out.push(fact);
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::env;
    use std::fs;
    use std::time::{SystemTime, UNIX_EPOCH};

    fn engine() -> ConversationEngine {
        let n = SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_nanos();
        let path = env::temp_dir().join(format!("frank-conversation-{n}.tsv"));
        ConversationEngine::new(MemoryStore::load(path).unwrap())
    }

    #[test]
    fn learns_explicit_relationships() {
        let mut e = engine();
        e.handle("Joel is my stepdad.").unwrap();
        assert_eq!(e.memory().fact("joel", "relationship").unwrap().value, "stepdad");
        let path = e.memory().path().to_path_buf();
        drop(e);
        let _ = fs::remove_file(path);
    }

    #[test]
    fn answer_to_own_question_becomes_memory() {
        let mut e = engine();
        let first = e.handle("I was working with Merkaba today.").unwrap();
        assert!(first.question.is_some());
        let pending_key = e.pending.as_ref().unwrap().key.clone();
        e.handle("It's a geometric spiritual concept I use in my writing.").unwrap();
        if let Some(subject) = pending_key.strip_prefix("concept:") {
            assert!(e.memory().fact(subject, "definition").is_some());
        }
        let path = e.memory().path().to_path_buf();
        drop(e);
        let _ = fs::remove_file(path);
    }
}
