use frank_android_compat::{ConversationEngine, MemoryStore};
use std::io::{self, Write};

fn main() -> io::Result<()> {
    let memory = MemoryStore::load_default()?;
    let path = memory.path().display().to_string();
    let mut frank = ConversationEngine::new(memory);

    println!("Frank local prototype");
    println!("Memory: {path}");
    println!("Type naturally. Commands: /memory, /pending, /forget, /help, /quit");
    println!("Frank: I'm here. Talk to me and I'll learn what I don't understand.\n");

    let stdin = io::stdin();
    loop {
        print!("You: ");
        io::stdout().flush()?;

        let mut input = String::new();
        if stdin.read_line(&mut input)? == 0 {
            println!();
            break;
        }
        let input = input.trim();
        if input.is_empty() {
            continue;
        }

        match input {
            "/quit" | "/exit" => {
                println!("Frank: Okay. I saved what I learned.");
                break;
            }
            "/help" => {
                println!("Frank: /memory shows learned facts; /pending shows what I'm currently wondering; /forget clears local memory; /quit exits.");
                continue;
            }
            "/memory" => {
                println!("Frank: I have {} facts across {} concepts.", frank.memory().fact_count(), frank.memory().concept_count());
                for fact in frank.memory().all_facts() {
                    println!("  {} --{}--> {}  [{}%]", fact.subject, fact.relation, fact.value, fact.confidence);
                }
                continue;
            }
            "/pending" => {
                match frank.pending_question() {
                    Some(q) => println!("Frank: I'm currently trying to understand: {q}"),
                    None => println!("Frank: Nothing specific is unresolved enough to ask about right now."),
                }
                continue;
            }
            "/forget" => {
                frank.memory_mut().forget_all()?;
                println!("Frank: Local learned memory cleared.");
                continue;
            }
            _ => {}
        }

        let result = frank.handle(input)?;
        println!("Frank: {}", result.reply);
    }

    Ok(())
}
