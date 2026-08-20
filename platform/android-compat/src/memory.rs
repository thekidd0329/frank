use std::collections::{BTreeMap, BTreeSet};
use std::env;
use std::fs::{self, File};
use std::io::{self, BufRead, BufReader, Write};
use std::path::{Path, PathBuf};
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Fact {
    pub subject: String,
    pub relation: String,
    pub value: String,
    pub confidence: u8,
    pub learned_at: u64,
}

impl Fact {
    pub fn new(subject: impl Into<String>, relation: impl Into<String>, value: impl Into<String>) -> Self {
        Self {
            subject: normalize(subject.into()),
            relation: normalize(relation.into()),
            value: value.into().trim().to_string(),
            confidence: 90,
            learned_at: now_epoch(),
        }
    }

    fn key(&self) -> String {
        format!("{}\u{1f}{}", self.subject, self.relation)
    }
}

#[derive(Debug)]
pub struct MemoryStore {
    facts: BTreeMap<String, Fact>,
    known_concepts: BTreeSet<String>,
    asked_questions: BTreeSet<String>,
    path: PathBuf,
}

impl MemoryStore {
    pub fn load_default() -> io::Result<Self> {
        let base = env::var_os("FRANK_HOME")
            .map(PathBuf::from)
            .or_else(|| env::var_os("HOME").map(|h| PathBuf::from(h).join(".frank")))
            .unwrap_or_else(|| PathBuf::from(".frank"));
        Self::load(base.join("memory.tsv"))
    }

    pub fn load(path: impl Into<PathBuf>) -> io::Result<Self> {
        let path = path.into();
        let mut store = Self {
            facts: BTreeMap::new(),
            known_concepts: BTreeSet::new(),
            asked_questions: BTreeSet::new(),
            path,
        };

        if store.path.exists() {
            let file = File::open(&store.path)?;
            for line in BufReader::new(file).lines() {
                let line = line?;
                store.decode_line(&line);
            }
        }
        Ok(store)
    }

    pub fn remember_fact(&mut self, fact: Fact) -> io::Result<bool> {
        let changed = self
            .facts
            .get(&fact.key())
            .map(|old| old.value != fact.value || old.confidence != fact.confidence)
            .unwrap_or(true);
        self.known_concepts.insert(fact.subject.clone());
        self.facts.insert(fact.key(), fact);
        if changed {
            self.save()?;
        }
        Ok(changed)
    }

    pub fn remember_concept(&mut self, concept: &str) -> io::Result<()> {
        let concept = normalize(concept);
        if !concept.is_empty() && self.known_concepts.insert(concept) {
            self.save()?;
        }
        Ok(())
    }

    pub fn mark_question_asked(&mut self, question_key: &str) -> io::Result<()> {
        if self.asked_questions.insert(normalize(question_key)) {
            self.save()?;
        }
        Ok(())
    }

    pub fn was_question_asked(&self, question_key: &str) -> bool {
        self.asked_questions.contains(&normalize(question_key))
    }

    pub fn knows_concept(&self, concept: &str) -> bool {
        let n = normalize(concept);
        if n.is_empty() {
            return true;
        }
        self.known_concepts.contains(&n)
            || self.facts.values().any(|f| f.subject == n || normalize(&f.value) == n)
    }

    pub fn facts_about(&self, subject: &str) -> Vec<&Fact> {
        let subject = normalize(subject);
        self.facts.values().filter(|f| f.subject == subject).collect()
    }

    pub fn fact(&self, subject: &str, relation: &str) -> Option<&Fact> {
        self.facts.get(&format!("{}\u{1f}{}", normalize(subject), normalize(relation)))
    }

    pub fn all_facts(&self) -> impl Iterator<Item = &Fact> {
        self.facts.values()
    }

    pub fn fact_count(&self) -> usize {
        self.facts.len()
    }

    pub fn concept_count(&self) -> usize {
        self.known_concepts.len()
    }

    pub fn forget_all(&mut self) -> io::Result<()> {
        self.facts.clear();
        self.known_concepts.clear();
        self.asked_questions.clear();
        self.save()
    }

    pub fn path(&self) -> &Path {
        &self.path
    }

