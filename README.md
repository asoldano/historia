# Historia

Historia is a tool for analyzing Git repositories to track the history of tests and requirements. It helps identify files that have been modified without corresponding test updates, providing insights into code quality and test coverage.

## Overview

Historia analyzes Git repositories to:

- Track file modifications over time
- Identify commits and pull requests that modify specific files
- Detect when code changes are made without corresponding test updates
- Generate reports showing statistics about untested updates for each file

This information can be valuable for:

- Identifying areas of code with insufficient test coverage
- Tracking technical debt related to untested code
- Prioritizing testing efforts for frequently modified files
- Understanding the history of code and test changes

## Features

- Analyzes Git repositories using JGit
- Detects untested commits by examining file changes
- Tracks pull requests and their associated commits
- Generates CSV reports with statistics on file updates and test coverage
- Filters analysis to specific file paths

## Requirements

- Java 8 or higher
- Maven 3.0.0 or higher
- Git

## Building the Project

To build Historia, run:

```bash
mvn clean install
```

## Running Historia

Historia can be run using the following command:

```bash
java -cp core/target/historia-core-1.0.0.Beta1.jar org.jboss.historia.core.Runner <strategy class> <output file> <path filter> <git repo uri> <local repo clone uri>
```

### Parameters

- `<strategy class>`: The analysis strategy to use (currently `org.jboss.historia.core.UntestedCommitDetectionStrategy`)
- `<output file>`: Path to the output CSV file
- `<path filter>`: Filter to limit analysis to specific file paths (e.g., `src/main/java`)
- `<git repo uri>`: URI of the Git repository to analyze
- `<local repo clone uri>`: Local path where the repository will be cloned

### Example

```bash
java -cp core/target/historia-core-1.0.0.Beta1.jar org.jboss.historia.core.Runner org.jboss.historia.core.UntestedCommitDetectionStrategy ./output.csv src/main/java https://github.com/jbossws/jbossws-spi.git target/jgit/jbossws-spi
```

## Output Format

The generated CSV file contains the following columns:

- **Module**: The module or prefix of the file
- **File**: The file path
- **# updates**: Total number of updates to the file
- **# untested updates**: Number of updates without corresponding test changes
- **# untested updates %**: Percentage of updates that were untested
- **# updates since last tested**: Number of consecutive updates without test changes

## License

This project is licensed under the GNU Lesser General Public License, v. 2.1.
