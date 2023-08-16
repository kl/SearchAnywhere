use anlocate::build::DatabaseOptions;
use anlocate::{build, search};
use clap::{Parser, Subcommand};
use std::fs::File;
use std::io::BufReader;

#[derive(Parser)]
#[command(about, long_about = None)]
struct Cli {
    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand)]
enum Commands {
    Search {
        db_path: String,
        text: String,
    },
    Build {
        db_path: String,
        #[arg(short, long, default_value = ".")]
        scan_root: String,
        #[arg(short, long, default_value_t = 1000000)]
        mem_limit: usize,
        #[arg(short, long)]
        relative_paths: bool,
        #[arg(short, long)]
        no_compress: bool
    },
}

fn main() {
    let cli = Cli::parse();
    match cli.command {
        Commands::Search { db_path, text } => {
            let mut db = BufReader::new(File::open(db_path).expect("could not open database file"));
            match search::search(&mut db, &text) {
                Ok(results) => {
                   for hit in results {
                       println!("{hit}");
                   }
                },
                Err(e) => {
                    eprintln!("error: {:?}", e);
                }
            }
        }
        Commands::Build {
            db_path,
            scan_root,
            mem_limit,
            relative_paths,
            no_compress,
        } => {
            print!("building...");
            build::build_database(
                db_path,
                scan_root,
                DatabaseOptions {
                    mem_limit,
                    write_absolute_paths: !relative_paths,
                    compress: !no_compress,
                },
            )
            .expect("failed to build database");
            println!("done!");
        }
    }
}