    fn save(&self) -> io::Result<()> {
        if let Some(parent) = self.path.parent() {
            fs::create_dir_all(parent)?;
        }
        let tmp = self.path.with_extension("tmp");
        let mut out = File::create(&tmp)?;
        writeln!(out, "# Frank local memory v1")?;
        for fact in self.facts.values() {
            writeln!(
                out,
                "F\t{}\t{}\t{}\t{}\t{}",
                escape(&fact.subject),
                escape(&fact.relation),
                escape(&fact.value),
                fact.confidence,
                fact.learned_at
            )?;
        }
        for concept in &self.known_concepts {
            writeln!(out, "C\t{}", escape(concept))?;
        }
        for q in &self.asked_questions {
            writeln!(out, "Q\t{}", escape(q))?;
        }
        out.flush()?;
        fs::rename(tmp, &self.path)
    }

    fn decode_line(&mut self, line: &str) {
        if line.is_empty() || line.starts_with('#') {
            return;
        }
        let parts: Vec<&str> = line.split('\t').collect();
        match parts.first().copied() {
            Some("F") if parts.len() >= 6 => {
                let fact = Fact {
                    subject: unescape(parts[1]),
                    relation: unescape(parts[2]),
                    value: unescape(parts[3]),
                    confidence: parts[4].parse().unwrap_or(70),
                    learned_at: parts[5].parse().unwrap_or(0),
                };
                self.facts.insert(fact.key(), fact);
            }
            Some("C") if parts.len() >= 2 => {
                self.known_concepts.insert(unescape(parts[1]));
            }
            Some("Q") if parts.len() >= 2 => {
                self.asked_questions.insert(unescape(parts[1]));
            }
            _ => {}
        }
    }
}

pub fn normalize(input: impl AsRef<str>) -> String {
    input
        .as_ref()
        .trim()
        .trim_matches(|c: char| !c.is_alphanumeric() && c != '_' && c != '-')
        .to_lowercase()
}

pub fn concept_tokens(text: &str) -> Vec<String> {
    text.split_whitespace()
        .map(normalize)
        .filter(|s| s.len() >= 5 && !is_stopword(s))
        .collect()
}

fn is_stopword(s: &str) -> bool {
    matches!(
        s,
        "the" | "and" | "but" | "for" | "with" | "that" | "this" | "from" | "into" | "you" | "your"
            | "are" | "was" | "were" | "have" | "has" | "had" | "his" | "her" | "their" | "our" | "not"
            | "just" | "like" | "really" | "about" | "what" | "when" | "where" | "who" | "why" | "how"
            | "can" | "could" | "would" | "should" | "will" | "did" | "does" | "doing" | "its" | "it's"
            | "i'm" | "im" | "ive" | "i've" | "mine" | "they" | "them" | "then" | "there" | "here"
            | "today" | "yesterday" | "tomorrow" | "working" | "talked" | "telling" | "something" | "anything"
            | "thing" | "things" | "know" | "think" | "going" | "right" | "maybe" | "pretty" | "still"
            | "because" | "called" | "means" | "understand" | "remember" | "learned" | "learning" | "want"
    )
}

fn escape(s: &str) -> String {
    s.replace('\\', "\\\\")
        .replace('\t', "\\t")
        .replace('\n', "\\n")
        .replace('\r', "\\r")
}

fn unescape(s: &str) -> String {
    let mut out = String::new();
    let mut chars = s.chars();
    while let Some(c) = chars.next() {
        if c == '\\' {
            match chars.next() {
                Some('t') => out.push('\t'),
                Some('n') => out.push('\n'),
                Some('r') => out.push('\r'),
                Some('\\') => out.push('\\'),
                Some(other) => {
                    out.push('\\');
                    out.push(other);
                }
                None => out.push('\\'),
            }
        } else {
            out.push(c);
        }
    }
    out
}

fn now_epoch() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn temp_path(name: &str) -> PathBuf {
        env::temp_dir().join(format!("frank-test-{}-{}", name, now_epoch()))
    }

    #[test]
    fn memory_round_trips() {
        let path = temp_path("memory");
        let mut m = MemoryStore::load(&path).unwrap();
        m.remember_fact(Fact::new("Joel", "relationship", "stepdad")).unwrap();
        drop(m);
        let m = MemoryStore::load(&path).unwrap();
        assert_eq!(m.fact("joel", "relationship").unwrap().value, "stepdad");
        let _ = fs::remove_file(path);
    }
}
