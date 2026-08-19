use std::collections::HashMap;

pub trait TrustRoot {
    fn read(&self, key: &str) -> Option<&[u8]>;
    fn write_once(&mut self, key: String, value: Vec<u8>) -> Result<(), &'static str>;
}

#[derive(Debug, Default)]
pub struct InMemoryTrustRoot {
    entries: HashMap<String, Vec<u8>>,
}

impl TrustRoot for InMemoryTrustRoot {
    fn read(&self, key: &str) -> Option<&[u8]> {
        self.entries.get(key).map(Vec::as_slice)
    }

    fn write_once(&mut self, key: String, value: Vec<u8>) -> Result<(), &'static str> {
        if self.entries.contains_key(&key) {
            return Err("trust-root entry is immutable once written");
        }
        self.entries.insert(key, value);
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn root_values_are_write_once() {
        let mut root = InMemoryTrustRoot::default();
        root.write_once("policy-root".into(), vec![1, 2, 3]).unwrap();
        assert_eq!(root.write_once("policy-root".into(), vec![9]), Err("trust-root entry is immutable once written"));
        assert_eq!(root.read("policy-root"), Some([1, 2, 3].as_slice()));
    }
}
